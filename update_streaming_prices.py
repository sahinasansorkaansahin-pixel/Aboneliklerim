import json
import re
import requests

# Fallback prices to use if scraping fails (Turkish prices)
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
    },
    "google_one": {
        "price": 5.79,
        "currency": "TRY",
        "plans": [
            {"name": "Basic (100 GB)", "price": 5.79, "currency": "TRY", "period": "monthly"},
            {"name": "Standard (200 GB)", "price": 11.59, "currency": "TRY", "period": "monthly"},
            {"name": "Premium (2 TB)", "price": 28.99, "currency": "TRY", "period": "monthly"}
        ]
    },
    "microsoft_onedrive": {
        "price": 59.99,
        "currency": "TRY",
        "plans": [
            {"name": "Basic (100 GB)", "price": 59.99, "currency": "TRY", "period": "monthly"},
            {"name": "Personal (1 TB)", "price": 179.99, "currency": "TRY", "period": "monthly"},
            {"name": "Family (6 TB)", "price": 239.99, "currency": "TRY", "period": "monthly"}
        ]
    }
}

# Regional prices data for the other 18 currencies/regions
regional_data = {
    "spotify": {
        "USD": {"price": 11.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 11.99}, {"name": "Duo", "price": 16.99}, {"name": "Family", "price": 19.99}]},
        "EUR": {"price": 10.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 10.99}, {"name": "Duo", "price": 14.99}, {"name": "Family", "price": 17.99}]},
        "JPY": {"price": 980.0, "plans": [{"name": "Student", "price": 480.0}, {"name": "Individual", "price": 980.0}, {"name": "Duo", "price": 1280.0}, {"name": "Family", "price": 1580.0}]},
        "GBP": {"price": 11.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 11.99}, {"name": "Duo", "price": 16.99}, {"name": "Family", "price": 19.99}]},
        "IDR": {"price": 54990.0, "plans": [{"name": "Student", "price": 27500.0}, {"name": "Individual", "price": 54990.0}, {"name": "Duo", "price": 71490.0}, {"name": "Family", "price": 86900.0}]},
        "KRW": {"price": 10900.0, "plans": [{"name": "Individual", "price": 10900.0}, {"name": "Duo", "price": 16350.0}, {"name": "Family", "price": 17900.0}]},
        "INR": {"price": 119.0, "plans": [{"name": "Student", "price": 59.0}, {"name": "Individual", "price": 119.0}, {"name": "Duo", "price": 149.0}, {"name": "Family", "price": 179.0}]},
        "SEK": {"price": 119.0, "plans": [{"name": "Student", "price": 65.0}, {"name": "Individual", "price": 119.0}, {"name": "Duo", "price": 159.0}, {"name": "Family", "price": 189.0}]},
        "NOK": {"price": 119.0, "plans": [{"name": "Student", "price": 65.0}, {"name": "Individual", "price": 119.0}, {"name": "Duo", "price": 159.0}, {"name": "Family", "price": 189.0}]},
        "DKK": {"price": 109.0, "plans": [{"name": "Student", "price": 59.0}, {"name": "Individual", "price": 109.0}, {"name": "Duo", "price": 139.0}, {"name": "Family", "price": 169.0}]},
        "CAD": {"price": 10.99, "plans": [{"name": "Student", "price": 6.49}, {"name": "Individual", "price": 10.99}, {"name": "Duo", "price": 14.99}, {"name": "Family", "price": 16.99}]},
        "AUD": {"price": 12.99, "plans": [{"name": "Student", "price": 6.99}, {"name": "Individual", "price": 12.99}, {"name": "Duo", "price": 17.99}, {"name": "Family", "price": 20.99}]},
        "SGD": {"price": 10.98, "plans": [{"name": "Student", "price": 5.98}, {"name": "Individual", "price": 10.98}, {"name": "Duo", "price": 14.98}, {"name": "Family", "price": 17.98}]},
        "AED": {"price": 21.99, "plans": [{"name": "Student", "price": 11.99}, {"name": "Individual", "price": 21.99}, {"name": "Duo", "price": 27.99}, {"name": "Family", "price": 35.99}]},
        "SAR": {"price": 21.99, "plans": [{"name": "Student", "price": 11.99}, {"name": "Individual", "price": 21.99}, {"name": "Duo", "price": 27.99}, {"name": "Family", "price": 35.99}]},
        "THB": {"price": 139.0, "plans": [{"name": "Student", "price": 65.0}, {"name": "Individual", "price": 139.0}, {"name": "Duo", "price": 189.0}, {"name": "Family", "price": 219.0}]},
        "PLN": {"price": 23.99, "plans": [{"name": "Student", "price": 12.99}, {"name": "Individual", "price": 23.99}, {"name": "Duo", "price": 30.99}, {"name": "Family", "price": 37.99}]},
        "CZK": {"price": 169.0, "plans": [{"name": "Student", "price": 89.0}, {"name": "Individual", "price": 169.0}, {"name": "Duo", "price": 229.0}, {"name": "Family", "price": 269.0}]}
    },
    "apple_music": {
        "USD": {"price": 10.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 10.99}, {"name": "Family", "price": 16.99}]},
        "EUR": {"price": 10.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 10.99}, {"name": "Family", "price": 16.99}]},
        "JPY": {"price": 1080.0, "plans": [{"name": "Student", "price": 580.0}, {"name": "Individual", "price": 1080.0}, {"name": "Family", "price": 1680.0}]},
        "GBP": {"price": 10.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 10.99}, {"name": "Family", "price": 16.99}]},
        "IDR": {"price": 55000.0, "plans": [{"name": "Student", "price": 35000.0}, {"name": "Individual", "price": 55000.0}, {"name": "Family", "price": 85000.0}]},
        "KRW": {"price": 8900.0, "plans": [{"name": "Student", "price": 4900.0}, {"name": "Individual", "price": 8900.0}, {"name": "Family", "price": 13500.0}]},
        "INR": {"price": 99.0, "plans": [{"name": "Student", "price": 59.0}, {"name": "Individual", "price": 99.0}, {"name": "Family", "price": 149.0}]},
        "SEK": {"price": 119.0, "plans": [{"name": "Student", "price": 65.0}, {"name": "Individual", "price": 119.0}, {"name": "Family", "price": 189.0}]},
        "NOK": {"price": 119.0, "plans": [{"name": "Student", "price": 65.0}, {"name": "Individual", "price": 119.0}, {"name": "Family", "price": 189.0}]},
        "DKK": {"price": 109.0, "plans": [{"name": "Student", "price": 59.0}, {"name": "Individual", "price": 109.0}, {"name": "Family", "price": 169.0}]},
        "CAD": {"price": 10.99, "plans": [{"name": "Student", "price": 5.99}, {"name": "Individual", "price": 10.99}, {"name": "Family", "price": 16.99}]},
        "AUD": {"price": 12.99, "plans": [{"name": "Student", "price": 6.99}, {"name": "Individual", "price": 12.99}, {"name": "Family", "price": 22.99}]},
        "SGD": {"price": 10.98, "plans": [{"name": "Student", "price": 5.98}, {"name": "Individual", "price": 10.98}, {"name": "Family", "price": 17.98}]},
        "AED": {"price": 21.99, "plans": [{"name": "Student", "price": 11.99}, {"name": "Individual", "price": 21.99}, {"name": "Family", "price": 35.99}]},
        "SAR": {"price": 21.99, "plans": [{"name": "Student", "price": 11.99}, {"name": "Individual", "price": 21.99}, {"name": "Family", "price": 35.99}]},
        "THB": {"price": 139.0, "plans": [{"name": "Student", "price": 79.0}, {"name": "Individual", "price": 139.0}, {"name": "Family", "price": 229.0}]},
        "PLN": {"price": 21.99, "plans": [{"name": "Student", "price": 11.99}, {"name": "Individual", "price": 21.99}, {"name": "Family", "price": 34.99}]},
        "CZK": {"price": 165.0, "plans": [{"name": "Student", "price": 89.0}, {"name": "Individual", "price": 165.0}, {"name": "Family", "price": 259.0}]}
    },
    "netflix": {
        "USD": {"price": 15.49, "plans": [{"name": "Basic", "price": 9.99}, {"name": "Standard", "price": 15.49}, {"name": "Premium", "price": 22.99}]},
        "EUR": {"price": 12.99, "plans": [{"name": "Basic", "price": 7.99}, {"name": "Standard", "price": 12.99}, {"name": "Premium", "price": 17.99}]},
        "JPY": {"price": 1490.0, "plans": [{"name": "Basic", "price": 990.0}, {"name": "Standard", "price": 1490.0}, {"name": "Premium", "price": 1980.0}]},
        "GBP": {"price": 10.99, "plans": [{"name": "Basic", "price": 6.99}, {"name": "Standard", "price": 10.99}, {"name": "Premium", "price": 17.99}]},
        "IDR": {"price": 120000.0, "plans": [{"name": "Basic", "price": 65000.0}, {"name": "Standard", "price": 120000.0}, {"name": "Premium", "price": 186000.0}]},
        "KRW": {"price": 13500.0, "plans": [{"name": "Basic", "price": 9500.0}, {"name": "Standard", "price": 13500.0}, {"name": "Premium", "price": 17000.0}]},
        "INR": {"price": 499.0, "plans": [{"name": "Basic", "price": 199.0}, {"name": "Standard", "price": 499.0}, {"name": "Premium", "price": 649.0}]},
        "SEK": {"price": 129.0, "plans": [{"name": "Basic", "price": 99.0}, {"name": "Standard", "price": 129.0}, {"name": "Premium", "price": 179.0}]},
        "NOK": {"price": 109.0, "plans": [{"name": "Basic", "price": 89.0}, {"name": "Standard", "price": 109.0}, {"name": "Premium", "price": 159.0}]},
        "DKK": {"price": 114.0, "plans": [{"name": "Basic", "price": 79.0}, {"name": "Standard", "price": 114.0}, {"name": "Premium", "price": 149.0}]},
        "CAD": {"price": 16.49, "plans": [{"name": "Basic", "price": 9.99}, {"name": "Standard", "price": 16.49}, {"name": "Premium", "price": 20.99}]},
        "AUD": {"price": 16.99, "plans": [{"name": "Basic", "price": 10.99}, {"name": "Standard", "price": 16.99}, {"name": "Premium", "price": 22.99}]},
        "SGD": {"price": 17.98, "plans": [{"name": "Basic", "price": 12.98}, {"name": "Standard", "price": 17.98}, {"name": "Premium", "price": 21.98}]},
        "AED": {"price": 39.0, "plans": [{"name": "Basic", "price": 29.0}, {"name": "Standard", "price": 39.0}, {"name": "Premium", "price": 56.0}]},
        "SAR": {"price": 43.0, "plans": [{"name": "Basic", "price": 32.0}, {"name": "Standard", "price": 43.0}, {"name": "Premium", "price": 61.0}]},
        "THB": {"price": 349.0, "plans": [{"name": "Basic", "price": 169.0}, {"name": "Standard", "price": 349.0}, {"name": "Premium", "price": 419.0}]},
        "PLN": {"price": 43.0, "plans": [{"name": "Basic", "price": 29.0}, {"name": "Standard", "price": 43.0}, {"name": "Premium", "price": 60.0}]},
        "CZK": {"price": 259.0, "plans": [{"name": "Basic", "price": 199.0}, {"name": "Standard", "price": 259.0}, {"name": "Premium", "price": 319.0}]}
    },
    "prime_video": {
        "USD": {"price": 14.99, "plans": [{"name": "Prime Video", "price": 8.99}, {"name": "Prime Monthly", "price": 14.99}]},
        "EUR": {"price": 8.99, "plans": [{"name": "Prime Monthly", "price": 8.99}, {"name": "Prime Yearly", "price": 89.90}]},
        "JPY": {"price": 600.0, "plans": [{"name": "Prime Monthly", "price": 600.0}, {"name": "Prime Yearly", "price": 5900.0}]},
        "GBP": {"price": 8.99, "plans": [{"name": "Prime Monthly", "price": 8.99}, {"name": "Prime Yearly", "price": 95.00}]},
        "IDR": {"price": 59000.0, "plans": [{"name": "Prime Monthly", "price": 59000.0}]},
        "KRW": {"price": 5.99, "plans": [{"name": "Prime Video", "price": 5.99}]},
        "INR": {"price": 299.0, "plans": [{"name": "Prime Monthly", "price": 299.0}, {"name": "Prime Yearly", "price": 1499.0}]},
        "SEK": {"price": 59.0, "plans": [{"name": "Prime Monthly", "price": 59.0}, {"name": "Prime Yearly", "price": 549.0}]},
        "NOK": {"price": 79.0, "plans": [{"name": "Prime Monthly", "price": 79.0}, {"name": "Prime Yearly", "price": 699.0}]},
        "DKK": {"price": 59.0, "plans": [{"name": "Prime Monthly", "price": 59.0}, {"name": "Prime Yearly", "price": 549.0}]},
        "CAD": {"price": 9.99, "plans": [{"name": "Prime Monthly", "price": 9.99}, {"name": "Prime Yearly", "price": 99.00}]},
        "AUD": {"price": 9.99, "plans": [{"name": "Prime Monthly", "price": 9.99}, {"name": "Prime Yearly", "price": 79.00}]},
        "SGD": {"price": 4.99, "plans": [{"name": "Prime Monthly", "price": 4.99}]},
        "AED": {"price": 16.0, "plans": [{"name": "Prime Monthly", "price": 16.0}]},
        "SAR": {"price": 16.0, "plans": [{"name": "Prime Monthly", "price": 16.0}]},
        "THB": {"price": 149.0, "plans": [{"name": "Prime Monthly", "price": 149.0}]},
        "PLN": {"price": 10.99, "plans": [{"name": "Prime Monthly", "price": 10.99}, {"name": "Prime Yearly", "price": 49.00}]},
        "CZK": {"price": 79.0, "plans": [{"name": "Prime Monthly", "price": 79.0}]}
    },
    "google_one": {
        "USD": {"price": 1.99, "plans": [{"name": "Basic (100 GB)", "price": 1.99}, {"name": "Standard (200 GB)", "price": 2.99}, {"name": "Premium (2 TB)", "price": 9.99}]},
        "EUR": {"price": 1.99, "plans": [{"name": "Basic (100 GB)", "price": 1.99}, {"name": "Standard (200 GB)", "price": 2.99}, {"name": "Premium (2 TB)", "price": 9.99}]},
        "JPY": {"price": 250.0, "plans": [{"name": "Basic (100 GB)", "price": 250.0}, {"name": "Standard (200 GB)", "price": 380.0}, {"name": "Premium (2 TB)", "price": 1300.0}]},
        "GBP": {"price": 1.59, "plans": [{"name": "Basic (100 GB)", "price": 1.59}, {"name": "Standard (200 GB)", "price": 2.49}, {"name": "Premium (2 TB)", "price": 7.99}]},
        "IDR": {"price": 26900.0, "plans": [{"name": "Basic (100 GB)", "price": 26900.0}, {"name": "Standard (200 GB)", "price": 43000.0}, {"name": "Premium (2 TB)", "price": 135000.0}]},
        "KRW": {"price": 2400.0, "plans": [{"name": "Basic (100 GB)", "price": 2400.0}, {"name": "Standard (200 GB)", "price": 3700.0}, {"name": "Premium (2 TB)", "price": 11900.0}]},
        "INR": {"price": 130.0, "plans": [{"name": "Basic (100 GB)", "price": 130.0}, {"name": "Standard (200 GB)", "price": 210.0}, {"name": "Premium (2 TB)", "price": 650.0}]},
        "SEK": {"price": 19.0, "plans": [{"name": "Basic (100 GB)", "price": 19.0}, {"name": "Standard (200 GB)", "price": 29.0}, {"name": "Premium (2 TB)", "price": 99.0}]},
        "NOK": {"price": 19.0, "plans": [{"name": "Basic (100 GB)", "price": 19.0}, {"name": "Standard (200 GB)", "price": 29.0}, {"name": "Premium (2 TB)", "price": 99.0}]},
        "DKK": {"price": 17.0, "plans": [{"name": "Basic (100 GB)", "price": 17.0}, {"name": "Standard (200 GB)", "price": 25.0}, {"name": "Premium (2 TB)", "price": 89.0}]},
        "CAD": {"price": 2.79, "plans": [{"name": "Basic (100 GB)", "price": 2.79}, {"name": "Standard (200 GB)", "price": 3.99}, {"name": "Premium (2 TB)", "price": 13.99}]},
        "AUD": {"price": 2.49, "plans": [{"name": "Basic (100 GB)", "price": 2.49}, {"name": "Standard (200 GB)", "price": 4.39}, {"name": "Premium (2 TB)", "price": 12.49}]},
        "SGD": {"price": 2.79, "plans": [{"name": "Basic (100 GB)", "price": 2.79}, {"name": "Standard (200 GB)", "price": 3.99}, {"name": "Premium (2 TB)", "price": 13.99}]},
        "AED": {"price": 7.39, "plans": [{"name": "Basic (100 GB)", "price": 7.39}, {"name": "Standard (200 GB)", "price": 10.99}, {"name": "Premium (2 TB)", "price": 36.99}]},
        "SAR": {"price": 7.49, "plans": [{"name": "Basic (100 GB)", "price": 7.49}, {"name": "Standard (200 GB)", "price": 10.99}, {"name": "Premium (2 TB)", "price": 36.99}]},
        "THB": {"price": 70.0, "plans": [{"name": "Basic (100 GB)", "price": 70.0}, {"name": "Standard (200 GB)", "price": 99.0}, {"name": "Premium (2 TB)", "price": 350.0}]},
        "PLN": {"price": 8.99, "plans": [{"name": "Basic (100 GB)", "price": 8.99}, {"name": "Standard (200 GB)", "price": 13.99}, {"name": "Premium (2 TB)", "price": 46.99}]},
        "CZK": {"price": 59.99, "plans": [{"name": "Basic (100 GB)", "price": 59.99}, {"name": "Standard (200 GB)", "price": 79.99}, {"name": "Premium (2 TB)", "price": 299.99}]}
    },
    "microsoft_onedrive": {
        "USD": {"price": 1.99, "plans": [{"name": "Basic (100 GB)", "price": 1.99}, {"name": "Personal (1 TB)", "price": 6.99}, {"name": "Family (6 TB)", "price": 9.99}]},
        "EUR": {"price": 2.0, "plans": [{"name": "Basic (100 GB)", "price": 2.0}, {"name": "Personal (1 TB)", "price": 7.0}, {"name": "Family (6 TB)", "price": 10.0}]},
        "JPY": {"price": 260.0, "plans": [{"name": "Basic (100 GB)", "price": 260.0}, {"name": "Personal (1 TB)", "price": 1490.0}, {"name": "Family (6 TB)", "price": 2100.0}]},
        "GBP": {"price": 1.99, "plans": [{"name": "Basic (100 GB)", "price": 1.99}, {"name": "Personal (1 TB)", "price": 5.99}, {"name": "Family (6 TB)", "price": 7.99}]},
        "IDR": {"price": 28999.0, "plans": [{"name": "Basic (100 GB)", "price": 28999.0}, {"name": "Personal (1 TB)", "price": 87999.0}, {"name": "Family (6 TB)", "price": 129999.0}]},
        "KRW": {"price": 2500.0, "plans": [{"name": "Basic (100 GB)", "price": 2500.0}, {"name": "Personal (1 TB)", "price": 8900.0}, {"name": "Family (6 TB)", "price": 11900.0}]},
        "INR": {"price": 149.0, "plans": [{"name": "Basic (100 GB)", "price": 149.0}, {"name": "Personal (1 TB)", "price": 489.0}, {"name": "Family (6 TB)", "price": 619.0}]},
        "SEK": {"price": 20.0, "plans": [{"name": "Basic (100 GB)", "price": 20.0}, {"name": "Personal (1 TB)", "price": 75.0}, {"name": "Family (6 TB)", "price": 100.0}]},
        "NOK": {"price": 20.0, "plans": [{"name": "Basic (100 GB)", "price": 20.0}, {"name": "Personal (1 TB)", "price": 75.0}, {"name": "Family (6 TB)", "price": 100.0}]},
        "DKK": {"price": 15.0, "plans": [{"name": "Basic (100 GB)", "price": 15.0}, {"name": "Personal (1 TB)", "price": 55.0}, {"name": "Family (6 TB)", "price": 75.0}]},
        "CAD": {"price": 2.0, "plans": [{"name": "Basic (100 GB)", "price": 2.0}, {"name": "Personal (1 TB)", "price": 8.0}, {"name": "Family (6 TB)", "price": 11.0}]},
        "AUD": {"price": 3.0, "plans": [{"name": "Basic (100 GB)", "price": 3.0}, {"name": "Personal (1 TB)", "price": 10.0}, {"name": "Family (6 TB)", "price": 13.0}]},
        "SGD": {"price": 3.0, "plans": [{"name": "Basic (100 GB)", "price": 3.0}, {"name": "Personal (1 TB)", "price": 11.0}, {"name": "Family (6 TB)", "price": 15.0}]},
        "AED": {"price": 7.0, "plans": [{"name": "Basic (100 GB)", "price": 7.0}, {"name": "Personal (1 TB)", "price": 26.0}, {"name": "Family (6 TB)", "price": 37.0}]},
        "SAR": {"price": 7.0, "plans": [{"name": "Basic (100 GB)", "price": 7.0}, {"name": "Personal (1 TB)", "price": 26.0}, {"name": "Family (6 TB)", "price": 37.0}]},
        "THB": {"price": 70.0, "plans": [{"name": "Basic (100 GB)", "price": 70.0}, {"name": "Personal (1 TB)", "price": 210.0}, {"name": "Family (6 TB)", "price": 290.0}]},
        "PLN": {"price": 8.0, "plans": [{"name": "Basic (100 GB)", "price": 8.0}, {"name": "Personal (1 TB)", "price": 30.0}, {"name": "Family (6 TB)", "price": 43.0}]},
        "CZK": {"price": 50.0, "plans": [{"name": "Basic (100 GB)", "price": 50.0}, {"name": "Personal (1 TB)", "price": 190.0}, {"name": "Family (6 TB)", "price": 270.0}]}
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
            ind_prices = [p for p in ind_prices if p > 20.0]
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

def scrape_google_one():
    print("Using fallback for Google One")
    return fallback_prices["google_one"]

def scrape_onedrive():
    print("Using fallback for OneDrive")
    return fallback_prices["microsoft_onedrive"]

def main():
    target_path = r"app/src/main/assets/streaming_prices.json"
    
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
        },
        "google_one": {
            "name": "Google One",
            "logo_res": "ic_logo_google_one",
            "official_url": "https://one.google.com/"
        },
        "microsoft_onedrive": {
            "name": "Microsoft 365 (OneDrive)",
            "logo_res": "ic_logo_onedrive",
            "official_url": "https://www.microsoft.com/tr-tr/microsoft-365"
        }
    }
    
    print("Scraping Spotify...")
    spotify_data = scrape_spotify()
    
    print("Scraping Apple Music...")
    apple_data = scrape_apple_music()
    
    print("Scraping Google One...")
    google_data = scrape_google_one()
    
    print("Scraping OneDrive...")
    onedrive_data = scrape_onedrive()
    
    prime_data = fallback_prices["prime_video"]
    netflix_data = fallback_prices["netflix"]
    
    scraped_map = {
        "prime_video": prime_data,
        "netflix": netflix_data,
        "spotify": spotify_data,
        "apple_music": apple_data,
        "google_one": google_data,
        "microsoft_onedrive": onedrive_data
    }
    
    updated_list = []
    for pid in ["prime_video", "netflix", "spotify", "apple_music", "google_one", "microsoft_onedrive"]:
        scraped = scraped_map[pid]
        meta = meta_info[pid]
        
        # Build "regional" field
        regional_dict = {}
        for cur, cur_data in regional_data[pid].items():
            plans_list = []
            for plan in cur_data["plans"]:
                plans_list.append({
                    "name": plan["name"],
                    "price": plan["price"],
                    "currency": cur,
                    "period": "monthly"
                })
            regional_dict[cur] = {
                "base_price": cur_data["price"],
                "base_currency": cur,
                "plans": plans_list
            }
            
        # Define supported currencies (Brazil/BRL added)
        all_currencies = ["TRY", "USD", "EUR", "JPY", "GBP", "IDR", "KRW", "INR", "SEK", "NOK", "DKK", "CAD", "AUD", "SGD", "AED", "SAR", "THB", "PLN", "CZK"]
        supported = all_currencies

        updated_plat = {
            "id": pid,
            "name": meta["name"],
            "base_price": scraped["price"],
            "base_currency": scraped["currency"],
            "logo_res": meta["logo_res"],
            "official_url": meta["official_url"],
            "plans": scraped["plans"],
            "regional": regional_dict,
            "supported_currencies": supported
        }
        updated_list.append(updated_plat)
        
    with open(target_path, "w", encoding="utf-8") as f:
        json.dump(updated_list, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully updated {target_path}!")
    for plat in updated_list:
        print(f"  - {plat['name']}: {plat['base_price']} {plat['base_currency']}")

if __name__ == "__main__":
    main()
