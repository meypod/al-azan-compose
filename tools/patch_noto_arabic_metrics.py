#!/usr/bin/env python3
"""Shrink Noto Sans Arabic's vertical line metrics to match Vazirmatn's line height.

Noto Sans Arabic reserves ~2.11em of vertical space per line (room for stacked harakat),
vs Vazirmatn's ~1.5625em, so Arabic UI text rendered taller lines than Persian/Latin. We
don't render harakat in the UI, so we retarget Noto's line-height metrics to Vazirmatn's
ratio while keeping the same ascent/descent split (baseline stays aligned).

Only the line-spacing metrics are touched:
  - hhea.ascent / descent / lineGap
  - OS/2.sTypoAscender / sTypoDescender / sTypoLineGap  (used by Android since USE_TYPO_METRICS is set)
usWin* is left large so tall glyphs are never clipped (it doesn't affect line height when
includeFontPadding=false, Compose's default). MVAR carries no vertical-metric deltas here,
so this static patch applies to every weight of the variable font.

Idempotent: writes absolute target values. Re-run safe.
"""
import sys
from fontTools.ttLib import TTFont

# Vazirmatn (upm 2048): typo/hhea ascent 2100, descent -1100, gap 0  ->  line ratio 1.5625em
VAZIRMATN_ASCENT_RATIO = 2100 / 2048
VAZIRMATN_DESCENT_RATIO = -1100 / 2048


def patch(path: str) -> None:
    font = TTFont(path)
    upm = font["head"].unitsPerEm
    ascent = round(VAZIRMATN_ASCENT_RATIO * upm)
    descent = round(VAZIRMATN_DESCENT_RATIO * upm)

    hhea = font["hhea"]
    hhea.ascent, hhea.descent, hhea.lineGap = ascent, descent, 0

    os2 = font["OS/2"]
    os2.sTypoAscender, os2.sTypoDescender, os2.sTypoLineGap = ascent, descent, 0
    os2.fsSelection |= (1 << 7)  # USE_TYPO_METRICS: make Android honor the sTypo* values

    font.save(path)
    line = ascent - descent
    print(f"patched {path}: ascent={ascent} descent={descent} lineGap=0 (ratio {line / upm:.4f}em)")


if __name__ == "__main__":
    patch(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res/font/noto_sans_arabic.ttf")
