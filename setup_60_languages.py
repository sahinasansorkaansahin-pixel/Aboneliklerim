import os
import shutil

res_dir = r'app/src/main/res'

# Define the exact 59 directories for the 60 premium languages (English is default 'values' so it doesn't have a values-en folder)
# 59 values-* folders + 1 values default folder = 60 languages!
premium_locales = {
    'values-tr': 'Turkce',
    'values-de': 'Almanca',
    'values-fr': 'Fransizca',
    'values-it': 'Italyanca',
    'values-es': 'Ispanyolca',
    'values-ja': 'Japonca',
    'values-ko': 'Korece',
    'values-pt': 'Portekizce',
    'values-ru': 'Rusca',
    'values-zh': 'Cince',
    'values-zh-rCN': 'Cince (Basitlestirilmis)',
    'values-zh-rTW': 'Cince (Geleneksel)',
    'values-nl': 'Felemenkce',
    'values-sv': 'Isvecce',
    'values-no': 'Norvecce',
    'values-da': 'Danca',
    'values-fi': 'Fince',
    'values-pl': 'Lehce',
    'values-cs': 'Cekce',
    'values-hu': 'Macarca',
    'values-ro': 'Romence',
    'values-uk': 'Ukraynaca',
    'values-ar': 'Arapca',
    'values-id': 'Endonezce',
    'values-ms': 'Malayca',
    'values-vi': 'Vietnamca',
    'values-th': 'Tayca',
    'values-tl': 'Filipince',
    'values-hi': 'Hintce',
    'values-uz': 'Ozbekce',
    'values-kk': 'Kazakca',
    'values-az': 'Azerice',
    'values-ka': 'Gurcuce',
    'values-bs': 'Bosnakca',
    'values-sr': 'Sirpca',
    'values-sq': 'Arnavutca',
    'values-is': 'Izlandaca',
    'values-ur': 'Urduca',
    'values-bg': 'Bulgarca',
    'values-hr': 'Hirvatca',
    'values-sk': 'Slovakca',
    'values-sl': 'Slovence',
    'values-et': 'Estonca',
    'values-lv': 'Letonca',
    'values-lt': 'Litvanca',
    'values-ga': 'Irlandaca',
    'values-mt': 'Maltaca',
    'values-af': 'Afrikanca',
    'values-bn': 'Bengalce',
    'values-mr': 'Marathice',
    'values-ta': 'Tamilce',
    'values-te': 'Teluguca',
    'values-gu': 'Gujarati',
    'values-pa': 'Pencapca',
    'values-kn': 'Kannadaca',
    'values-ml': 'Malayalamca',
    'values-ca': 'Katalanca',
    'values-gl': 'Galiyciaca',
    'values-lb': 'Luksemburgca'
}

exclusions = {'night', 'ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi', 'sw600dp', 'sw720dp', 'v21', 'v23', 'v24', 'v26', 'v28'}

# 1. Clean up unwanted directories
print("Scanning active locale directories...")
for item in os.listdir(res_dir):
    item_path = os.path.join(res_dir, item)
    if item.startswith('values-') and os.path.isdir(item_path):
        suffix = item[7:]
        if suffix in exclusions:
            continue
        
        # If it is not in our premium list, remove it!
        if item not in premium_locales:
            print(f"Removing unwanted/excluded directory: {item}")
            shutil.rmtree(item_path)

# 2. Create missing premium directories with template strings.xml
for folder, name in premium_locales.items():
    folder_path = os.path.join(res_dir, folder)
    if not os.path.exists(folder_path):
        print(f"Creating premium directory: {folder}")
        os.makedirs(folder_path, exist_ok=True)
        
    strings_file = os.path.join(folder_path, 'strings.xml')
    if not os.path.exists(strings_file) or os.path.getsize(strings_file) == 0:
        with open(strings_file, 'w', encoding='utf-8') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n')

print("Premium locale directory setup complete.")
