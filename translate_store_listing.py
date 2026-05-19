import os
import time
from deep_translator import GoogleTranslator

# Original Turkish values
original_title = "Aboneliklerim - Akıllı Abonelik ve Gider Takipçisi"
original_short = "Aboneliklerinizi yönetin, ödemelerinizi takip edin ve bütçenizi koruyun."

full_desc_path = r"magaza_aciklamasi_tr.txt"
if os.path.exists(full_desc_path):
    with open(full_desc_path, "r", encoding="utf-8") as f:
        original_full = f.read()
else:
    original_full = """Aboneliklerim - Akıllı Abonelik ve Gider Takipçisi

Bütün aboneliklerinizi tek bir yerden yönetin, ödemelerinizi asla kaçırmayın ve bütçenizi tam kontrol altına alın!

Dijital dünyada her şey bir aboneliğe dönüştü. Film platformları, müzik servisleri, yazılımlar ve üyelikler derken kaç tane aboneliğiniz olduğunu ve ne kadar harcadığınızı takip etmek zorlaştı. Aboneliklerim, bu karmaşayı sona erdirmek ve gizli giderlerinizi ortaya çıkarmak için tasarlanmış modern, şık ve güçlü bir finans asistanıdır."""

# 60 Locales focusing on wealthy and high-potential markets
locales = [
    "tr-TR", "en-US", "en-GB", "en-CA", "en-AU", "en-NZ", "en-SG", "en-IE", "de-DE", "de-AT",
    "de-CH", "fr-FR", "fr-BE", "fr-CH", "fr-CA", "fr-LU", "it-IT", "it-CH", "es-ES", "es-MX",
    "es-AR", "es-CL", "es-CO", "es-PE", "pt-PT", "pt-BR", "nl-NL", "nl-BE", "sv-SE", "no-NO",
    "da-DK", "fi-FI", "ja-JP", "ko-KR", "zh-CN", "zh-TW", "zh-HK", "pl-PL", "cs-CZ",
    "hu-HU", "ro-RO", "sk-SK", "bg-BG", "hr-HR", "sl-SI", "et-EE", "lv-LV", "lt-LT", "th-TH",
    "id-ID", "vi-VN", "ms-MY", "ar-SA", "ar-AE", "ar-QA", "ar-KW", "ar-BH", "ar-OM", "hi-IN"
]

def map_locale_to_lang(loc):
    # Map Android locale tag to deep-translator language code
    lang = loc.split("-")[0].lower()
    if lang == "zh":
        if "TW" in loc or "HK" in loc:
            return "zh-TW"
        return "zh-CN"
    if lang == "fil":
        return "tl" # Tagalog
    return lang

output_dir = "magaza_cevirileri"
os.makedirs(output_dir, exist_ok=True)

print("Starting store listing translations for 60 locales...")

translated_cache = {}

for idx, loc in enumerate(locales, 1):
    lang_code = map_locale_to_lang(loc)
    
    loc_dir = os.path.join(output_dir, loc)
    os.makedirs(loc_dir, exist_ok=True)
    
    # Files to generate
    title_file = os.path.join(loc_dir, "baslik.txt")
    short_file = os.path.join(loc_dir, "kisa_aciklama.txt")
    full_file = os.path.join(loc_dir, "uzun_aciklama.txt")
    
    # Force update
    if False and os.path.exists(title_file) and os.path.exists(short_file) and os.path.exists(full_file):
        print(f"[{idx}/60] Skipping {loc} (Already translated)")
        continue

    print(f"[{idx}/60] Translating for locale: {loc} (lang: {lang_code})...")
    
    try:
        # Check cache to avoid duplicate calls for same language code
        if lang_code in translated_cache:
            t_title, t_short, t_full = translated_cache[lang_code]
        else:
            if lang_code == "tr":
                t_title = original_title
                t_short = original_short
                t_full = original_full
            else:
                translator = GoogleTranslator(source='tr', target=lang_code)
                
                # Brand name "Aboneliklerim" should be preserved, so we translate the suffix part
                # Or translate directly and then make sure "Aboneliklerim" remains recognizable
                t_title = translator.translate(original_title)
                # Max Play Store title limit is 50 chars. If translated title is too long, we clip or simplify it
                if len(t_title) > 50:
                    t_title = translator.translate("Aboneliklerim - Abonelik Takip")
                    if len(t_title) > 50:
                        t_title = "Aboneliklerim"
                
                t_short = translator.translate(original_short)
                if len(t_short) > 80:
                    t_short = t_short[:77] + "..."
                    
                t_full = translator.translate(original_full)
            
            translated_cache[lang_code] = (t_title, t_short, t_full)
            time.sleep(0.5)

        # Write files
        with open(title_file, "w", encoding="utf-8") as f:
            f.write(t_title)
        with open(short_file, "w", encoding="utf-8") as f:
            f.write(t_short)
        with open(full_file, "w", encoding="utf-8") as f:
            f.write(t_full)
            
        print(f"Successfully generated translations for {loc}!")
        
    except Exception as e:
        print(f"Error translating for {loc}: {e}")
        # Write fallback files using english or original to prevent empty listings
        with open(title_file, "w", encoding="utf-8") as f:
            f.write("Aboneliklerim")
        with open(short_file, "w", encoding="utf-8") as f:
            f.write(original_short)
        with open(full_file, "w", encoding="utf-8") as f:
            f.write(original_full)

print("\nStore listing translations completed successfully for all 60 locales!")
