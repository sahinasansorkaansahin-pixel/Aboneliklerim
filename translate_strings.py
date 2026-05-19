import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time
import re

base_file = r'app/src/main/res/values/strings.xml'
res_dir = r'app/src/main/res'

# These are the unique languages corresponding to the 20 target locales (including base en)
targets = [
    "tr", "de", "fr", "it", "es", "pt", "nl", "sv", "no", "da", "fi", "ja", "ko", 
    "zh-CN", "pl", "cs", "th", "ar"
]

def get_translator(target_lang):
    if target_lang == 'zh-CN':
        return GoogleTranslator(source='auto', target='zh-CN')
    elif target_lang == 'zh-TW':
        return GoogleTranslator(source='auto', target='zh-TW')
    elif target_lang == 'he':
        return GoogleTranslator(source='auto', target='iw')
    return GoogleTranslator(source='auto', target=target_lang)

def translate_text(translator, text):
    if not text: return text
    if text == "Aboneliklerim": return text
    
    try:
        translated = translator.translate(text)
        if translated:
            # Fix common translation artifacts for Android strings
            translated = re.sub(r'%\s+d', '%d', translated)
            translated = re.sub(r'%\s+s', '%s', translated)
            translated = re.sub(r'% 1\$s', '%1$s', translated)
            translated = re.sub(r'% 2\$d', '%2$d', translated)
            translated = translated.replace("'", "\\'") # escape quotes
        return translated
    except Exception as e:
        print(f"Error translating: {text[:20]}... -> {e}")
        return None

tree = ET.parse(base_file)
root = tree.getroot()

for target in targets:
    if target == "en": continue # Base is English in values/strings.xml
    
    val_dir_name = f"values-{target}"
    if target == "zh-CN": val_dir_name = "values-zh-rCN"
    elif target == "zh-TW": val_dir_name = "values-zh-rTW"
    
    target_dir = os.path.join(res_dir, val_dir_name)
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, 'strings.xml')
    
    # Load existing translations to resume if interrupted
    existing_strings = {}
    if os.path.exists(target_file):
        try:
            target_tree = ET.parse(target_file)
            for child in target_tree.getroot():
                if child.tag == 'string':
                    existing_strings[child.get('name')] = child.text
        except:
            pass

    if len(existing_strings) >= len(root) - 2:
        print(f"Skipping {target}, already translated.")
        continue

    print(f"Translating to {target} ({len(existing_strings)}/{len(root)} done)...")
    translator = get_translator(target)
    
    new_strings = existing_strings.copy()
    
    count = 0
    for child in root:
        if child.tag == 'string':
            name = child.get('name')
            text = child.text
            
            if name in new_strings and new_strings[name]:
                continue
                
            if text:
                new_text = translate_text(translator, text)
                if new_text:
                    new_strings[name] = new_text
                    count += 1
                    # Small delay to avoid rate limits
                    if count % 5 == 0:
                        time.sleep(0.5)
                else:
                    # If we hit an error, stop this language for now to avoid hanging
                    print(f"Failed to translate {name} for {target}. Stopping batch.")
                    break
    
    # Save progress for this language
    with open(target_file, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        # Sort to keep order consistent
        for child in root:
            if child.tag == 'string':
                name = child.get('name')
                text = new_strings.get(name)
                if text:
                    f.write(f'    <string name="{name}">{text}</string>\n')
        f.write('</resources>\n')
    
    print(f"Updated {target} with {count} new translations.")

print("All tasks completed.")
