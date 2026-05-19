import os
import re
import time
import sys
from deep_translator import GoogleTranslator

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

base_dir = r"c:\Aboneliklerim\app\src\main\res"
english_text = "This contact is already added"

# Get all directories in res that start with values
folders = [d for d in os.listdir(base_dir) if d.startswith("values") and d != "values-night"]

def get_lang_code(folder):
    if folder == "values":
        return None
    if folder == "values-zh-rCN":
        return "zh-CN"
    if folder == "values-zh-rTW":
        return "zh-TW"
    if folder == "values-zh":
        return "zh-CN"
    # Otherwise, split by hyphen and take the second part
    parts = folder.split("-")
    if len(parts) >= 2:
        return parts[1]
    return None

for folder in folders:
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if not os.path.exists(file_path):
        continue
    
    lang = get_lang_code(folder)
    
    if lang is None:
        translated_text = english_text
        print(f"Base/English: {translated_text}")
    else:
        try:
            translator = GoogleTranslator(source='auto', target=lang)
            translated_text = translator.translate(english_text)
            print(f"Translated to {lang} ({folder}): {translated_text}")
            time.sleep(0.1) # Small rate limiting
        except Exception as e:
            print(f"Error translating to {lang}: {e}")
            if lang == "tr":
                translated_text = "Bu kişi zaten ekli"
            else:
                translated_text = english_text
    
    # Escape quotes
    translated_text = translated_text.replace("'", "\\'")
    
    # Read strings.xml
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    key = "duplicate_contact_warning"
    if f'name="{key}"' not in content:
        content = content.replace('</resources>', f'    <string name="{key}">{translated_text}</string>\n</resources>')
    else:
        content = re.sub(f'<string name="{key}">.*?</string>', f'<string name="{key}">{translated_text}</string>', content)
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("All translations completed successfully!")
