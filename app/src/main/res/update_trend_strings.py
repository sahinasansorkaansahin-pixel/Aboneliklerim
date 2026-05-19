import os
import re
from deep_translator import GoogleTranslator
import time

res_dir = r"c:\Aboneliklerim\app\src\main\res"

def get_lang_code(folder_name):
    if folder_name == "values":
        return "en"
    if folder_name in ["values-zh", "values-zh-rCN"]:
        return "zh-CN"
    if folder_name == "values-zh-rTW":
        return "zh-TW"
    parts = folder_name.split("-")
    if len(parts) >= 2:
        return parts[1]
    return None

def translate_and_update():
    folders = [f for f in os.listdir(res_dir) if f == "values" or (f.startswith("values-") and f != "values-night")]
    
    for folder in folders:
        lang = get_lang_code(folder)
        if not lang:
            continue
            
        file_path = os.path.join(res_dir, folder, "strings.xml")
        if not os.path.exists(file_path):
            continue
            
        print(f"Checking {folder} ({lang})...")
        
        # Read content
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()
            
        if 'name="spending_trend"' in content:
            print(f"  Already has spending_trend. Skipping.")
            continue
            
        # Translate
        try:
            if lang == "en":
                translation = "Monthly Spending Trend"
            elif lang == "tr":
                translation = "Aylık Harcama Trendi"
            else:
                translator = GoogleTranslator(source="en", target=lang)
                translation = translator.translate("Monthly Spending Trend")
                time.sleep(0.05) # Be nice to API
                
            if not translation:
                print(f"  Translation failed for {lang}.")
                continue
                
            # Escape quotes
            translation_esc = translation.replace("'", "\\'")
            
            # Insert before </resources>
            new_content = content.replace("</resources>", f'    <string name="spending_trend">{translation_esc}</string>\n</resources>')
            
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(new_content)
                
            print(f"  Added spending_trend to {folder}")
            
        except Exception as e:
            print(f"  Error processing {folder}: {e}")

if __name__ == "__main__":
    translate_and_update()
