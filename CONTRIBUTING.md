# 🤝 Katkıda Bulunma Rehberi (Contributing)

Öncelikle projemize zaman ayırdığınız ve **Aboneliklerim** uygulamasını daha iyi bir yerel Android uygulaması yapma vizyonumuza destek olduğunuz için teşekkür ederiz!

Bu rehber, projemize sorunsuz bir şekilde katkıda bulunmanız (Pull Request oluşturmanız, hata bildirimi yapmanız) için hazırlanmıştır. Lütfen geliştirme aşamasına geçmeden önce aşağıdaki adımları okuyunuz.

## 🐛 Hata Bildirimi (Bug Reports)
Uygulamada bir çökme (crash) veya mantık hatası bulduysanız, GitHub/GitLab Issues bölümünden bize bildirebilirsiniz. Hata bildirimi açarken lütfen şu bilgileri ekleyin:
- Kullandığınız Android versiyonu ve cihaz modeli.
- Hatayı nasıl tekrar edebileceğimiz (Steps to reproduce).
- Mümkünse Logcat çıktıları veya ekran görüntüleri.

## 💻 Koda Katkı Sağlama (Pull Requests)

### 1. Hazırlık ve Dal (Branch) Oluşturma
Proje deposunu kendi hesabınıza forklayın ve bilgisayarınıza klonlayın. Geliştirme yaparken lütfen her bir özellik veya hata düzeltmesi için ayrı bir dal (branch) açın:
`git checkout -b feature/yeni-ozellik-adi`
veya hata düzeltiyorsanız:
`git checkout -b bugfix/hata-adi`

### 2. Geliştirme Süreci ve Standartlar
- Kodlama yaparken **Kotlin idiomatic** kurallarına (Kotlin'in doğasına uygun, modern yazım) uymaya özen gösterin.
- Eklediğiniz metodlara ve karmaşık algoritmalara (özellikle tarih / zaman hesaplamaları yapılan kısımlara) İngilizce veya Türkçe açıklayıcı (docstring) yorumlar ekleyin.
- Kod bloğu (UI katmanı ile veri katmanı) değişikliklerinizin, mevcut uygulamadaki `SharedPreferences` yapısını bozmadığından emin olun (Eğer veritabanı taşıması (Room) PR'ı açmıyorsanız).
- Lütfen uygulamanın **Akıllı Karanlık Mod (ThemeHelper)** sistemine uygun olarak UI değişiklikleri yapın. Hardcoded renk kullanmak yerine `colors.xml` içerisindeki mevcut renkleri (`@color/text_primary`, `@color/surface_dark` vb.) kullanmaya özen gösterin.

### 3. Test Etme
Değişikliklerinizi Push etmeden önce uygulamanın başarılı bir şekilde derlendiğinden emin olun:
`./gradlew clean build`

Ek olarak, uygulamanın emülatörde veya fiziksel bir cihazda çalışıp çalışmadığını, çökme yaratıp yaratmadığını bizzat kontrol edin.

### 4. Pull Request Gönderme
Değişikliklerinizi kendi deponuza pushladıktan sonra orijinal depoya Pull Request açın. PR açıklamasında:
- Ne değiştirdiğinizi (What changed?)
- Neden bu değişikliğe ihtiyaç duyulduğunu (Why?)
kısa ve net bir şekilde açıklayın. Varsa, çözdüğünüz Issue numarasını ekleyin (Örn: `Fixes #12`).

Tekrar teşekkürler ve keyifli kodlamalar! 🚀
