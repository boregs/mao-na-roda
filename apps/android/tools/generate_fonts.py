"""Generate Android static weights from the bundled variable fonts.

Requires fonttools. Run from any directory with: python tools/generate_fonts.py
Static instances avoid the variable font's ExtraLight default on Android.
"""

from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont


android = Path(__file__).resolve().parents[1]
source_fonts = android / "app" / "src" / "main" / "res" / "font"
output = android / "app" / "src" / "main" / "res" / "font"

for source, weight, filename in [
    ("source_sans_3.ttf", 400, "source_sans_3_regular.ttf"),
    ("source_sans_3.ttf", 700, "source_sans_3_bold.ttf"),
    ("source_serif_4.ttf", 600, "source_serif_4_semibold.ttf"),
]:
    font = TTFont(source_fonts / source)
    instantiateVariableFont(font, {"wght": weight}, inplace=True).save(output / filename)
