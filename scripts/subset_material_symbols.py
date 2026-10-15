#!/usr/bin/env python3
"""Subset the Material Symbols Rounded variable font to the glyphs OpenSplitIcons uses.

Usage: scripts/subset_material_symbols.py <path to MaterialSymbolsRounded[FILL,GRAD,opsz,wght].ttf>
Needs fonttools (pip install fonttools). Re-run after adding a Symbol to OpenSplitIcons.kt.
Source font: https://github.com/google/material-design-icons/tree/master/variablefont
"""
import re, subprocess, sys, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
icons = (root / "app/src/main/java/com/opensplit/ui/theme/OpenSplitIcons.kt").read_text()
cps = sorted({int(m, 16) for m in re.findall(r"Symbol\(0x([0-9a-fA-F]+)", icons)})
out = root / "app/src/main/res/font/material_symbols_rounded.ttf"
subprocess.check_call([
    sys.executable, "-m", "fontTools.subset", sys.argv[1],
    "--unicodes=" + ",".join(f"U+{c:04X}" for c in cps),
    "--layout-features=", "--no-hinting", "--desubroutinize",
    "--name-IDs=1,2,4,6", f"--output-file={out}",
])
print(f"{len(cps)} glyphs -> {out} ({out.stat().st_size // 1024} KB)")
