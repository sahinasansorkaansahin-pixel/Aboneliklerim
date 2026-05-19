# 🌍 Dijital Platform Fiyatları & Döviz Kuru Sistemi

Bu repo, **Aboneliklerim** uygulamasının kalbini oluşturan iki bağımsız modülü içerir:

---

## 📦 İçerik

### 1. `streaming_prices.json` — Dijital Platform Fiyat Veritabanı
20 ülke ve 4 büyük platform için **resmi** ve **güncel** fiyat verileri:

| Platform | Desteklenen Ülkeler |
|---|---|
| 🎬 Netflix | TRY, USD, EUR, GBP, JPY, CHF, KRW, SEK, NOK, DKK, CAD, AUD, SGD, AED, SAR, THB, PLN, CZK (CNY hariç) |
| 🎵 Spotify | TRY, USD, EUR, GBP, JPY, CHF, KRW, SEK, NOK, DKK, CAD, AUD, SGD, AED, SAR, THB, PLN, CZK (CNY hariç) |
| 🎵 Apple Music | Tüm 20 ülke |
| 📺 Amazon Prime Video | Tüm 20 ülke |

Her platform için `supported_currencies` alanı ile hangi ülkelerde aktif olduğu tanımlıdır. Desteklenmeyen ülkelerde platform listede görünmez.

---

### 2. `update_streaming_prices.py` — Otomatik Fiyat Güncelleyici

Her platformun resmi web sitesinden fiyatları otomatik olarak çeken ve `streaming_prices.json` dosyasını güncelleyen Python betiği.

#### Kullanım:
```bash
pip install requests beautifulsoup4
python update_streaming_prices.py
```

#### Özellikler:
- Her platformun **resmi web sitesini** scrape eder
- Türkiye baz fiyatı olarak günceller
- 20 ülkenin **regional** fiyat tablosunu doldurur
- `supported_currencies` listesini dinamik olarak yönetir
- Çıktıyı doğrudan `app/src/main/assets/streaming_prices.json` olarak yazar

---

## 🌐 Desteklenen Para Birimleri / Ülkeler

| Para Birimi | Ülke |
|---|---|
| TRY | 🇹🇷 Türkiye |
| USD | 🇺🇸 Amerika |
| EUR | 🇪🇺 Avrupa |
| GBP | 🇬🇧 İngiltere |
| JPY | 🇯🇵 Japonya |
| CHF | 🇨🇭 İsviçre |
| KRW | 🇰🇷 Güney Kore |
| CNY | 🇨🇳 Çin |
| SEK | 🇸🇪 İsveç |
| NOK | 🇳🇴 Norveç |
| DKK | 🇩🇰 Danimarka |
| CAD | 🇨🇦 Kanada |
| AUD | 🇦🇺 Avustralya |
| SGD | 🇸🇬 Singapur |
| AED | 🇦🇪 BAE |
| SAR | 🇸🇦 Suudi Arabistan |
| THB | 🇹🇭 Tayland |
| PLN | 🇵🇱 Polonya |
| CZK | 🇨🇿 Çekya |

---

## 🔄 Otomatik Güncelleme

Uygulama başlatıldığında her 12 saatte bir arka planda fiyatları kontrol eder.  
Manuel güncelleme için betiği çalıştırabilirsiniz:

```bash
python update_streaming_prices.py
```

---

## 📱 Aboneliklerim Uygulaması

Bu sistem **Aboneliklerim** Android uygulamasının bir parçasıdır.  
Uygulama: [sahinasansorkaansahin-pixel/Aboneliklerim](https://github.com/sahinasansorkaansahin-pixel/Aboneliklerim)
