import re

# English Strings
en_strings = """
    <string name="premium_title">Premium Service</string>
    <string name="premium_subtitle">Supercharge your subscriptions!</string>
    <string name="premium_get_service">💎 Get Premium Service</string>
    <string name="plan_monthly">Monthly Plan</string>
    <string name="plan_yearly">Yearly Plan</string>
    <string name="plan_lifetime">Lifetime Plan</string>
    <string name="choose_plan">Choose a Plan</string>
    <string name="premium_limit_reached">You can only create up to 8 subscriptions on the free plan. Upgrade to Premium to remove limits!</string>
    <string name="premium_limit_max">You have reached the maximum limit of 100 subscriptions.</string>
</resources>
"""

# Turkish Strings
tr_strings = """
    <string name="premium_title">Premium Hizmet</string>
    <string name="premium_subtitle">Aboneliklerinizi uçurun!</string>
    <string name="premium_get_service">💎 Premium Hizmeti Al</string>
    <string name="plan_monthly">Aylık Plan</string>
    <string name="plan_yearly">Yıllık Plan</string>
    <string name="plan_lifetime">Ömür Boyu (Life-time) Plan</string>
    <string name="choose_plan">Bir Plan Seçin</string>
    <string name="premium_limit_reached">Ücretsiz planda en fazla 8 abonelik oluşturabilirsiniz. Sınırları kaldırmak için Premium\\'a geçin!</string>
    <string name="premium_limit_max">Maksimum 100 abonelik sınırına ulaştınız.</string>
</resources>
"""

en_path = r'c:\Aboneliklerim\app\src\main\res\values\strings.xml'
with open(en_path, 'r', encoding='utf-8') as f:
    en_content = f.read()
en_content = en_content.replace('</resources>', en_strings)
with open(en_path, 'w', encoding='utf-8') as f:
    f.write(en_content)

tr_path = r'c:\Aboneliklerim\app\src\main\res\values-tr\strings.xml'
with open(tr_path, 'r', encoding='utf-8') as f:
    tr_content = f.read()
tr_content = tr_content.replace('</resources>', tr_strings)
with open(tr_path, 'w', encoding='utf-8') as f:
    f.write(tr_content)

print("Added Premium strings to EN and TR")
