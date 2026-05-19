import os
import re

res_dir = r'app/src/main/res'

for root_dir, dirs, files in os.walk(res_dir):
    for file in files:
        if file == 'strings.xml':
            file_path = os.path.join(root_dir, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Regex to find one or more backslashes followed by a single quote
            # and replace it with exactly a single backslash followed by a single quote.
            fixed_content = re.sub(r'\\+\'', r"\'", content)
            
            if fixed_content != content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(fixed_content)
                print(f"Fixed over-escaped quotes in: {file_path}")

print("Clean-up of over-escaped quotes complete.")
