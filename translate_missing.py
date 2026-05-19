import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time
import re

base_file = r'app/src/main/res/values/strings.xml'
res_dir = r'app/src/main/res'

# 1. Dynamically discover all active locales in app/src/main/res/
exclusions = {'night', 'ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi', 'sw600dp', 'sw720dp', 'v21', 'v23', 'v24', 'v26', 'v28'}
detected_targets = []

for item in os.listdir(res_dir):
    if item.startswith('values-') and os.path.isdir(os.path.join(res_dir, item)):
        suffix = item[7:] # Extract suffix after 'values-'
        if suffix not in exclusions:
            # Map Android resource directory names to Google Translator language codes
            lang_code = suffix
            if suffix == "zh-rCN":
                lang_code = "zh-CN"
            elif suffix == "zh-rTW":
                lang_code = "zh-TW"
            else:
                lang_code = suffix.replace('-r', '-')
            
            detected_targets.append((item, lang_code))

print(f"Detected {len(detected_targets)} locale directories to verify:")
for folder, lang in sorted(detected_targets):
    print(f"  - {folder} -> code: {lang}")

# 2. Read base strings from values/strings.xml
tree = ET.parse(base_file)
root = tree.getroot()
base_strings = {}
for child in root:
    if child.tag == 'string':
        name = child.get('name')
        text = child.text
        if name and text and name != "app_name":
            base_strings[name] = text

print(f"Total base strings: {len(base_strings)}")

def translate_single(text, target_lang):
    translator = GoogleTranslator(source='auto', target=target_lang)
    try:
        t_text = translator.translate(text)
        # Fix Google Translate spacing issues on placeholders
        t_text = re.sub(r'%\s+d', '%d', t_text)
        t_text = re.sub(r'%\s+s', '%s', t_text)
        t_text = re.sub(r'% 1\$s', '%1$s', t_text)
        t_text = re.sub(r'% 2\$s', '%2$s', t_text)
        return t_text
    except Exception as e:
        print(f"Error translating '{text}' to {target_lang}: {e}")
        return text

def translate_batch_optimized(texts, target_lang):
    if not texts:
        return []
    
    translator = GoogleTranslator(source='auto', target=target_lang)
    chunk_size = 40
    results = []
    
    for i in range(0, len(texts), chunk_size):
        chunk = texts[i:i+chunk_size]
        try:
            # translate_batch takes a list of strings
            translated_chunk = translator.translate_batch(chunk)
            
            # Format and clean each translated string
            cleaned_chunk = []
            for t_text in translated_chunk:
                t_text = str(t_text)
                t_text = re.sub(r'%\s+d', '%d', t_text)
                t_text = re.sub(r'%\s+s', '%s', t_text)
                t_text = re.sub(r'% 1\$s', '%1$s', t_text)
                t_text = re.sub(r'% 2\$s', '%2$s', t_text)
                cleaned_chunk.append(t_text)
                
            results.extend(cleaned_chunk)
            print(f"  Translated chunk of size {len(chunk)} successfully.")
            time.sleep(1.0) # Small delay between batches to respect API limits
        except Exception as e:
            print(f"  Error translating batch of size {len(chunk)} to {target_lang}: {e}. Retrying one by one...")
            # Fallback to translate one-by-one for this chunk
            for item in chunk:
                results.append(translate_single(item, target_lang))
                time.sleep(0.3)
                
    return results

def escape_xml(val):
    val = str(val)
    # Escape XML entities first
    val = val.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    # Escape single quotes properly (if not already escaped)
    val = re.sub(r"(?<!\\)'", r"\'", val)
    # Replace literal newlines with XML newlines
    val = val.replace('\n', '\\n')
    return val

# 3. Process each detected locale - Prioritize Turkish 'tr' first!
ordered_targets = sorted(detected_targets)
tr_target = None
for item in ordered_targets:
    if item[1] == "tr":
        tr_target = item
        break
if tr_target:
    ordered_targets.remove(tr_target)
    ordered_targets.insert(0, tr_target)

for folder, target in ordered_targets:
    if target == "en": 
        continue
    
    target_dir = os.path.join(res_dir, folder)
    target_file = os.path.join(target_dir, 'strings.xml')
    
    target_strings = {}
    if os.path.exists(target_file):
        try:
            t_tree = ET.parse(target_file)
            for child in t_tree.getroot():
                if child.tag == 'string':
                    name = child.get('name')
                    text = child.text
                    if name: 
                        target_strings[name] = text or ""
        except Exception as e:
            print(f"Error parsing {target_file}: {e}")
            
    # Find missing keys
    missing = []
    for name, text in base_strings.items():
        if name not in target_strings:
            missing.append((name, text))
            
    if not missing:
        print(f"No missing strings for {target} ({folder}).")
        continue
        
    print(f"Translating {len(missing)} missing strings to {target} ({folder})...")
    missing_texts = [text for name, text in missing]
    translated_texts = translate_batch_optimized(missing_texts, target)
    
    for (name, text), translated in zip(missing, translated_texts):
        target_strings[name] = translated
        
    # Write back the strings file safely
    with open(target_file, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        f.write('    <string name="app_name">Aboneliklerim</string>\n')
        
        # Write all base strings in their original order
        for name, text in base_strings.items():
            val = target_strings.get(name, text)
            escaped_val = escape_xml(val)
            f.write(f'    <string name="{name}">{escaped_val}</string>\n')
            
        f.write('</resources>\n')
        
    print(f"Finished {target} ({folder}).")

print("All missing translations complete.")
