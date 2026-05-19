import os
import re

mappings = {
    "tr": "Maliyet Paylaşımı",
    "fr": "Partage des Coûts",
    "de": "Kostenaufteilung",
    "es": "Compartir Costos",
    "it": "Divisione Spese",
    "pt": "Divisão de Custos",
    "nl": "Kosten Delen",
    "no": "Kostnadsdeling",
    "sv": "Kostnadsdelning",
    "da": "Deling af omkostninger",
    "fi": "Kustannusten jakaminen",
    "is": "Kostnaðarhlutdeild",
    "pl": "Dzielenie kosztów",
    "cs": "Sdílení nákladů",
    "hu": "Költségmegosztás",
    "ro": "Împărțirea costurilor",
    "bg": "Споделяне на разходи",
    "ru": "Разделение расходов",
    "uk": "Розподіл витрат",
    "ja": "コスト共有",
    "ko": "비용 공유",
    "zh": "费用分摊",
    "ar": "تقاسم التكاليف",
    "hi": "लागत साझाकरण",
    "id": "Berbagi Biaya",
    "ms": "Kongsi Kos",
    "th": "การแชร์ค่าใช้จ่าย",
    "vi": "Chia sẻ chi phí",
    "az": "Xərc Paylaşımı",
    "ka": "ხარჯების გაზიარება",
    "kk": "Шығындарды бөлісу",
    "uz": "Xarajatlarni bo'lishish",
    "fa": "تقسیم هزینه‌ها",
    "ur": "اخراجات کی تقسیم",
    "af": "Kostedeling",
    "bn": "খরচ ভাগাভাগি",
    "bs": "Podjela troškova",
    "ca": "Compartir Costos",
    "et": "Kulude jagamine",
    "ga": "Comhroinnt Costas",
    "hr": "Podjela troškova",
    "lb": "Koste deelen",
    "lt": "Išlaidų dalijimasis",
    "lv": "Izmaksu kopīgošana",
    "mt": "Qsim tal-Ispejjeż",
    "sk": "Zdieľanie nákladov",
    "sl": "Porazdelitev stroškov",
    "sq": "Ndarja e kostove",
    "sr": "Подела трошкова",
    "ta": "செலவு பகிர்வு",
    "te": "ఖర్చు భాగస్వామ్యం",
    "fil": "Paghahati sa Gastos"
}

res_path = "app/src/main/res"
updated_count = 0

for folder in os.listdir(res_path):
    if folder.startswith("values"):
        xml_path = os.path.join(res_path, folder, "strings.xml")
        if os.path.exists(xml_path):
            with open(xml_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            # Find current onboarding_title_9
            match = re.search(r'<string name="onboarding_title_9">(.*?)</string>', content)
            if match:
                # Determine language code from folder name
                lang = "en"
                if "-" in folder:
                    parts = folder.split("-")
                    lang_part = parts[1]
                    # handle region codes
                    if len(lang_part) > 3 and lang_part[2] == "r":
                        lang = lang_part[:2]
                    else:
                        lang = lang_part
                
                # Get the mapped short name
                short_name = mappings.get(lang, "Cost Sharing")
                
                # Replace
                new_content = re.sub(
                    r'<string name="onboarding_title_9">.*?</string>',
                    f'<string name="onboarding_title_9">{short_name}</string>',
                    content
                )
                
                with open(xml_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                updated_count += 1

print(f"Successfully updated onboarding_title_9 in {updated_count} files!")
