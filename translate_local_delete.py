import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator

base_file = r'c:\Aboneliklerim\app\src\main\res\values\strings.xml'
res_dir = r'c:\Aboneliklerim\app\src\main\res'

keys_to_update = ["local_delete"]

tree = ET.parse(base_file)
root = tree.getroot()

eng_texts = {}
for child in root:
    name = child.get('name')
    if name in keys_to_update:
        eng_texts[name] = child.text

targets = [d for d in os.listdir(res_dir) if d.startswith("values-")]

for val_dir in targets:
    if val_dir in ["values-en", "values-tr"]: 
        continue
    
    lang_code = val_dir.replace("values-", "")
    if lang_code == "zh-rCN": lang_code = "zh-CN"
    if lang_code == "zh-rTW": lang_code = "zh-TW"
    if lang_code.startswith("b+"): continue
    
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
                try:
                    translated = translator.translate(eng_texts[name])
                    translated = translated.replace("'", "\\'")
                    child.text = translated
                    updated = True
                except:
                    pass
                    
        if updated:
            t_tree.write(target_file, encoding='utf-8', xml_declaration=True)
            print(f"Updated {val_dir}")
    except:
        pass
        
import re
for val_dir in targets:
    target_file = os.path.join(res_dir, val_dir, 'strings.xml')
    if not os.path.exists(target_file): continue
    with open(target_file, 'r', encoding='utf-8') as f:
        content = f.read()
    orig = content
    content = content.replace(r"\\'", r"\'")
    content = content.replace(r"\\\"", r"\"")
    content = re.sub(r'&(?!(amp;|#\d+;|[a-zA-Z]+;))', '&amp;', content)
    if orig != content:
        with open(target_file, 'w', encoding='utf-8') as f:
            f.write(content)

print("All done.")
