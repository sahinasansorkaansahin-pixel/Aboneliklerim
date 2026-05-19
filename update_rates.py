import urllib.request
import json
import re

url = "https://open.er-api.com/v6/latest/TRY"
target_file = r"app/src/main/java/com/aboneliklerim/app/CurrencyService.kt"

# The 20 rich currencies for our 20 premium countries/languages
currency_list = [
    "TRY", "USD", "EUR", "JPY", "GBP", "CHF", "KRW", "CNY", "SEK", "NOK", "DKK", "CAD", "AUD", "SGD", "AED", "SAR", "ILS", "RUB", "PLN", "CZK"
]

try:
    print("Fetching live exchange rates from API (Base: TRY)...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        
    if data.get("result") == "success":
        rates = data.get("rates", {})
        
        # We want 1 TRY = X Currency rates
        final_rates = {}
        for currency in sorted(currency_list):
            rate = rates.get(currency)
            if rate is not None and rate > 0:
                final_rates[currency] = round(rate, 6)
            else:
                final_rates[currency] = 1.0
                
        # Generate Kotlin mapOf declaration
        rates_entries = []
        for currency in sorted(final_rates.keys()):
            val = final_rates[currency]
            rates_entries.append(f'        "{currency}" to {val}')
            
        map_declaration = "    private val fallbackRates = mapOf(\n" + ",\n".join(rates_entries) + "\n    )"
        
        # Read CurrencyService.kt
        with open(target_file, "r", encoding="utf-8") as f:
            content = f.read()
            
        # Replace old fallbackRates block with new one
        pattern = r"private val fallbackRates = mapOf\s*\([\s\S]*?\)"
        updated_content = re.sub(pattern, map_declaration, content)
        
        with open(target_file, "w", encoding="utf-8") as f:
            f.write(updated_content)
            
        print("Successfully updated CurrencyService.kt with live exchange rates for 20 rich currencies!")
        for c, r in sorted(final_rates.items()):
            print(f"  - 1 TRY = {r} {c}")
            
    else:
        print("API returned failure status.")
except Exception as e:
    print(f"Error fetching live rates or updating: {e}")
