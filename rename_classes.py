"""Rename all NewProfiler classes with an NP_ prefix so they never collide
with the original Color_Profiler.jar classes on ImageJ's shared classpath.
Reads src/ -> writes src_np/. Source files are untouched.
"""
import re, pathlib, shutil

src = pathlib.Path("src")
dst = pathlib.Path("src_np")
if dst.exists():
    shutil.rmtree(dst)
dst.mkdir()

RENAME = {
    "ColorProfiler_": "NP_ColorProfiler",
    "ProfileColorPlot": "NP_ProfileColorPlot",
    "MultyPlotWindow": "NP_MultyPlotWindow",
    "MultyPlot": "NP_MultyPlot",
}
# Longest-first alternation + word boundaries prevents partial/double matches.
pat = re.compile(r"\b(" + "|".join(sorted(RENAME, key=len, reverse=True)) + r")\b")

def sub(text: str) -> str:
    return pat.sub(lambda m: RENAME[m.group(1)], text)

for f in sorted(src.glob("*.java")):
    new_text = sub(f.read_text(encoding="utf-8"))
    new_name = RENAME.get(f.stem, f.stem) + ".java"
    (dst / new_name).write_text(new_text, encoding="utf-8")
    print(f"{f.name} -> {new_name}")

for name in ("plugins.config", "License.txt", "README.txt"):
    p = src / name
    if p.exists():
        (dst / name).write_text(sub(p.read_text(encoding="utf-8")), encoding="utf-8")
        print("copied+substituted", name)

print("\n=== new plugins.config ===")
print((dst / "plugins.config").read_text(encoding="utf-8"))
