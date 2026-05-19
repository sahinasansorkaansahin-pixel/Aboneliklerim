import os
import re

res_dir = r'c:\Aboneliklerim\app\src\main\res'
targets = [d for d in os.listdir(res_dir) if d.startswith("values-")]

for val_dir in targets:
    target_file = os.path.join(res_dir, val_dir, 'strings.xml')
    if not os.path.exists(target_file):
        continue
        
    with open(target_file, 'r', encoding='utf-8') as f:
        content = f.read()
        
    orig = content
    content = content.replace(r"\\'", r"\'")
    content = content.replace(r"\\\"", r"\"")
    # Also fix unescaped ampersands
    # This is tricky without parsing, but we can look for & that isn't followed by amp; or #
    content = re.sub(r'&(?!(amp;|#\d+;|[a-zA-Z]+;))', '&amp;', content)

    if orig != content:
        with open(target_file, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Fixed {val_dir}")
