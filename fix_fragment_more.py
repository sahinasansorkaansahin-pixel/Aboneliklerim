import re

print("Starting fix_fragment_more.py")

fragment_file = r"c:\Aboneliklerim\app\src\main\res\layout\fragment_more.xml"
with open(fragment_file, 'r', encoding='utf-8') as f:
    fragment_content = f.read()

# 1. Remove Eye Protection section
eye_pattern = re.compile(r"<!-- Eye Protection -->.*?</LinearLayout>", re.DOTALL)
fragment_content = eye_pattern.sub("", fragment_content)

# 2. Fix missing drawables
fragment_content = fragment_content.replace("@drawable/ic_delete", "@drawable/ic_error_purple")
fragment_content = fragment_content.replace("@drawable/ic_category", "@drawable/ic_date_format_purple")
fragment_content = fragment_content.replace("@drawable/ic_chart", "@drawable/ic_chart_soft")

with open(fragment_file, 'w', encoding='utf-8') as f:
    f.write(fragment_content)

print("Updated fragment_more.xml")


# 3. Update MoreFragment.kt to remove eye protection logic
fragment_kt_file = r"c:\Aboneliklerim\app\src\main\java\com\aboneliklerim\app\MoreFragment.kt"
with open(fragment_kt_file, 'r', encoding='utf-8') as f:
    kt_content = f.read()

kt_eye_pattern = re.compile(r"// --- EYE PROTECTION ---.*?// Apply eye protection globally", re.DOTALL)
kt_content = kt_eye_pattern.sub("", kt_content)

with open(fragment_kt_file, 'w', encoding='utf-8') as f:
    f.write(kt_content)

print("Updated MoreFragment.kt")
