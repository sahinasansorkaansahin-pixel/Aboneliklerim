import re

filepath = 'app/src/main/res/values-tr/strings.xml'
with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Find the position of filter_archived
idx = text.find('<string name="filter_archived">')
if idx != -1:
    end_idx = text.find('\n', idx)
    clean_text = text[:end_idx+1]
    
    clean_text += '    <string name="default_currency">Varsayılan Para Birimi</string>\n'
    clean_text += '    <string name="date_format">Tarih Formatı</string>\n'
    clean_text += '    <string name="select_date_format">Tarih Formatı Seçin</string>\n'
    clean_text += '</resources>\n'
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(clean_text)
