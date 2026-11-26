package io.github.niravgrowlity.quotewidget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var header: View
    private val adapter = QuoteAdapter()
    private val units = listOf("minutes" to 1L, "hours" to 60L, "days" to 1440L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = findViewById<ListView>(R.id.list)
        header = layoutInflater.inflate(R.layout.header, list, false)
        list.addHeaderView(header, null, false)
        list.adapter = adapter

        list.setOnItemClickListener { parent, _, position, _ ->
            val q = parent.getItemAtPosition(position) as? Quote ?: return@setOnItemClickListener
            if (QuoteStore.showNow(this, q.id)) {
                Rotation.schedule(this, force = true)
                refresh()
                toast("Now showing on the widget")
            } else {
                toast("That quote isn't in rotation. Pin it, or clear pins.")
            }
        }
        list.setOnItemLongClickListener { parent, _, position, _ ->
            val q = parent.getItemAtPosition(position) as? Quote ?: return@setOnItemLongClickListener false
            editQuote(q)
            true
        }

        view<Button>(R.id.btn_paste).setOnClickListener { pasteDialog() }
        view<Button>(R.id.btn_next).setOnClickListener { Rotation.advance(this); refresh() }
        view<Button>(R.id.btn_clear_pins).setOnClickListener { QuoteStore.clearPins(this); refresh() }
        view<Button>(R.id.btn_period).setOnClickListener { savePeriod() }
        view<Button>(R.id.btn_update).setOnClickListener { checkForUpdate(manual = true) }
        view<TextView>(R.id.version).text = "Version ${BuildConfig.VERSION_NAME}"

        setupPeriod()
        setupLook()

        Rotation.schedule(this, force = false)
        // Automatic update check at most once a day, and only while the app is open.
        if (savedInstanceState == null && QuoteStore.updateCheckDue(this)) checkForUpdate(manual = false)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun <T : View> view(id: Int): T = header.findViewById(id)
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---------- main refresh ----------

    private fun refresh() {
        val all = QuoteStore.all(this)
        val pinned = all.count { it.pinned }
        val current = QuoteStore.current(this)

        adapter.items = all
        adapter.currentId = current?.id
        adapter.notifyDataSetChanged()

        view<TextView>(R.id.status).text = when {
            all.isEmpty() -> "No quotes yet. Tap “Paste quotes” to add a list."
            pinned > 0 -> "Rotating through your $pinned pinned quote${if (pinned == 1) "" else "s"}"
            else -> "No pins, so rotating through all ${all.size} quotes"
        }
        view<TextView>(R.id.pins_status).text =
            if (pinned > 0) "$pinned pinned" else "Nothing pinned"
        view<Button>(R.id.btn_clear_pins).isEnabled = pinned > 0
        view<TextView>(R.id.list_title).text = "Your quotes (${all.size})"

        WidgetRenderer.updateAll(this)
        renderPreview()
    }

    /** Renders the exact same RemoteViews the home screen gets, so the preview is always truthful. */
    private fun renderPreview() {
        val box = view<FrameLayout>(R.id.preview)
        box.removeAllViews()
        try {
            box.addView(WidgetRenderer.build(this).apply(this, box))
        } catch (_: Exception) {
            box.visibility = View.GONE
        }
    }

    // ---------- rotation period ----------

    private fun setupPeriod() {
        val spinner = view<Spinner>(R.id.period_unit)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units.map { it.first })
        val m = QuoteStore.periodMinutes(this)
        val (unitIdx, value) = when {
            m % 1440 == 0L -> 2 to m / 1440
            m % 60 == 0L -> 1 to m / 60
            else -> 0 to m
        }
        spinner.setSelection(unitIdx)
        view<EditText>(R.id.period_value).setText(value.toString())
    }

    private fun savePeriod() {
        val value = view<EditText>(R.id.period_value).text.toString().toLongOrNull()
        if (value == null || value < 1) { toast("Enter a number of 1 or more"); return }
        val unit = units[view<Spinner>(R.id.period_unit).selectedItemPosition]
        if (value * unit.second < QuoteStore.MIN_PERIOD_MINUTES) {
            toast("The shortest is ${QuoteStore.MIN_PERIOD_MINUTES} minutes, to save battery")
            return
        }
        QuoteStore.setPeriodMinutes(this, value * unit.second)
        Rotation.schedule(this, force = true)
        val name = if (value == 1L) unit.first.removeSuffix("s") else unit.first
        toast("The widget will change every $value $name")
    }

    // ---------- widget look ----------

    private fun setupLook() {
        val shuffle = view<CheckBox>(R.id.shuffle)
        shuffle.isChecked = QuoteStore.shuffle(this)
        shuffle.setOnCheckedChangeListener { _, on -> QuoteStore.setShuffle(this, on); refresh() }

        val counter = view<CheckBox>(R.id.show_counter)
        counter.isChecked = QuoteStore.showCounter(this)
        counter.setOnCheckedChangeListener { _, on -> QuoteStore.setShowCounter(this, on); refresh() }

        bindSpinner(R.id.style_spinner, WidgetStyle.entries, { it.label }, QuoteStore.style(this)) {
            QuoteStore.setStyle(this, it)
        }
        bindSpinner(R.id.size_spinner, TextSize.entries, { it.label }, QuoteStore.textSize(this)) {
            QuoteStore.setTextSize(this, it)
        }
        bindSpinner(R.id.align_spinner, Align.entries, { it.label }, QuoteStore.align(this)) {
            QuoteStore.setAlign(this, it)
        }
    }

    private fun <E> bindSpinner(id: Int, values: List<E>, label: (E) -> String, current: E, onPick: (E) -> Unit) {
        val spinner = view<Spinner>(id)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, values.map(label))
        spinner.setSelection(values.indexOf(current))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                onPick(values[pos])
                refresh()
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
    }

    // ---------- adding / editing quotes ----------

    private fun pasteDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_paste, null)
        val input = v.findViewById<EditText>(R.id.paste_input)
        val mode = v.findViewById<RadioGroup>(R.id.split_mode)

        fun parsed(): List<String> {
            val text = input.text.toString()
            val blank = when (mode.checkedRadioButtonId) {
                R.id.mode_lines -> false
                R.id.mode_blank -> true
                else -> QuoteParser.hasBlankLines(text)
            }
            return QuoteParser.parse(text, blank)
        }

        AlertDialog.Builder(this)
            .setTitle("Paste quotes")
            .setView(v)
            .setPositiveButton("Add") { _, _ ->
                val n = QuoteStore.addAll(this, parsed())
                toast(if (n == 0) "Nothing new to add" else "Added $n quote${if (n == 1) "" else "s"}")
                refresh()
            }
            .setNeutralButton("Replace all") { _, _ ->
                val items = parsed()
                if (items.isEmpty()) { toast("Nothing to add"); return@setNeutralButton }
                AlertDialog.Builder(this)
                    .setMessage("Replace all ${QuoteStore.all(this).size} quotes and pins with these ${items.size}?")
                    .setPositiveButton("Replace") { _, _ -> QuoteStore.replaceAll(this, items); refresh() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editQuote(q: Quote) {
        val input = EditText(this).apply {
            setText(q.text)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit quote")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) QuoteStore.update(this, q.copy(text = t))
                refresh()
            }
            .setNeutralButton("Delete") { _, _ ->
                QuoteStore.delete(this, q.id)
                refresh()
                toast("Deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- updates ----------

    private fun checkForUpdate(manual: Boolean) {
        if (manual) toast("Checking for updates…")
        Thread {
            val release = try { Updater.fetchLatest() } catch (_: Exception) { null }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                when {
                    release == null -> if (manual) toast("Couldn't reach GitHub. Try again later.")
                    release.versionCode <= BuildConfig.VERSION_CODE -> if (manual) toast("You're on the latest version")
                    else -> AlertDialog.Builder(this)
                        .setTitle("Update available: ${release.name}")
                        .setMessage(release.notes.ifBlank { "A new version is ready." })
                        .setPositiveButton("Install") { _, _ -> installUpdate(release) }
                        .setNegativeButton("Later", null)
                        .show()
                }
            }
        }.start()
    }

    private fun installUpdate(release: Updater.Release) {
        if (!packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(this)
                .setTitle("Allow updates")
                .setMessage("Android needs your OK for Quote Widget to install its own updates. Turn on “Allow from this source”, come back, and tap Install again.")
                .setPositiveButton("Open settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        toast("Downloading update…")
        Thread {
            try {
                Updater.downloadAndInstall(this, release)
            } catch (e: Exception) {
                runOnUiThread { toast("Update failed: ${e.message}") }
            }
        }.start()
    }

    // ---------- list ----------

    private inner class QuoteAdapter : BaseAdapter() {
        var items: List<Quote> = emptyList()
        var currentId: Long? = null

        override fun getCount() = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.row_quote, parent, false)
            val q = items[position]
            val text = v.findViewById<TextView>(R.id.quote_text)
            text.text = q.text
            text.setTypeface(null, if (q.id == currentId) Typeface.BOLD else Typeface.NORMAL)
            val pin = v.findViewById<ImageButton>(R.id.pin_button)
            pin.setImageResource(
                if (q.pinned) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
            pin.contentDescription = if (q.pinned) "Unpin" else "Pin"
            pin.setOnClickListener {
                QuoteStore.update(this@MainActivity, q.copy(pinned = !q.pinned))
                refresh()
            }
            return v
        }
    }
}
