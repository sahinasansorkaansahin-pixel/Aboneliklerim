import json
import re
import os
import requests

# Fallback prices to use if scraping fails
fallback_prices = {
    "prime_video": {
        "price": 69.90,
        "currency": "TRY",
        "plans": [{"name": "Prime Membership", "price": 69.90, "currency": "TRY", "period": "monthly"}]
    },
    "netflix": {
        "price": 149.99,
        "currency": "TRY",
        "plans": [
            {"name": "Basic (Temel)", "price": 149.99, "currency": "TRY", "period": "monthly"},
            {"name": "Standard (Standart)", "price": 229.99, "currency": "TRY", "period": "monthly"},
            {"name": "Premium (Özel)", "price": 299.99, "currency": "TRY", "period": "monthly"}
        ]
    },
    "spotify": {
        "price": 99.00,
        "currency": "TRY",
        "plans": [
            {"name": "Bireysel (Individual)", "price": 99.00, "currency": "TRY", "period": "monthly"},
            {"name": "Öğrenci (Student)", "price": 55.00, "currency": "TRY", "period": "monthly"},
            {"name": "Duo", "price": 135.00, "currency": "TRY", "period": "monthly"},
            {"name": "Aile (Family)", "price": 165.00, "currency": "TRY", "period": "monthly"}
        ]
    },
    "apple_music": {
        "price": 59.99,
        "currency": "TRY",
        "plans": [
            {"name": "Student (Öğrenci)", "price": 32.99, "currency": "TRY", "period": "monthly"},
            {"name": "Individual (Bireysel)", "price": 59.99, "currency": "TRY", "period": "monthly"},
            {"name": "Family (Aile)", "price": 99.99, "currency": "TRY", "period": "monthly"}
        ]
    }
}

