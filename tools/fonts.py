"""The widget font library. Used by fetch_fonts.py (downloads) and gen_widget_res.py (layouts)."""

# (enum, label, Google family, weight, italic, category)
FONTS = [
    # Serif: classic book and editorial faces
    ("PLAYFAIR", "Playfair Display", "Playfair Display", 500, False, "SERIF"),
    ("PLAYFAIR_ITALIC", "Playfair Italic", "Playfair Display", 400, True, "SERIF"),
    ("CORMORANT", "Cormorant", "Cormorant Garamond", 500, False, "SERIF"),
    ("CORMORANT_ITALIC", "Cormorant Italic", "Cormorant Garamond", 500, True, "SERIF"),
    ("EB_GARAMOND", "EB Garamond", "EB Garamond", 400, False, "SERIF"),
    ("LORA", "Lora", "Lora", 400, False, "SERIF"),
    ("LORA_ITALIC", "Lora Italic", "Lora", 400, True, "SERIF"),
    ("LIBRE_BASKERVILLE", "Libre Baskerville", "Libre Baskerville", 400, False, "SERIF"),
    ("FRAUNCES", "Fraunces", "Fraunces", 400, False, "SERIF"),
    ("INSTRUMENT_SERIF", "Instrument Serif", "Instrument Serif", 400, False, "SERIF"),
    ("INSTRUMENT_ITALIC", "Instrument Italic", "Instrument Serif", 400, True, "SERIF"),
    ("DM_SERIF", "DM Serif Display", "DM Serif Display", 400, False, "SERIF"),
    ("BODONI", "Bodoni Moda", "Bodoni Moda", 500, False, "SERIF"),
    ("CRIMSON", "Crimson Pro", "Crimson Pro", 400, False, "SERIF"),
    # Sans: clean modern faces
    ("MANROPE", "Manrope", "Manrope", 500, False, "SANS"),
    ("OUTFIT", "Outfit", "Outfit", 400, False, "SANS"),
    ("SPACE_GROTESK", "Space Grotesk", "Space Grotesk", 500, False, "SANS"),
    ("JOSEFIN", "Josefin Sans", "Josefin Sans", 400, False, "SANS"),
    ("POPPINS", "Poppins", "Poppins", 500, False, "SANS"),
    ("QUICKSAND", "Quicksand", "Quicksand", 500, False, "SANS"),
    ("SYNE", "Syne", "Syne", 600, False, "SANS"),
    # Handwritten
    ("CAVEAT", "Caveat", "Caveat", 500, False, "HAND"),
    ("DANCING_SCRIPT", "Dancing Script", "Dancing Script", 500, False, "HAND"),
    ("GREAT_VIBES", "Great Vibes", "Great Vibes", 400, False, "HAND"),
    ("SACRAMENTO", "Sacramento", "Sacramento", 400, False, "HAND"),
    ("HOMEMADE_APPLE", "Homemade Apple", "Homemade Apple", 400, False, "HAND"),
    ("PATRICK_HAND", "Patrick Hand", "Patrick Hand", 400, False, "HAND"),
    ("KALAM", "Kalam", "Kalam", 400, False, "HAND"),
    # Typewriter and mono
    ("SPECIAL_ELITE", "Special Elite", "Special Elite", 400, False, "MONO"),
    ("COURIER_PRIME", "Courier Prime", "Courier Prime", 400, False, "MONO"),
    ("IBM_PLEX_MONO", "IBM Plex Mono", "IBM Plex Mono", 400, False, "MONO"),
    ("JETBRAINS_MONO", "JetBrains Mono", "JetBrains Mono", 400, False, "MONO"),
    # Display: big statement faces
    ("ABRIL", "Abril Fatface", "Abril Fatface", 400, False, "DISPLAY"),
    ("BEBAS", "Bebas Neue", "Bebas Neue", 400, False, "DISPLAY"),
    ("ALFA_SLAB", "Alfa Slab One", "Alfa Slab One", 400, False, "DISPLAY"),
    ("RIGHTEOUS", "Righteous", "Righteous", 400, False, "DISPLAY"),
]
CATEGORIES = [("SERIF", "Serif"), ("SANS", "Sans"), ("HAND", "Handwritten"),
              ("MONO", "Typewriter"), ("DISPLAY", "Display")]
