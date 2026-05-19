import os

filepath = 'app/src/main/res/values-tr/strings.xml'
with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

# Remove the last 4 lines (default_currency, date_format, select_date_format, </resources>)
lines = lines[:-4]

lines.append('    <string name="default_currency">Varsayılan Para Birimi</string>\n')
lines.append('    <string name="date_format">Tarih Formatı</string>\n')
lines.append('    <string name="select_date_format">Tarih Formatı Seçin</string>\n')
lines.append('</resources>\n')

with open(filepath, 'w', encoding='utf-8') as f:
    f.writelines(lines)
