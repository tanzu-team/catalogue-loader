import glob

def repair_text(text):
    for bad, good in [("â€™", "’"), ("â€“", "–"), ("â€œ", "“"), ("â€", "”"), ("Â®", "®"), ("Â©", "©")]:
        text = text.replace(bad, good)
    try:
        # Re-decode CP1252 mojibake if present
        fixed = text.encode("cp1252", errors="ignore").decode("utf-8")
        return fixed if "\ufffd" not in fixed else text
    except Exception:
        return text

for f in glob.glob("./demo_site/products.csv"):
    with open(f, "r", encoding="utf-8", errors="replace") as infile:
        content = repair_text(infile.read())
    with open(f, "w", encoding="utf-8") as outfile:
        outfile.write(content)
    print(f"Cleaned {f}")