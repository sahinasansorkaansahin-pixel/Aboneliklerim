import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time
import re

base_file = r'app/src/main/res/values/strings.xml'
res_dir = r'app/src/main/res'

targets = [
    "tr", "de", "ar", "no", "da", "sv", "ja", "zh-CN", "zh-TW", "is", "ko", 
    "cs", "pl", "hu", "ro", "es", "ru", "ms", "kk", "pt", "sr", "sq", "bs", 
    "th", "az", "ka", "id", "tl", "vi", "hi", "uk", "fa", "uz", "ur", "ps"
]

tree = ET.parse(base_file)
root = tree.getroot()

# Extract all strings that need translation
items_to_translate = []
for child in root:
    if child.tag == 'string':
        name = child.get('name')
        text = child.text
        # Skip app_name or empty strings
        if name == "app_name" or not text or text.strip() == "":
            continue
        items_to_translate.append((name, text))

print(f"Total items to translate: {len(items_to_translate)}")

# We will batch them into groups to avoid exceeding Google's maximum character limit per request (usually 5000 chars)
# 150 strings is around 3000 chars, so we can do it in 1 or 2 batches per language.

def batch_translate(texts, target_lang):
    translator = GoogleTranslator(source='auto', target=target_lang)
    
    # We join with a unique delimiter that Google Translate preserves, e.g., " [[[NS]]] "
    delimiter = " \n "
    joined_text = delimiter.join(texts)
    
    try:
        translated_joined = translator.translate(joined_text)
        # Split back
        translated_parts = translated_joined.split("\n")
        # Strip and clean parts
        cleaned_parts = []
        for p in translated_parts:
            p_str = p.strip()
            # Clean up potential artifacts from translator
            p_str = re.sub(r'%\s+d', '%d', p_str)
            p_str = re.sub(r'%\s+s', '%s', p_str)
            p_str = re.sub(r'% 1\$s', '%1$s', p_str)
            p_str = re.sub(r'% 2\$d', '%2$d', p_str)
            p_str = p_str.replace("'", "\\'") # escape single quotes for android xml
            cleaned_parts.append(p_str)
            
        return cleaned_parts
    except Exception as e:
        print(f"Error in batch translation for {target_lang}: {e}")
        return None

# Test batch translation
for target in targets:
    if target == "en": continue
    
    val_dir_name = f"values-{target}"
    if target == "zh-CN": val_dir_name = "values-zh-rCN"
    elif target == "zh-TW": val_dir_name = "values-zh-rTW"
    
    target_dir = os.path.join(res_dir, val_dir_name)
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, 'strings.xml')
    
    # Check if already exists and complete
    if os.path.exists(target_file):
        try:
            t_tree = ET.parse(target_file)
            if len(t_tree.getroot()) >= len(root) - 5:
                print(f"Skipping {target}, already complete.")
                continue
        except:
            pass
            
    print(f"Translating to {target} in batch...")
    
    # Split items into batches of 40 strings each to stay well within limits
    batch_size = 40
    translated_texts = []
    success = True
    
    for i in range(0, len(items_to_translate), batch_size):
        chunk = items_to_translate[i:i+batch_size]
        chunk_texts = [item[1] for item in chunk]
        
        translated_chunk = batch_translate(chunk_texts, target)
        if translated_chunk is None or len(translated_chunk) != len(chunk):
            # Fallback one-by-one for this chunk if batch size mismatch occurred
            print(f"Batch mismatch or error for {target} at chunk {i}. Size: {len(translated_chunk) if translated_chunk else 0} vs {len(chunk)}. Falling back to individual translation...")
            translator = GoogleTranslator(source='auto', target=target)
            fallback_chunk = []
            for text in chunk_texts:
                try:
                    t_text = translator.translate(text)
                    t_text = re.sub(r'%\s+d', '%d', t_text)
                    t_text = re.sub(r'%\s+s', '%s', t_text)
                    t_text = re.sub(r'% 1\$s', '%1$s', t_text)
                    t_text = re.sub(r'% 2\$d', '%2$d', t_text)
                    t_text = t_text.replace("'", "\\'")
                    fallback_chunk.append(t_text)
                except:
                    fallback_chunk.append(text.replace("'", "\\'"))
            translated_texts.extend(fallback_chunk)
        else:
            translated_texts.extend(translated_chunk)
            
        time.sleep(0.5) # small delay to be extremely safe
        
    # Write strings.xml
    with open(target_file, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        # Always write app_name first (untranslated or keeping original)
        f.write('    <string name="app_name">Aboneliklerim</string>\n')
        
        for idx, (name, _) in enumerate(items_to_translate):
            if idx < len(translated_texts):
                val = translated_texts[idx]
                f.write(f'    <string name="{name}">{val}</string>\n')
            else:
                # Fallback to original if we somehow missed it
                f.write(f'    <string name="{name}">{items_to_translate[idx][1].replace("'", "\\'")}</string>\n')
                
        f.write('</resources>\n')
        
    print(f"Finished {target}.")

print("All translations complete.")
