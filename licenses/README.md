# Bundled fonts

Both fonts are licensed under the SIL Open Font License 1.1 (full text alongside this file) and
shipped in `app/src/main/res/font/`. They are applied per UI locale in `core/presentation/Theme.kt`.

## Vazirmatn — Persian (`fa`)
- File: `app/src/main/res/font/vazirmatn.ttf` (variable, `wght` axis)
- Source: https://github.com/google/fonts/tree/main/ofl/vazirmatn
- License: [Vazirmatn-OFL.txt](Vazirmatn-OFL.txt)
- **Unmodified.**

## Noto Sans Arabic — Arabic (`ar`)
- File: `app/src/main/res/font/noto_sans_arabic.ttf` (variable, `wght` axis)
- Source: https://github.com/google/fonts/tree/main/ofl/notosansarabic
- License: [NotoSansArabic-OFL.txt](NotoSansArabic-OFL.txt)
- **Modified.** Its line-spacing metrics were reduced from ~2.112em to ~1.562em per line so Arabic
  UI text matches the line height of Vazirmatn (Persian) and the Latin default; upstream reserves
  the extra space for stacked harakat, which this app does not render. Only line metrics were
  touched — `hhea` ascent/descent/lineGap and `OS/2.sTypo*` (ascent 1025, descent -537, gap 0);
  glyphs are untouched. Reproduce with [`tools/patch_noto_arabic_metrics.py`](../tools/patch_noto_arabic_metrics.py).
  The OFL declares no Reserved Font Name, so the modification keeps the original name.