def scrape_spotify():
    try:
        url = "https://www.spotify.com/tr/premium/"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        r = requests.get(url, headers=headers, timeout=10)
        match = re.search(r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>', r.text)
        if match:
            data = json.loads(match.group(1))
            plans = data.get("props", {}).get("pageProps", {}).get("components", {}).get("storefront", {}).get("plans", [])
            
            scraped_plans = []
            individual_price = 99.0
            
            for plan in plans:
                name = plan.get("shortPlanName") or plan.get("header") or ""
                # Map names to nicer Turkish/English descriptions
                if "bireysel" in name.lower() or "individual" in name.lower():
                    disp_name = "Bireysel (Individual)"
                elif "renci" in name.lower() or "student" in name.lower():
                    disp_name = "Öğrenci (Student)"
                elif "duo" in name.lower():
                    disp_name = "Duo"
                elif "aile" in name.lower() or "family" in name.lower():
                    disp_name = "Aile (Family)"
                else:
                    disp_name = name

                # Parse price from subheaderPrice or secondaryPriceDescription
                sh_price = plan.get("subheaderPrice", "")
                sec_desc = plan.get("secondaryPriceDescription", "")
                
                price_num = None
                nums_sh = re.findall(r'\d+', sh_price)
                if nums_sh:
                    price_num = float(nums_sh[0])
                else:
                    nums_sec = re.findall(r'\d+', sec_desc)
                    if nums_sec:
                        price_num = float(nums_sec[0])
                
                if price_num is not None:
                    scraped_plans.append({
                        "name": disp_name,
                        "price": price_num,
                        "currency": "TRY",
                        "period": "monthly"
                    })
                    if "bireysel" in name.lower() or "individual" in name.lower():
                        individual_price = price_num
                        
            if scraped_plans:
                print(f"Scraped Spotify (TR): Bireysel = {individual_price} TL")
                # Sort plans so Student/Individual is first
                scraped_plans.sort(key=lambda x: x["price"])
                return {
                    "price": individual_price,
                    "currency": "TRY",
                    "plans": scraped_plans
                }
    except Exception as e:
        print(f"Error scraping Spotify: {e}")
    print("Using fallback for Spotify")
    return fallback_prices["spotify"]

def scrape_apple_music():
    try:
        url = "https://www.apple.com/tr/apple-music/"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        res = requests.get(url, headers=headers, timeout=10)
        if res.status_code == 200:
            html = res.text
            text = re.sub(r'<[^>]+>', ' ', html)
            text = re.sub(r'\s+', ' ', text)
            
            student_matches = re.findall(r'(?:öğrenci|student)[^0-9]{0,40}(\d+[\.,]\d+)\s*TL', text, re.IGNORECASE)
            individual_matches = re.findall(r'(?:bireysel|individual)[^0-9]{0,40}(\d+[\.,]\d+)\s*TL', text, re.IGNORECASE)
            family_matches = re.findall(r'(?:aile|family)[^0-9]{0,40}(\d+[\.,]\d+)\s*TL', text, re.IGNORECASE)
            
            student_price = float(student_matches[0].replace(",", ".")) if student_matches else 32.99
            
            ind_prices = [float(p.replace(",", ".")) for p in individual_matches]
            ind_prices = [p for p in ind_prices if p > 20.0]  # Avoid promo 19.99
            individual_price = ind_prices[0] if ind_prices else 59.99
            
            family_price = float(family_matches[0].replace(",", ".")) if family_matches else 99.99
            
            print(f"Scraped Apple Music (TR): Bireysel = {individual_price} TL")
            return {
                "price": individual_price,
                "currency": "TRY",
                "plans": [
                    {"name": "Student (Öğrenci)", "price": student_price, "currency": "TRY", "period": "monthly"},
                    {"name": "Individual (Bireysel)", "price": individual_price, "currency": "TRY", "period": "monthly"},
                    {"name": "Family (Aile)", "price": family_price, "currency": "TRY", "period": "monthly"}
                ]
            }
    except Exception as e:
        print(f"Error scraping Apple Music: {e}")
    print("Using fallback for Apple Music")
    return fallback_prices["apple_music"]

def main():
    target_path = r"app/src/main/assets/streaming_prices.json"
    
    # Existing assets details map (to keep links, logo res ids etc.)
    meta_info = {
        "prime_video": {
            "name": "Amazon Prime Video",
            "logo_res": "ic_logo_prime_video",
            "official_url": "https://www.amazon.com.tr/prime"
        },
        "netflix": {
            "name": "Netflix",
            "logo_res": "ic_logo_netflix",
            "official_url": "https://www.netflix.com"
        },
        "spotify": {
            "name": "Spotify",
            "logo_res": "ic_logo_spotify",
            "official_url": "https://www.spotify.com/tr/premium/"
        },
        "apple_music": {
            "name": "Apple Music",
            "logo_res": "ic_logo_apple_music",
            "official_url": "https://www.apple.com/tr/apple-music/"
        }
    }
    
    print("Scraping Spotify...")
    spotify_data = scrape_spotify()
    
    print("Scraping Apple Music...")
    apple_data = scrape_apple_music()
    
    prime_data = fallback_prices["prime_video"]
    netflix_data = fallback_prices["netflix"]
    
    scraped_map = {
        "prime_video": prime_data,
        "netflix": netflix_data,
        "spotify": spotify_data,
        "apple_music": apple_data
    }
    
    updated_list = []
    for pid in ["prime_video", "netflix", "spotify", "apple_music"]:
        scraped = scraped_map[pid]
        meta = meta_info[pid]
        
        updated_plat = {
            "id": pid,
            "name": meta["name"],
            "base_price": scraped["price"],
            "base_currency": scraped["currency"],
            "logo_res": meta["logo_res"],
            "official_url": meta["official_url"],
            "plans": scraped["plans"]
        }
        updated_list.append(updated_plat)
        
    # Write back
    with open(target_path, "w", encoding="utf-8") as f:
        json.dump(updated_list, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully updated {target_path}!")
    for plat in updated_list:
        print(f"  - {plat['name']}: {plat['base_price']} {plat['base_currency']}")

if __name__ == "__main__":
    main()
