# 📱 Aboneliklerim (Subscription Manager)

Aboneliklerim, kullanıcıların dijital ve fiziksel aboneliklerini tek bir merkezden, modern bir arayüzle takip etmelerini sağlayan yerel Android uygulamasıdır. 

Uygulama, Kotlin ve modern Android geliştirme standartları (büyük oranda) gözetilerek geliştirilmiştir. Bu depo, uygulamanın kaynak kodlarını içerir ve geliştirici topluluğunun projeye katkıda bulunmasını kolaylaştırmak amacıyla belgelendirilmiştir.

## ✨ Temel Özellikler

- **Global Para Birimi Desteği:** En zengin ekonomilerden gelişmekte olan ülkelere doğru sıralanmış 60 farklı para birimi desteği.
- **Akıllı Karanlık Mod (Smart Dark Mode):** Cihaz saatine göre (19:00 - 07:00 arası) otomatik devreye giren veya sistem varsayılanına bırakılabilen dinamik tema yapısı.
- **Yerel Bildirim Sistemi (Alarms):** `AlarmManager` ve `BroadcastReceiver` kullanılarak, ödeme günlerinden önce (1, 3, 7 gün vb.) kullanıcıyı uyaran yerel anlık bildirim altyapısı.
- **Modern UI & Onboarding:** `ViewPager2` ile tasarlanmış 8 kartlık dikey kaydırmalı tanıtım ekranı ve `BottomSheetDialog` kullanan filtre/sıralama menüleri.
- **Kategorizasyon ve Renklendirme:** Abonelikleri etiketlere göre ayırma ve her kartı özel HEX renk koduyla kişiselleştirme.

## 🛠️ Teknik Altyapı ve Stack

- **Dil:** Kotlin
- **Arayüz Tasarımı:** Vanilla XML (Material Design Bileşenleri, CardView, ViewPager2)
- **Veri Saklama:** Şu an için `SharedPreferences` ve `Gson` kullanılarak veriler yerel olarak (JSON formatında) cihazda tutulmaktadır. (Veritabanı kullanılmamıştır, mimari belgesine bakınız).
- **Arka Plan İşlemleri:** `AlarmManager` ve `NotificationManager`.

## 🚀 Kurulum ve Çalıştırma

1. Projeyi Android Studio'ya aktarın (File -> Open -> `Aboneliklerim` klasörünü seçin).
2. Gradle senkronizasyonunun bitmesini bekleyin (Gerekiyorsa `gradle-wrapper.properties` içerisindeki gradle versiyonunu sisteminize göre güncelleyin).
3. Minimum SDK gereksinimi: API 24 (Android 7.0) veya üzeri. Hedef SDK: API 34.
4. Cihazınızı bağlayın veya emülatörü başlatıp **Run (Shift+F10)** butonuna tıklayın.

## 🤝 Geliştiriciler İçin Notlar (Nasıl Yardımcı Olabilirsiniz?)

Proje şu an oldukça kararlı çalışıyor ancak mimari olarak büyümeye ve refactor edilmeye ihtiyacı var. Aşağıdaki konularda katkı (PR) beklenmektedir:

1. **Room Database Göçü:** Şu anda veriler `Gson` ile String formatında `SharedPreferences`'ta tutuluyor. Bu yapı, veri büyüdükçe performans kaybı yaratabilir. Uygulamanın `Room` veya `SQLite` tabanlı bir yapıya geçirilmesi çok iyi olur.
2. **Deprecated API'lerin Güncellenmesi:** Mevcut kod base'de `startActivityForResult` gibi eski yapılandırmalar bulunuyor. Bunların `ActivityResultLauncher` (Activity Result API) kullanılarak modernize edilmesi gerekiyor.
3. **Bulut Senkronizasyonu (Cloud Sync):** "Yedek & Aktarım" butonu şu an sadece arayüz olarak var. Firebase Realtime Database / Firestore veya Google Drive API entegrasyonu eklenmesi planlanmaktadır.
4. **MVVM ve State Management:** Mevcut durumda UI mantığı ve iş mantığı `Activity` sınıfları içinde biraz iç içe girmiş durumdadır. Projenin MVVM (Model-View-ViewModel) mimarisine taşınması mükemmel bir geliştirme olacaktır.

Daha detaylı teknik bilgi için lütfen **[ARCHITECTURE.md](ARCHITECTURE.md)** dosyasını okuyun. Projeye katkı sağlamak isterseniz **[CONTRIBUTING.md](CONTRIBUTING.md)** kılavuzunu inceleyebilirsiniz.

---
**Geliştiren:** Falcon APP Studios & Topluluk
