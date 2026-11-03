/* PRO only removes ads; every attendance feature remains free and unlimited. */
(function(){
    let active=false, catalog=[], message="PRO reklamsız kullanım içindir. Reklamlar henüz eklenmediği için yeni abonelik satışı kapalıdır.";
    const individual=!!document.getElementById("individualProModal");
    const prefix=individual?"individualPro":"pro";
    const el=suffix=>document.getElementById(prefix+suffix);
    const previous=window.onProEntitlementChanged;
    function render(){
        if(el("Status"))el("Status").textContent=active?"PRO hakkınız doğrulandı. Tüm özellikler ücretsiz ve sınırsızdır.":message;
        const select=el("Plan"),button=el("Buy");
        if(!select||!button)return;
        for(const option of Array.from(select.options)){
            const plan=catalog.find(x=>x.plan===option.value);
            const period=option.value==="aylik"?"Aylık":"Yıllık";
            option.textContent=period+(plan?" — "+plan.price+" / "+(option.value==="aylik"?"ay":"yıl"):" — Fiyat henüz alınamadı");
        }
        const plan=catalog.find(x=>x.plan===select.value);
        button.disabled=active||!plan||plan.purchasable!==true;
        button.textContent=active?"PRO etkin":plan&&plan.purchasable?"Abone Ol":"Satış henüz açılmadı";
        button.style.display="";
    }
    window.onProEntitlementChanged=function(value){active=value===true;if(previous)previous(active);render();};
    window.onProCatalog=function(plans){catalog=Array.isArray(plans)?plans:[];render();};
    window.onProBillingMessage=function(text){message=String(text);render();};
    window.onProBillingError=function(){message="Satın alma tamamlanamadı. Lütfen yeniden deneyin.";render();};
    function open(){
        el("Modal").classList.add("show");render();
        if(window.PlayBilling){window.PlayBilling.isPro();if(window.PlayBilling.getPlans)window.PlayBilling.getPlans();}
        else {message="Satın alma ve geri yükleme Google Play Android sürümünde kullanılabilir. Tüm özellikler ücretsizdir.";render();}
    }
    window.openPro=open;window.openIndividualPro=open;
    window.manageProSubscription=function(){
        if(window.PlayBilling&&window.PlayBilling.manageSubscription)window.PlayBilling.manageSubscription();
        else {message="Aboneliklerinizi Google Play > Ödemeler ve abonelikler bölümünden yönetebilirsiniz.";render();}
    };
    if(el("Plan"))el("Plan").addEventListener("change",render);
    window.onAdPrivacyRequired=function(required){
        for(const button of document.querySelectorAll('[data-ad-privacy]'))button.hidden=required!==true;
    };
    window.showAdPrivacyOptions=function(){
        if(window.PuantajNative&&window.PuantajNative.showAdPrivacy)window.PuantajNative.showAdPrivacy();
    };
    render();
})();
