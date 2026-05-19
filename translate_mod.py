import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time
import re

base_file = r'c:\Aboneliklerim\app\src\main\res\values\strings.xml'
res_dir = r'c:\Aboneliklerim\app\src\main\res'

keys_to_update = [
    "policy_deletion_title",
    "policy_deletion_content",
    "delete_cloud_data",
    "delete_cloud_confirm_title",
    "delete_cloud_confirm_msg",
    "delete_cloud_success"
]

tree = ET.parse(base_file)
root = tree.getroot()

eng_texts = {}
for child in root:
    name = child.get('name')
    if name in keys_to_update:
        eng_texts[name] = child.text

print(f"Loaded {len(eng_texts)} English strings to update.")

targets = [d for d in os.listdir(res_dir) if d.startswith("values-")]

for val_dir in targets:
    if val_dir in ["values-en", "values-tr"]: 
        # Skip base and Turkish since we manually updated Turkish
        continue
    
    lang_code = val_dir.replace("values-", "")
    if lang_code == "zh-rCN": lang_code = "zh-CN"
    if lang_code == "zh-rTW": lang_code = "zh-TW"
    
    # special mappings if needed for deep_translator
    if lang_code.startswith("b+"): # skip exotic ones if any or fix names
        continue
    
    target_file = os.path.join(res_dir, val_dir, 'strings.xml')
    if not os.path.exists(target_file):
        continue
        
    try:
        t_tree = ET.parse(target_file)
        t_root = t_tree.getroot()
        
        translator = GoogleTranslator(source='en', target=lang_code)
        
        updated = False
        for child in t_root:
            name = child.get('name')
            if name in keys_to_update:
                eng_text = eng_texts[name]
                try:
                    translated = translator.translate(eng_text)
                    translated = translated.replace("'", "\\'")
                    child.text = translated
                    updated = True
                except Exception as e:
                    print(f"Error translating {name} to {lang_code}: {e}")
                    
        if updated:
            t_tree.write(target_file, encoding='utf-8', xml_declaration=True)
            print(f"Updated {val_dir}")
            
    except Exception as e:
        print(f"Error processing {val_dir}: {e}")
        
print("All updates completed.")
