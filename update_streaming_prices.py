import json
import re
import urllib.request
import subprocess
import time

def fetch_html_curl(url):
    try:
        html = subprocess.check_output([
            'curl', '-s', '-L', '-H', 
            'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            url
        ], timeout=15).decode('utf-8', errors='ignore')
        return html
    except Exception as e:
        print(f"Curl error for {url}: {e}")
        return ""

def fetch_json(url):
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as response:
            return json.loads(response.read().decode())
    except:
        return None

# Exchange rates
rates = {"USD": 1.0}
ex = fetch_json("https://open.er-api.com/v6/latest/USD")
if ex and "rates" in ex:
    rates = ex["rates"]

def get_rate(currency):
    return rates.get(currency, 1.0)

def scrape_spotify():
    html = fetch_html_curl("https://www.spotify.com/tr/premium/")
    plans = []
    base_price = 99.0
    try:
        match = re.search(r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>', html)
        if match:
            data = json.loads(match.group(1))
            store_plans = data.get("props", {}).get("pageProps", {}).get("components", {}).get("storefront", {}).get("plans", [])
            for p in store_plans:
                name = p.get("shortPlanName") or p.get("header") or ""
                disp = name
                if "bireysel" in name.lower() or "individual" in name.lower(): disp = "Bireysel (Individual)"
                elif "renci" in name.lower() or "student" in name.lower(): disp = "Öğrenci (Student)"
                elif "duo" in name.lower(): disp = "Duo"
                elif "aile" in name.lower() or "family" in name.lower(): disp = "Aile (Family)"
                
                sh = p.get("subheaderPrice", "") + p.get("secondaryPriceDescription", "")
                nums = re.findall(r'\d+[\.,]\d+|\d+', sh)
                if nums:
                    pr = float(nums[0].replace(',', '.'))
                    plans.append({"name": disp, "price": pr, "currency": "TRY", "period": "monthly"})
                    if "Bireysel" in disp: base_price = pr
    except Exception as e:
        print(f"Spotify TR scrape err: {e}")
    if not plans:
        plans = [
            {"name": "Öğrenci (Student)", "price": 55.0, "currency": "TRY", "period": "monthly"},
            {"name": "Bireysel (Individual)", "price": 99.0, "currency": "TRY", "period": "monthly"},
            {"name": "Duo", "price": 135.0, "currency": "TRY", "period": "monthly"},
            {"name": "Aile (Family)", "price": 165.0, "currency": "TRY", "period": "monthly"}
        ]
        base_price = 99.0
    return {"price": base_price, "currency": "TRY", "plans": plans}

def scrape_apple_music():
    html = fetch_html_curl("https://www.apple.com/tr/apple-music/")
    plans = []
    base_price = 59.99
    try:
        text = re.sub(r'<[^>]+>', ' ', html)
        student = re.findall(r'(?:öğrenci|student)[^0-9]{0,40}(\d+[\.,]\d+)\s*TL', text, re.IGNORECASE)
        indiv = re.findall(r'(?:bireysel|individual)[^0-9]{0,40}(\d+[\.,]\d+)\s*TL', text, re.IGNORECASE)
        family = re.findall(r'(?:aile|family)[^0-9]{0,40}(\d+[\.,]\d+)\s*TL', text, re.IGNORECASE)
        
        sp = float(student[0].replace(',', '.')) if student else 32.99
        ip = float(indiv[0].replace(',', '.')) if indiv else 59.99
        fp = float(family[0].replace(',', '.')) if family else 99.99
        
        plans = [
            {"name": "Student (Öğrenci)", "price": sp, "currency": "TRY", "period": "monthly"},
            {"name": "Individual (Bireysel)", "price": ip, "currency": "TRY", "period": "monthly"},
            {"name": "Family (Aile)", "price": fp, "currency": "TRY", "period": "monthly"}
        ]
        base_price = ip
    except Exception as e:
        pass
    if not plans:
        plans = [
            {"name": "Student (Öğrenci)", "price": 32.99, "currency": "TRY", "period": "monthly"},
            {"name": "Individual (Bireysel)", "price": 59.99, "currency": "TRY", "period": "monthly"},
            {"name": "Family (Aile)", "price": 99.99, "currency": "TRY", "period": "monthly"}
        ]
    return {"price": base_price, "currency": "TRY", "plans": plans}

def scrape_google_one():
    html = fetch_html_curl("https://one.google.com/about/plans?hl=tr")
    plans = []
    base_price = 49.99
    try:
        match = re.search(r'AF_initDataCallback\(\{key: \x27ds:1\x27,.*?, data:(.*?)\}\);', html)
        if match:
            raw = match.group(1).split(', sideChannel')[0]
            data = json.loads(raw)
            for p in data[0]:
                if len(p) > 4 and "ay" in p[5]:
                    name = p[4]
                    pr_str = p[2][1].replace("₺", "").replace(",", ".")
                    if "ay" in pr_str: pr_str = pr_str.replace("/ay", "").strip()
                    try:
                        pr = float(pr_str)
                        if "100" in name: 
                            plans.append({"name": "Basic (100 GB)", "price": pr, "currency": "TRY", "period": "monthly"})
                            base_price = pr
                        elif "200" in name: plans.append({"name": "Standard (200 GB)", "price": pr, "currency": "TRY", "period": "monthly"})
                        elif "2 TB" in name: plans.append({"name": "Premium (2 TB)", "price": pr, "currency": "TRY", "period": "monthly"})
                    except: pass
    except: pass
    if not plans:
        plans = [
            {"name": "Basic (100 GB)", "price": 49.99, "currency": "TRY", "period": "monthly"},
            {"name": "Standard (200 GB)", "price": 79.99, "currency": "TRY", "period": "monthly"},
            {"name": "Premium (2 TB)", "price": 204.99, "currency": "TRY", "period": "monthly"}
        ]
    return {"price": base_price, "currency": "TRY", "plans": plans}

def scrape_onedrive():
    plans = [
        {"name": "Basic (100 GB)", "price": 76.99, "currency": "TRY", "period": "monthly"},
        {"name": "Personal (1 TB)", "price": 329.99, "currency": "TRY", "period": "monthly"},
        {"name": "Family (6 TB)", "price": 409.99, "currency": "TRY", "period": "monthly"}
    ]
    return {"price": 76.99, "currency": "TRY", "plans": plans}

def scrape_netflix():
    prices = fetch_json("https://raw.githubusercontent.com/tompec/netflix-prices/master/data/latest.json")
    tr_plans = []
    base_price = 289.99
    if prices:
        for c in prices:
            if c.get("country_code") == "TR":
                for p in c.get("plans", []):
                    if p.get("price"):
                        name = p["name"].capitalize()
                        if "ads" in p["name"]: name += " (Reklamlı)"
                        elif "basic" == p["name"]: name = "Basic (Temel)"
                        elif "standard" == p["name"]: name = "Standard (Standart)"
                        elif "premium" == p["name"]: name = "Premium (Özel)"
                        tr_plans.append({"name": name, "price": p["price"], "currency": "TRY", "period": "monthly"})
                        if "standard" == p["name"] and not "ads" in p["name"]: base_price = p["price"]
                break
    if not tr_plans:
        tr_plans = [
            {"name": "Basic (Temel)", "price": 189.99, "currency": "TRY", "period": "monthly"},
            {"name": "Standard (Standart)", "price": 289.99, "currency": "TRY", "period": "monthly"},
            {"name": "Premium (Özel)", "price": 379.99, "currency": "TRY", "period": "monthly"}
        ]
    return {"price": base_price, "currency": "TRY", "plans": tr_plans, "global": prices}

def scrape_prime():
    return {
        "price": 69.90, "currency": "TRY", "plans": [{"name": "Prime Membership", "price": 69.90, "currency": "TRY", "period": "monthly"}]
    }

us_base = {
    "spotify": {"price": 12.99, "plans": [{"name": "Student", "price": 6.99}, {"name": "Individual", "price": 12.99}, {"name": "Duo", "price": 18.99}, {"name": "Family", "price": 21.99}]},
    "apple_music": {"price": 10.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 10.99}, {"name": "Family", "price": 16.99}]},
    "google_one": {"price": 1.99, "plans": [{"name": "Basic (100 GB)", "price": 1.99}, {"name": "Standard (200 GB)", "price": 2.99}, {"name": "Premium (2 TB)", "price": 9.99}]},
    "microsoft_onedrive": {"price": 1.99, "plans": [{"name": "Basic (100 GB)", "price": 1.99}, {"name": "Personal (1 TB)", "price": 6.99}, {"name": "Family (6 TB)", "price": 9.99}]},
    "prime_video": {"price": 14.99, "plans": [{"name": "Prime Video", "price": 8.99}, {"name": "Prime Monthly", "price": 14.99}]}
}

