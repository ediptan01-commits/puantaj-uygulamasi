
(function () {
  "use strict";
  const current = location.pathname.split("/").pop() || "index.html";
  const mode = current === "bireysel.html" ? "individual" :
               current === "kurumsal.html" ? "corporate" : "home";
  const key = "puantaj_onboarding_v1_" + mode;
  const instructions = {
    individual: [
      {selector:'.managementButton[onclick*="openSiteManager"]',title:"1. Şantiyeni oluştur",desc:"Şantiyeler alanından çalıştığın şantiyeyi ekle. Önce buradan başlamak, çalışanları doğru yerde takip etmeni sağlar."},
      {selector:'.managementButton[onclick*="openEmployeeManager"]',title:"2. Çalışan ekle",desc:"Çalışanlar bölümünden kişi ekle, şantiyesini seç ve günlük yevmiye tutarını tanımla."},
      {selector:'.weekdays',title:"3. Günlük puantajı işle",desc:"Çalışan ekledikten sonra takvimde bir güne dokun. Çalıştı, Gelmedi, İzinli, Raporlu veya Yarım seçebilirsin."},
      {selector:'.summaryGrid',title:"4. Aylık özeti incele",desc:"Çalışılan günler, toplam saat, yevmiye ve gelinmeyen günler burada hesaplanır."},
      {selector:'.paymentGrid',title:"5. Ödemeleri takip et",desc:"Günlük yevmiye, alınan ödeme ve kalan tutarı bu bölümden kontrol et."},
      {selector:'.managementButton[onclick*="downloadBackup"]',title:"6. Verilerini yedekle",desc:"Kayıtlarını cihazından dışa aktararak yedekle. Rehbere her zaman sağ alttaki ? simgesinden dönebilirsin."}
    ],
    corporate: [
      {selector:'button[onclick="openModal(\'site\')"]',title:"1. Şantiye ekle",desc:"Önce bir şantiye oluştur. Şantiye ve ekiplerini buradan takip edebilirsin."},
      {selector:'button[onclick="openModal(\'team\')"]',title:"2. Ekip oluştur",desc:"Yeni ekibi oluşturup ilgili şantiyeye bağla."},
      {selector:'.quickWorker',title:"3. Çalışan ekle",desc:"Çalışan bilgilerini ve ekip ilişkisini gir. Puantaj kaydı için önce çalışan gerekir."},
      {selector:'.monthlyOverview',title:"4. Günlük puantajı yönet",desc:"Ekip veya çalışan kartına girip takvimden günlük çalışma durumunu işle; sonuçlar bu aylık özette görünür."},
      {selector:'.quickPayment',title:"5. Ücret ve ödeme takibi",desc:"Çalışan ödemelerini kaydet; yapılan ve kalan ödemeleri takip et."},
      {selector:'.quickReports',title:"6. Raporları görüntüle",desc:"Günlük, haftalık ve aylık raporlarını aç. Bazı dışa aktarma özellikleri PRO abonelik gerektirebilir."}
    ]
  };
  let currentIndex = 0, container = null, focus = null, panel = null, active = false;
  function make(tag, className, text) {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (text) el.textContent = text;
    return el;
  }
  function cleanup() {
    active = false;
    if (container) container.remove();
    if (focus) focus.remove();
    if (panel) panel.remove();
    container = focus = panel = null;
    window.removeEventListener("resize", position);
    window.removeEventListener("scroll", position, true);
  }
  function done() {
    localStorage.setItem(key, "1");
    cleanup();
  }
  function position() {
    if (!active || !focus || !panel) return;
    const step = instructions[mode][currentIndex];
    const el = document.querySelector(step.selector);
    if (!el) return;
    const r = el.getBoundingClientRect();
    const top = Math.max(6, r.top - 5);
    const bottom = Math.min(innerHeight - 6, r.bottom + 5);
    focus.style.top = top + "px";
    focus.style.left = Math.max(6, r.left - 5) + "px";
    focus.style.width = Math.max(20, Math.min(innerWidth - 12, r.width + 10)) + "px";
    focus.style.height = Math.max(12, bottom - top) + "px";
    const panelHeight = panel.offsetHeight || 190;
    const spaceBelow = innerHeight - r.bottom;
    const spaceAbove = r.top;
    if (spaceBelow > panelHeight + 28) {
      panel.style.top = Math.min(innerHeight - panelHeight - 10, r.bottom + 16) + "px";
    } else if (spaceAbove > panelHeight + 24) {
      panel.style.top = Math.max(8, r.top - panelHeight - 16) + "px";
    } else {
      panel.style.top = Math.max(8, innerHeight - panelHeight - 12) + "px";
    }
  }
  function show(index) {
    const steps = instructions[mode];
    if (!steps || index >= steps.length) return done();
    currentIndex = index;
    const step = steps[index];
    const target = document.querySelector(step.selector);
    if (!target) return show(index + 1);
    panel.replaceChildren();
    panel.appendChild(make("div", "pt-step-count", "ADIM " + (index + 1) + " / " + steps.length));
    panel.appendChild(make("h2", "", step.title));
    panel.appendChild(make("p", "", step.desc));
    const actions = make("div", "pt-actions");
    const skip = make("button", "pt-skip", "Atla");
    skip.type = "button"; skip.addEventListener("click", done);
    const next = make("button", "pt-next", index === steps.length - 1 ? "Bitir" : "İleri");
    next.type = "button"; next.addEventListener("click", () => show(index + 1));
    actions.append(skip, next);
    panel.appendChild(actions);
    target.scrollIntoView({block:"center", behavior:"instant"});
    requestAnimationFrame(() => requestAnimationFrame(position));
  }
  function start() {
    cleanup();
    if (!instructions[mode]) return;
    active = true;
    container = make("div", "pt-overlay");
    focus = make("div", "pt-focus");
    panel = make("section", "pt-panel");
    panel.setAttribute("role","dialog");
    panel.setAttribute("aria-modal","true");
    panel.setAttribute("aria-label","Puantaj kullanım rehberi");
    document.body.append(container, focus, panel);
    window.addEventListener("resize", position);
    window.addEventListener("scroll", position, true);
    show(0);
  }
  function welcome() {
    if (localStorage.getItem(key) === "1") return;
    const mask = make("div", "pt-overlay");
    const box = make("section", "pt-welcome");
    box.setAttribute("role","dialog"); box.setAttribute("aria-modal","true");
    box.appendChild(make("div", "pt-emoji", "👷📋"));
    box.appendChild(make("h2", "", "Puantaj'a Hoş Geldiniz!"));
    box.appendChild(make("p", "", "Bireysel veya Kurumsal modu seçip şantiye, çalışan, puantaj ve ödeme takibini adım adım öğrenebilirsiniz."));
    const buttons=make("div","pt-actions");
    function close(){localStorage.setItem(key,"1");mask.remove();box.remove();}
    const skip=make("button","pt-skip","Atla");skip.onclick=close;
    const begin=make("button","pt-next","Başlayalım");begin.onclick=close;
    buttons.append(skip,begin);box.appendChild(buttons);
    document.body.append(mask,box);
  }
  function init() {
    if (mode === "home") { welcome(); return; }
    const button=make("button","pt-help-button","? Rehber");
    button.type="button";
    button.setAttribute("aria-label","Kullanım rehberini yeniden aç");
    button.addEventListener("click", start);
    document.body.appendChild(button);
    if (localStorage.getItem(key) !== "1") start();
  }
  if (document.readyState === "complete") init();
  else window.addEventListener("load", init, {once:true});
})();
