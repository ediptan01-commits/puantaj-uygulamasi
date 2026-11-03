# 1.0.10 PRO hazırlığı

Uygulama ücretsiz ve sınırsız kalır. PRO yalnızca reklamları kaldırmak için aylık/yıllık abonelik olarak hazırlanmıştır. Reklam altyapısı kapalı olarak eklendi; reklam gösterimi ve yeni abonelik satışı kapalıdır.

(7) arşivinden doğrulanan kimlikler: puantaj_pro, aylik, yillik. Eski tek seferlik kurumsal_puantaj_pro satın almaları da geri yükleme kapsamındadır. Fiyatlar Play ProductDetails verisinden alınır. Bekleyen ödeme PRO açmaz; doğrulanmış satın alma onaylanır. Yerel depolama PRO açamaz.

Doğrulama hizmetinin kaynak ve testleri eklendi, fakat canlı sunucuya kurulmadı. billing-config.json verificationUrl boş olduğundan gerçek geri yükleme henüz çalışmaz.

Paket com.ediptan.kurumsalpuantaj, önerilen versionCode 11 / 1.0.10. Yeni anahtar üretilmez. Mevcut v1.0.9 AAB sertifikasıyla eşleşme derlemede zorunludur. Play üretimine yayın adımı yoktur.