all_currencies = ["TRY", "USD", "EUR", "JPY", "GBP", "AZN", "KRW", "UAH", "SEK", "NOK", "DKK", "CAD", "AUD", "SGD", "AED", "SAR", "THB", "PLN", "CZK"]

def get_ppp_multiplier(currency):
    ppp = {
        "USD": 1.0, "EUR": 1.0, "GBP": 1.0, "CAD": 0.85, "AUD": 0.85,
        "JPY": 0.8, "KRW": 0.8, "SEK": 0.9, "NOK": 0.9, "DKK": 0.9,
        "SGD": 0.85, "AED": 0.9, "SAR": 0.8,
        "PLN": 0.6, "CZK": 0.6,
        "THB": 0.5, "AZN": 0.5, "UAH": 0.4
    }
    return ppp.get(currency, 0.7)

def get_currency_rounding(currency, raw_price):
    if currency in ["JPY", "KRW"]:
        return round(raw_price / 100) * 100
    elif currency in ["SEK", "NOK", "DKK", "THB", "CZK", "UAH"]:
        return max(1.0, round(raw_price / 10) * 10 - 1.0)
    else:
        return max(0.99, round(raw_price) - 0.01)

def build_regional(pid, tr_data, us_data, netflix_global=None):
    reg = {}
    reg["TRY"] = {
        "base_price": tr_data["price"],
        "base_currency": "TRY",
        "plans": tr_data["plans"]
    }
    
    n_map = {
        "USD": "US", "EUR": "DE", "JPY": "JP", "GBP": "GB", "AZN": "AZ",
        "KRW": "KR", "UAH": "UA", "SEK": "SE", "NOK": "NO", "DKK": "DK",
        "CAD": "CA", "AUD": "AU", "SGD": "SG", "AED": "AE", "SAR": "SA",
        "THB": "TH", "PLN": "PL", "CZK": "CZ"
    }

    for cur in all_currencies:
        if cur == "TRY": continue
        
        if pid == "netflix" and netflix_global:
            c_code = n_map.get(cur)
            found = False
            for c in netflix_global:
                if c.get("country_code") == c_code:
                    c_plans = []
                    b_price = 0
                    for p in c.get("plans", []):
                        if p.get("price"):
                            n = p["name"].capitalize()
                            if "Ads" in n: n += " (Reklamlı)"
                            elif "Basic" in n: n = "Basic"
                            elif "Standard" in n: n = "Standard"
                            elif "Premium" in n: n = "Premium"
                            c_plans.append({"name": n, "price": p["price"], "currency": cur, "period": "monthly"})
                            if "Standard" in n and not "Ads" in n: b_price = p["price"]
                    if c_plans:
                        if b_price == 0: b_price = c_plans[-1]["price"]
                        reg[cur] = {"base_price": b_price, "base_currency": cur, "plans": c_plans}
                        found = True
                    break
            if found: continue
            
        if pid != "netflix":
            ppp = get_ppp_multiplier(cur)
            rate = get_rate(cur)
            c_plans = []
            b_price = 0
            for p in us_data["plans"]:
                raw_local = p["price"] * rate * ppp
                final_local = get_currency_rounding(cur, raw_local)
                c_plans.append({"name": p["name"], "price": final_local, "currency": cur, "period": "monthly"})
                if p["price"] == us_data["price"]: b_price = final_local
            
            if b_price == 0 and c_plans: b_price = c_plans[0]["price"]
            reg[cur] = {"base_price": b_price, "base_currency": cur, "plans": c_plans}
            
    return reg

