# Reklam altyapısı — kapalı

ads-config.json enabled=false. Google Mobile Ads 25.5.0 ve UMP 4.0.0 bağımlılıkları, native banner alanı, izin akışı ve PRO bağlantısı hazırdır. Bu sürüm reklam yüklemez. Demo AdMob kimlikleri yapılandırmada tutulur; kullanıcıya reklam gösterilmez.

PRO durumu belirsiz veya etkinse native katman reklam isteğini engeller. WebView/yerel depolama reklam kaldırma yetkisi veremez. Ücretsiz olduğu doğrulanmış kullanıcıda ancak reklamlar etkinse ve UMP canRequestAds true ise SDK başlatılır ve banner yüklenir. Banner uygulama içeriğinin üzerine bindirilmez; PRO etkinleşince yok edilir.

İleride: gerçek AdMob appId/bannerUnitId girin, UMP mesajlarını hesapta yapılandırın, Play imza doğrulamasını ve reklam/PRO cihaz testlerini yapın (sunuculu doğrulama ayrıca önerilir). Test için testMode=true ve Google test kimlikleri kullanılır. Gerçek gösterimde testMode=false ve gerçek kimlikler gerekir; örnek kimliklerle gerçek mod engellenir. App ID manifestte aynı JSON'dan alınır. enabled ancak bundan sonra true yapılır.

Gizlilik politikası ve Google Play Data safety/reklam beyanını gerçek reklam açılışından önce güncelleyin. Geçiş reklamı veya zorunlu reklam eklenmedi. Ücretsiz kullanım hiçbir reklam etkileşimine bağlı değildir.

https://developers.google.com/admob/android/quick-start
https://developers.google.com/admob/android/privacy
