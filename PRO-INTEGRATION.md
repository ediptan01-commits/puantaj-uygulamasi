# PRO entegrasyonu — 3 Kasım 2026

## Son kapsam

Puantaj ücretsiz ve sınırsızdır. PRO yalnızca reklamsız kullanım için aylık/yıllık aboneliktir. Kullanıcının açık talebiyle uygulamadaki PRO satış ayarı etkindir. Reklam SDK altyapısı hazır, gösterim kapalıdır. Kullanıcıya şu anda reklam gösterilmediği ve ücretsiz kullanımın sınırsız olduğu açıklanır. Önceden konuşulan üç yeni raporlama özelliği bu pakette yoktur.

## Kaynaklar

(8) taban sürümdür ve GitHub main 14aad053152b3af1d7ac587c040e5ee2d73ebf28 ile birebir eşleşti. (5) eski INAPP ürün kurumsal_puantaj_pro içerir. (7) SUBS ürün puantaj_pro ve aylik/yillik temel planlarını içerir. (4)'te reklam kimlikleri bulunmadı.

## Doğrulama

Play Console'un Puantaj > Para kazanma kurulumu > Lisanslama sayfasındaki mevcut Base64 RSA ortak anahtarı alındı. Bu, uygulamanın özel imzalama anahtarı değildir; uygulama ikili dosyasına eklenmesi için Google'ın sağladığı ortak anahtardır. Özel anahtar okunmadı/oluşturulmadı.

billing-config.json verificationMode=play_signature, salesEnabled=true. Uygulama her açılışta/öne gelişte INAPP ve SUBS güncel sahiplik sorgularını yapar. Yalnızca beklenmiş paket/ürün/token, PURCHASED ve duraklatılmamış işlem için Google imzası doğrulanır. İmzasız, sahte, değiştirilmiş, farklı anahtarlı kayıtlar açılmaz. Doğrulama sonrası acknowledgement yapılır. Yerel depolama veya WebView callback'i native PRO yetkisini açamaz. Başarısız sorguda sınırsız ücretsiz özellikler çalışır; PRO doğrulanmış kabul edilmez.

Bu model cihazdaki Play sahiplik sorgusunu kullanır. Sunucu üzerinden gerçek zamanlı iade/süre sonu kontrolü ve RTDN daha güçlü koruma sağlar; Google bunları önerir. verification-service ayrı sunucu için hazırdır, fakat canlı kurulum yapılmadı. Sunucuya geçilirse verificationMode=server ve HTTPS verificationUrl tanımlanır; sunucu hatasında imza moduna sessiz geçiş yoktur.

## Kullanıcı arayüzü

Bireysel/kurumsal PRO ekranları görünür. Uygun aylık/yıllık otomatik yenilenen temel plan, teklif belirteci ve fiyat Play'den alınır. Ürün/fiyat alınamıyorsa ödeme düğmesi kullanılamaz. Sahiplik sorgusu bitmeden ödeme başlatılmaz; mevcut PRO sahibi tekrar satın almaya gönderilmez. Satın alma iptali ve bekleme açıklanır. Geri yükleme ve Play abonelik yönetimi vardır.

## Play Console'da gözlenen durum

Üretim: 10 (1.0.9), 2 Kasım 2026. Hazırlanan yeni paket: 11 (1.0.10). Abonelik ekranı henüz abonelik göstermiyor ve yeni APK istiyor. Kullanıcı daha önce abonelikleri kurduğunu belirtti; Billing içeren paket yüklendikten sonra ürün ve planların gerçekten etkin/görünür olduğu teyit edilmelidir. Aylık/yıllık fiyatlar tahmin edilmedi.

## İmza

Paket com.ediptan.kurumsalpuantaj. Mevcut SIGNING_* ayarları kullanılır. Önceki v1.0.9 AAB sertifikası okundu; SHA-256 EB:B7:EB:62:AB:C9:DA:75:5F:37:10:2A:C5:5C:CF:ED:D9:29:98:55:22:3E:CB:63:FA:46:DD:26:99:16:01:82. Derleme hem anahtarı hem yeni AAB'yi bu sertifikaya karşı kontrol eder. Bu yükleme sertifikasıdır; Play dağıtım sertifikası farklı olabilir.

## Test ve yayın

TEST-RESULTS.txt yalnızca çalıştırılmış kontrolleri belirtir. Gerçek Google Play test hesabında başarılı/iptal/bekleyen ödeme, yeniden başlatma, geri yükleme, eski satın alma ve abonelik yaşam döngüsü henüz denenmedi. Yayın onayı verilmez; kullanıcı açıkça onaylamadan Google Play üretimine yayın yapılmaz. Reklamlar açılmadan gizlilik/izin ve Play Data safety beyanları güncellenmelidir. Reklam olmayan sürümde reklamsız aboneliğin mağaza açıklaması ve güncel Play abonelik politikası ayrıca gözden geçirilmelidir.

https://developer.android.com/reference/com/android/billingclient/api/Purchase
https://developer.android.com/google/play/billing/security
