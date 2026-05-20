import re

# 1. Update StreamingPriceService.kt (cache checks)
with open('app/src/main/java/com/aboneliklerim/app/StreamingPriceService.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if 'return@withContext resolvePlatformsForUserCurrency(context, filtered)' in line:
        if len(new_lines) > 0 and 'google_one' in new_lines[-1] and 'list.filter' in new_lines[-1]:
            indent = line[:line.find('return@withContext')]
            new_lines.append(indent + 'if (filtered.any { it.id == "google_one" }) {\n')
            new_lines.append(indent + '    return@withContext resolvePlatformsForUserCurrency(context, filtered)\n')
            new_lines.append(indent + '}\n')
            continue
    new_lines.append(line)

with open('app/src/main/java/com/aboneliklerim/app/StreamingPriceService.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("StreamingPriceService.kt updated successfully.")


# 2. Update fragment_more.xml using regex
with open('app/src/main/res/layout/fragment_more.xml', 'r', encoding='utf-8') as f:
    xml_content = f.read()

# Replace ImageViews (ic_lock)
# We match:
# <ImageView 
#     android:layout_width="40dp" 
#     android:layout_height="40dp" 
#     android:src="@drawable/ic_lock"
# and replace 40dp with 24dp
pattern_image = re.compile(
    r'(<ImageView\s+[^>]*?android:layout_width=")(40dp)("\s+[^>]*?android:layout_height=")(40dp)("\s+[^>]*?android:src="@drawable/ic_lock")',
    re.DOTALL
)

xml_content_new, count_img = pattern_image.subn(r'\1 24dp \3 24dp \5', xml_content)
print(f"Replaced {count_img} ImageView lock icons.")

# Replace TextViews (premium_get_service)
# We match:
# <TextView ... android:text="@string/premium_get_service" ... android:textSize="14sp" ... android:layout_marginTop="8dp" ... />
# and replace 14sp with 12sp, and 8dp with 4dp
pattern_text = re.compile(
    r'(<TextView\s+[^>]*?android:text="@string/premium_get_service"[^>]*?android:textSize=")(14sp)("[^>]*?android:layout_marginTop=")(8dp)(")',
    re.DOTALL
)

xml_content_final, count_txt = pattern_text.subn(r'\1 12sp \3 4dp \5', xml_content_new)
print(f"Replaced {count_txt} TextView locks.")

# Also try another order just in case android:layout_marginTop comes before android:textSize
if count_txt == 0:
    pattern_text_alt = re.compile(
        r'(<TextView\s+[^>]*?android:text="@string/premium_get_service"[^>]*?android:layout_marginTop=")(8dp)("[^>]*?android:textSize=")(14sp)(")',
        re.DOTALL
    )
    xml_content_final, count_txt = pattern_text_alt.subn(r'\1 4dp \3 12sp \5', xml_content_new)
    print(f"Replaced {count_txt} TextView locks (alternative order).")

with open('app/src/main/res/layout/fragment_more.xml', 'w', encoding='utf-8') as f:
    f.write(xml_content_final)

print("fragment_more.xml updated successfully.")
