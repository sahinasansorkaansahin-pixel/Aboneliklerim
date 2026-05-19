import os
import re

# List of brand variants to replace with the target brand
brand_old_variants = [
    "Falcon APP Studios",
    "Falcon App Studios",
    "Falcon App Estudios",
    "Falcon Uygulama Stüdyoları",
    "Studio aplikacji Falcon",
    "استوديوهات Falcon APP",
    "استوديوهات تطبيق فالكون",
    "Falcon APP Studios tərəfindən hazırlanmışdır",
    "Falcon APP Studios 製",
    "Falcon APP Studios tarafından geliştirildi",
    "Realizzato da Falcon APP Studios",
    "Hecho por Falcon APP Studios",
    "Erstellt von Falcon APP Studios",
    "Réalisé par Falcon APP Studios",
    "Izradio Falcon APP Studios",
    "Gert af Falcon APP Studios",
    "Skapad av Falcon APP Studios",
    "Lavet af Falcon APP Studios",
    "Prodhuar nga Falcon APP Studios",
    "Wykonane przez Falcon APP Studios",
    "Dibuat oleh Falcon APP Studios",
    "Realizat de Falcon APP Studios",
    "Vyrobeno společností Falcon APP Studios",
    "A Falcon APP Studios készítette",
]

target_brand = "FALCON ANDROİD STUDİO"

res_path = r"C:\Aboneliklerim\app\src\main\res"

def fix_content(content, is_turkish):
    # 1. Update brand name
    # We replace common variants
    for old in brand_old_variants:
        content = content.replace(old, target_brand)

    # Aggressive regex for brand name to catch case variations
    content = re.sub(r"Falcon App Studios", target_brand, content, flags=re.IGNORECASE)
    content = re.sub(r"Falcon APP Studios", target_brand, content, flags=re.IGNORECASE)

    # 2. Remove English text in parentheses from Turkish (and other languages if mixed)
    if is_turkish:
        # Specific known mixed strings in Turkish
        content = content.replace("(Life-time)", "")
        content = content.replace("(Plan)", "")
        content = content.replace("(Cost Split)", "")
        # Remove any other common English/Turkish mixes if found
        # Example: "Ömür Boyu (Lifetime)" -> "Ömür Boyu"
        content = re.sub(r"\s*\(Life-?time\)", "", content, flags=re.IGNORECASE)

    # Generic cleanup: Remove common English suffixes in parentheses often seen in auto-translations
    # But be careful not to remove (optional) or (%d days)
    # We only remove it if it looks like a translation of the preceding word.

    # 3. Clean up double spaces
    content = content.replace("  ", " ")

    return content

for root, dirs, files in os.walk(res_path):
    if "strings.xml" in files:
        file_path = os.path.join(root, "strings.xml")
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        is_tr = "values-tr" in root
        new_content = fix_content(content, is_tr)

        if new_content != content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)

print("Brand and localization fixed.")