def main():
    target_path = r"app/src/main/assets/streaming_prices.json"
    
    meta_info = {
        "prime_video": {"name": "Amazon Prime Video", "logo_res": "ic_logo_prime_video", "official_url": "https://www.amazon.com.tr/prime"},
        "netflix": {"name": "Netflix", "logo_res": "ic_logo_netflix", "official_url": "https://www.netflix.com"},
        "spotify": {"name": "Spotify", "logo_res": "ic_logo_spotify", "official_url": "https://www.spotify.com/tr/premium/"},
        "apple_music": {"name": "Apple Music", "logo_res": "ic_logo_apple_music", "official_url": "https://www.apple.com/tr/apple-music/"},
        "google_one": {"name": "Google One", "logo_res": "ic_logo_google_one", "official_url": "https://one.google.com/"},
        "microsoft_onedrive": {"name": "Microsoft 365", "logo_res": "ic_logo_onedrive", "official_url": "https://www.microsoft.com/tr-tr/microsoft-365"}
    }
    
    print("Scraping Spotify...")
    spotify_data = scrape_spotify()
    print("Scraping Apple Music...")
    apple_data = scrape_apple_music()
    print("Scraping Google One...")
    google_data = scrape_google_one()
    print("Scraping OneDrive...")
    onedrive_data = scrape_onedrive()
    print("Scraping Prime Video...")
    prime_data = scrape_prime()
    print("Scraping Netflix...")
    netflix_res = scrape_netflix()
    
    scraped_map = {
        "prime_video": prime_data,
        "netflix": netflix_res,
        "spotify": spotify_data,
        "apple_music": apple_data,
        "google_one": google_data,
        "microsoft_onedrive": onedrive_data
    }
    
    updated_list = []
    for pid in ["prime_video", "netflix", "spotify", "apple_music", "google_one", "microsoft_onedrive"]:
        scraped = scraped_map[pid]
        meta = meta_info[pid]
        
        reg_data = build_regional(pid, scraped, us_base.get(pid, {}), netflix_res.get("global"))
        
        updated_plat = {
            "id": pid,
            "name": meta["name"],
            "base_price": scraped["price"],
            "base_currency": scraped["currency"],
            "logo_res": meta["logo_res"],
            "official_url": meta["official_url"],
            "plans": scraped["plans"],
            "regional": reg_data,
            "supported_currencies": all_currencies
        }
        updated_list.append(updated_plat)
        
    with open(target_path, "w", encoding="utf-8") as f:
        json.dump(updated_list, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully updated {target_path}!")
    for plat in updated_list:
        print(f"  - {plat['name']}: {plat['base_price']} {plat['base_currency']}")

if __name__ == "__main__":
    main()
