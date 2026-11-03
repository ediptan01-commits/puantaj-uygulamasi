# Satın alma doğrulama hizmeti

Bu klasör güvenli sunucu içindir; Android uygulamasına paketlenmez. Canlı ortama kurulmadı. Google servis hesabı anahtarını uygulamaya veya depoya koymayın.

Node.js 22+ ortamında `npm install` ve `npm start` ile çalışır. Üretimde kilit dosyasını oluşturup sabitleyin, HTTPS arkasına koyun. Google Application Default Credentials ile Android Publisher kapsamını kullanır. Servis hesabına Google Play Console'da bu uygulama için satın alma/abonelik durumlarını okuma izni verilmeli ve Android Publisher API etkin olmalıdır. Sunucunun dış erişiminde istek boyutu, zaman aşımı ve oran sınırı uygulanmalı; çoklu örnekler için merkezi ağ geçidi sınırı gereklidir.

`POST /verify`: `{packageName, purchaseToken, products:[productId]}`. İzin verilen paket ve ürünler kodda sabittir. Sunucu Google Play'e sorar; istemcinin `active` iddiasını kabul etmez. Tek seferlik satın alma ile abonelik farklı Google API uçlarından okunur. Yenilemesi iptal edilmiş abonelik ödenmiş sürenin sonuna kadar korunur. Bekleyen, sona ermiş, beklemeye alınmış ve duraklatılmış abonelikler erişim açmaz. Grace period erişimi korur.

Yanıt `{active,packageName,productId,purchaseToken}`. Tokenlar günlüklere yazılmaz, yanıtlar önbelleğe alınmaz. Hatalı istek 400, Google/ağ hatası 503 döndürür. Native uygulama doğrulamadan sonra acknowledgement yapar. Güvenilir sunucu acknowledgement/retry ve RTDN, gerçek reklam/satış açılışından önce tamamlanmalıdır. Hesap sistemi yoktur; ileride hesap eklenirse token sahipliği hesaba bağlanmalıdır.

Testler Google'a bağlanmadan `node --test test/*.test.mjs` ile çalışır. Bunlar gerçek Play lisans testinin yerini tutmaz.

Kurulumdan sonra Android `billing-config.json` içindeki verificationUrl alanına `https://SUNUCUNUZ/verify` girilir. Reklamlar henüz eklenmediği için salesEnabled false kalmalıdır.
