import os
import re

print("Running fixes...")

# 1. Update local_delete string in values/strings.xml and values-tr/strings.xml
en_strings_path = r'c:\Aboneliklerim\app\src\main\res\values\strings.xml'
with open(en_strings_path, 'r', encoding='utf-8') as f:
    en_str = f.read()
en_str = re.sub(r'<string name="local_delete">.*?</string>', '<string name="local_delete">Delete Local (Plan) Backup</string>', en_str)
with open(en_strings_path, 'w', encoding='utf-8') as f:
    f.write(en_str)

tr_strings_path = r'c:\Aboneliklerim\app\src\main\res\values-tr\strings.xml'
with open(tr_strings_path, 'r', encoding='utf-8') as f:
    tr_str = f.read()
tr_str = re.sub(r'<string name="local_delete">.*?</string>', '<string name="local_delete">Yerel(Plan)Yedeği Sil</string>', tr_str)
with open(tr_strings_path, 'w', encoding='utf-8') as f:
    f.write(tr_str)

print("Updated strings")

# 2. Update BottomNavText in themes.xml
themes_path = r'c:\Aboneliklerim\app\src\main\res\values\themes.xml'
with open(themes_path, 'r', encoding='utf-8') as f:
    themes_str = f.read()
themes_str = themes_str.replace('<item name="android:textSize">10sp</item>', '<item name="android:textSize">9sp</item>')
with open(themes_path, 'w', encoding='utf-8') as f:
    f.write(themes_str)

# 3. Update BottomNavigationView in all 4 activity layouts
layout_dir = r'c:\Aboneliklerim\app\src\main\res\layout'
layouts = ['activity_main.xml', 'activity_upcoming.xml', 'activity_reports.xml', 'activity_more.xml']

for layout in layouts:
    layout_path = os.path.join(layout_dir, layout)
    with open(layout_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # ensure it has app:itemIconSize="20dp" and the text appearance styles
    if 'app:itemIconSize=' not in content:
        content = content.replace('app:menu="@menu/bottom_nav_menu"', 'app:itemIconSize="20dp"\n                app:menu="@menu/bottom_nav_menu"')
    else:
        content = re.sub(r'app:itemIconSize=".*?"', 'app:itemIconSize="20dp"', content)
        
    if 'app:itemTextAppearanceActive' not in content:
        content = content.replace('app:menu="@menu/bottom_nav_menu"', 'app:itemTextAppearanceActive="@style/BottomNavText"\n                app:itemTextAppearanceInactive="@style/BottomNavText"\n                app:menu="@menu/bottom_nav_menu"')
        
    with open(layout_path, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Updated layouts")
