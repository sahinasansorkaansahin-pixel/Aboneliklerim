import os
import re

res_dir = r'app/src/main/res'

for root_dir, dirs, files in os.walk(res_dir):
    for file in files:
        if file == 'strings.xml':
            file_path = os.path.join(root_dir, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Regex to find '&' that is NOT part of a standard entity reference
            # Matches any '&' that is NOT followed by (amp|lt|gt|quot|apos);
            # Note: Android strings might also have others like \uXXXX or HTML tags.
            # But the main XML entities are amp, lt, gt, quot, apos.
            # Let's match '&' that is not followed by some alphanumeric chars and a semicolon.
            # Actually, standard regex: &(?!amp;|lt;|gt;|quot;|apos;)
            # Or just replace any '&' that is not followed by: (amp;|lt;|gt;|quot;|apos;|#)
            # Let's use a negative lookahead:
            fixed_content = re.sub(r'&(?!(amp;|lt;|gt;|quot;|apos;|#\d+;|#x[a-fA-F0-9]+;))', '&amp;', content)
            
            # Also fix unescaped single quotes "'" inside XML tags
            # Android requires single quotes in string resources to be escaped as \' or wrapped in double quotes
            # Our translate_batch.py already did replace("'", "\\'"), but let's double check
            # Wait, does the translator sometimes translate \' to ' ?
            # Yes! Translator sometimes outputs ' instead of \'!
            # Let's fix unescaped single quotes:
            # We want to replace any single quote that is not preceded by a backslash, inside the string tag content.
            # But regex for that can be tricky. Let's do it simply:
            # First, let's find the content between > and </string>
            def escape_quotes(match):
                tag_open = match.group(1)
                text = match.group(2)
                tag_close = match.group(3)
                
                # Replace unescaped ' with \'
                # We replace any ' not preceded by \
                escaped_text = re.sub(r'(?<!\\)\'', r"\'", text)
                return f"{tag_open}{escaped_text}{tag_close}"
            
            fixed_content = re.compile(r'(<string[^>]*>)(.*?)(</string>)', re.DOTALL).sub(escape_quotes, fixed_content)
            
            if fixed_content != content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(fixed_content)
                print(f"Fixed unescaped entities/quotes in: {file_path}")

print("Entity/Quote fix completed.")
