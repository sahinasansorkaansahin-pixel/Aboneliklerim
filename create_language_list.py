import os

res_dir = r'app/src/main/res'
exclusions = {'night', 'ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi', 'sw600dp', 'sw720dp', 'v21', 'v23', 'v24', 'v26', 'v28'}

# Map Android locale folders to full human-readable Turkish names
language_names = {
    'ar': 'Arapça',
    'az': 'Azerice',
    'bs': 'Boşnakça',
    'cs': 'Çekçe',
    'da': 'Danca (Danimarkaca)',
    'de': 'Almanca',
    'es': 'İspanyolca',
    'fa': 'Farsça',
    'fi': 'Fince',
    'fr': 'Fransızca',
    'hi': 'Hintçe',
    'hu': 'Macarca',
    'id': 'Endonezce',
    'is': 'İzlandaca',
    'it': 'İtalyanca',
    'ja': 'Japonca',
    'ka': 'Gürcüce',
    'kk': 'Kazakça',
    'ko': 'Korece',
    'ms': 'Malayca',
    'nl': 'Felemenkçe',
    'no': 'Norveççe',
    'pl': 'Lehçe',
    'ps': 'Peştuca',
    'pt': 'Portekizce',
    'ro': 'Romence',
    'ru': 'Rusça',
    'sq': 'Arnavutça',
    'sr': 'Sırpça',
    'sv': 'İsveççe',
    'th': 'Tayca',
    'tl': 'Tagalogca (Filipince)',
    'tr': 'Türkçe',
    'uk': 'Ukraynaca',
    'ur': 'Urduca',
    'uz': 'Özbekçe',
    'vi': 'Vietnamca',
    'zh': 'Çince',
    'zh-rCN': 'Çince (Basitleştirilmiş)',
    'zh-rTW': 'Çince (Geleneksel)',
    'bg': 'Bulgarca',
    'hr': 'Hırvatça',
    'sk': 'Slovakça',
    'sl': 'Slovence',
    'et': 'Estonca',
    'lv': 'Letonca',
    'lt': 'Litvanca',
    'ga': 'İrlandaca',
    'mt': 'Maltaca',
    'af': 'Afrikanca',
    'bn': 'Bengalce',
    'mr': 'Marathice',
    'ta': 'Tamilce',
    'te': 'Teluguca',
    'gu': 'Gujarati',
    'pa': 'Pencapça',
    'kn': 'Kannadaca',
    'ml': 'Malayalamca',
    'ca': 'Katalanca',
    'gl': 'Galiçyaca',
    'lb': 'Lüksemburgca'
}

detected_languages = ['İngilizce'] # The base default 'values' folder contains English

for item in os.listdir(res_dir):
    if item.startswith('values-') and os.path.isdir(os.path.join(res_dir, item)):
        suffix = item[7:]
        if suffix not in exclusions:
            name = language_names.get(suffix, suffix)
            detected_languages.append(name)

# Remove duplicates and sort alphabetically
detected_languages = sorted(list(set(detected_languages)))

# Format output
total_count = len(detected_languages)
lang_list_str = ", ".join(detected_languages)

output_content = f"Toplam Dil Sayısı: {total_count}\n\nDesteklenen Diller:\n{lang_list_str}\n"

with open('diller.txt', 'w', encoding='utf-8') as f:
    f.write(output_content)

print(f"Created diller.txt with {total_count} languages successfully.")
