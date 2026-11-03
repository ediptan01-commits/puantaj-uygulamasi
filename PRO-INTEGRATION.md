# PRO düzenlemesi — 3 Kasım 2026

## Kullanıcının son belirlediği kapsam

Tüm puantaj özellikleri ücretsiz ve sınırsızdır. PRO yalnızca reklamları kaldırır; aylık ve yıllık abonelik olur. Reklamlar daha sonra eklenecektir. Bu çalışma PRO düzenlemesine odaklanır. Daha önce onaylanan üç raporlama ilavesi bu pakete eklenmedi; sonraki ücretsiz geliştirmeler olarak bekliyor.

## Üç arşivin karşılaştırması

- (5): Billing 8.2.1; tek seferlik INAPP ürün kurumsal_puantaj_pro. Sunucu doğrulaması yok.
- (7): SUBS ürün puantaj_pro; aylik/yillik temel plan seçimi ve offer token bağlantısı var. Sunucu doğrulaması yok. Mevcut Play Console durumu kaynak kodundan teyit edilemez.
- (8): Billing kaldırılmış, PRO ekranları gizli, tüm özellikler ücretsiz. Bu sürüm taban alındı.
- GitHub main 14aad053152b3af1d7ac587c040e5ee2d73ebf28 dosyaları (8) ZIP'i ile birebir eşleşti. 29 Ekim Android derlemesi başarılı.

## Yapılan düzenlemeler

Bireysel ve kurumsal bölümlerde PRO ekranları erişilebilir. Sınırsız kullanım korunur. Aylık/yıllık fiyat Google Play'den alınır, yalnızca doğru dönemli otomatik yenilenen temel plan seçilir; başka kampanyaya rastgele geçilmez. Satın alma iptali, bekleme, bağlantı/doğrulama hataları açıklanır. Geri yükleme hem eski tek seferlik hem abonelik satın almalarını sorgular. Mevcut PRO sahibi yeniden satın almaya yönlendirilmez. Abonelik yönetimi Google Play'de açılır.

Reklam SDK/UMP, yerleşim ve PRO durumuna bağlı native kontrol eklendi; ads-config.json enabled=false. Hiçbir reklam isteği gönderilmez. salesEnabled=false; olmayan reklamsızlık avantajı için ödeme alınmaz. Yerel depolama erişim kaynağı değildir. Erişim önbelleği yoktur; doğrulama hatasında PRO durumu belirsiz/kapalı kalır, ücretsiz özellikler etkilenmez.

## Güvenli doğrulama

verification-service klasörü Google Play Developer API üzerinden token doğrulayan Node sunucusudur. Google Application Default Credentials kullanır; özel anahtar uygulamaya konmaz. Tek seferlik ve abonelik için ayrı uçlar kullanılır. İade/bekleme/süre sonu kapatır; ödenmiş süre içindeki iptal ve grace period erişimi korur. Paket/ürün izin listesi sunucuda sabittir. Testler gerçek Google'a bağlanmaz.

Sunucu henüz kurulmadı. verificationUrl boş olduğu için bu pakette gerçek geri yükleme tamamlanamaz. Servis hesabı yetkisi, HTTPS kurulum, merkezi oran sınırı, sunucuda güvenilir acknowledgement/retry ve RTDN gerçek satış açılışından önce tamamlanmalıdır. Ayrıntı verification-service/README.md içindedir.

## İmza ve derleme

Paket: com.ediptan.kurumsalpuantaj. Yeni anahtar oluşturulmadı. Mevcut SIGNING_* gizli ayarları korunur. Önceki v1.0.9 AAB indirilip sertifikası okundu. SHA-256: EB:B7:EB:62:AB:C9:DA:75:5F:37:10:2A:C5:5C:CF:ED:D9:29:98:55:22:3E:CB:63:FA:46:DD:26:99:16:01:82. Yeni derleme bu sertifikayla eşleşmezse durur. Bu yükleme sertifikasıdır; Play App Signing dağıtım sertifikası farklı olabilir.

Önerilen sürüm kodu 11, ad 1.0.10. Play Console'daki en yüksek kod ayrıca kontrol edilmelidir. Hazırlık dalına gönderildiğinde yalnızca APK/AAB dosyaları derlenir. GitHub sürümü oluşturma ve Play'e yayın adımı yoktur. Ana dal birleştirmesi yapılmaz.

## Tamamlanmadan önce

1. Doğrulama hizmetini kurun ve verificationUrl girin.
2. Play Console'da puantaj_pro + aylik/yillik planlarını doğrulayın.
3. Play test kanalında eski satın alma, aylık/yıllık satın alma, iptal, bekleyen ödeme, yeniden başlatma, geri yükleme, iade, süre sonu ve ağ kesintisini deneyin.
4. Reklamlar ileride açılınca gerçek AdMob kimlikleri, UMP mesajları ve gizlilik metni/Data safety beyanı güncellensin. Native kontrol doğrulanmış PRO veya belirsiz durumdayken reklam istemez. Reklam ve izin durumuna ilişkin cihaz testleri yapılmalı.
5. Reklamlar ve gerçek lisans testleri tamamlanmadan salesEnabled true yapılmasın. Kullanıcının açık onayı olmadan üretime yayın yapılmasın.

Referans: https://developer.android.com/google/play/billing/integrate
