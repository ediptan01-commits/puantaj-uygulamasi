/* VERİ */

let data = {

    company:"",

    sites:[],

    teams:[],

    workers:[],

    payments:[]

};

let modalType="";


/* YÜKLE */

    function openReports(){
    document.getElementById("mainScreen").style.display = "none";
    document.getElementById("reportsScreen").style.display = "block";
}

function closeReports(){
    document.getElementById("reportsScreen").style.display = "none";
    document.getElementById("mainScreen").style.display = "block";
}

function showReport(type){

    const content = document.getElementById("reportContent");
    const today = new Date();

    let startDate = new Date(today);
    let endDate = new Date(today);

    if(type === "weekly"){
        const day = today.getDay();
        const diff = day === 0 ? -6 : 1 - day;

        startDate.setDate(today.getDate() + diff);
        endDate = new Date(startDate);
        endDate.setDate(startDate.getDate() + 6);
    }

    if(type === "monthly"){
        startDate = new Date(today.getFullYear(), today.getMonth(), 1);
        endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    }

    const dateKey = date =>
        date.getFullYear() + "-" +
        String(date.getMonth() + 1).padStart(2,"0") + "-" +
        String(date.getDate()).padStart(2,"0");

    const startKey = dateKey(startDate);
    const endKey = dateKey(endDate);

    const workerTotals = data.workers.map(worker => {
        let days = 0;

        Object.entries(worker.attendance || {}).forEach(([date, status]) => {
            if(date >= startKey && date <= endKey && status === "worked"){
                days++;
            }
        });

        const team = data.teams.find(t => t.id === worker.teamId);
        const site = team
            ? data.sites.find(s => s.id === team.siteId)
            : null;

        return {
            name: worker.name,
            team: team ? team.name : "Ekip yok",
            site: site ? site.name : "Şantiye yok",
            days: days
        };
    });

    const teamTotals = {};
    const siteTotals = {};

    workerTotals.forEach(worker => {
        if(!teamTotals[worker.team]){
            teamTotals[worker.team] = 0;
        }

        if(!siteTotals[worker.site]){
            siteTotals[worker.site] = 0;
        }

        teamTotals[worker.team] += worker.days;
        siteTotals[worker.site] += worker.days;
    });

    const totalDays = workerTotals.reduce(
        (sum, worker) => sum + worker.days, 0
    );

    const periodName = {
        daily: "Günlük",
        weekly: "Haftalık",
        monthly: "Aylık"
    };

    let html = `
        <h3>${periodName[type]} Raporu</h3>
        <p>${startKey} - ${endKey}</p>

        <h2>Toplam çalışma günü: ${totalDays}</h2>

        <h3>Çalışan Raporu</h3>
        <div style="overflow-x:auto">
        <table style="width:100%;border-collapse:collapse;text-align:left">
            <tr>
                <th>Çalışan</th>
                <th>Ekip</th>
                <th>Şantiye</th>
                <th>Gün</th>
            </tr>
    `;

    workerTotals.forEach(worker => {
        html += `
            <tr>
                <td>${worker.name}</td>
                <td>${worker.team}</td>
                <td>${worker.site}</td>
                <td>${worker.days}</td>
            </tr>
        `;
    });

    html += `
        </table>
        </div>

        <h3>Ekip Raporu</h3>
        <div style="overflow-x:auto">
        <table style="width:100%;border-collapse:collapse;text-align:left">
            <tr>
                <th>Ekip</th>
                <th>Toplam gün</th>
            </tr>
    `;

    Object.entries(teamTotals).forEach(([name, days]) => {
        html += `<tr><td>${name}</td><td>${days}</td></tr>`;
    });

    html += `
        </table>
        </div>

        <h3>Şantiye Raporu</h3>
        <div style="overflow-x:auto">
        <table style="width:100%;border-collapse:collapse;text-align:left">
            <tr>
                <th>Şantiye</th>
                <th>Toplam gün</th>
            </tr>
    `;

    Object.entries(siteTotals).forEach(([name, days]) => {
        html += `<tr><td>${name}</td><td>${days}</td></tr>`;
    });

    html += `</table></div>`;

    content.innerHTML = html;
}

function loadData(){

    const saved =
        localStorage.getItem(
            "kurumsalPuantaj"
        );

    if(saved){

        try{

            data=JSON.parse(saved);

        }catch(e){

            data={
                company:"",
                sites:[],
                teams:[],
                workers:[],
                payments:[]
            };

        }

    }

    if(!Array.isArray(data.payments)){

    data.payments=[];

    }

}


/* KAYDET */

function saveData(){

    localStorage.setItem(

        "kurumsalPuantaj",

        JSON.stringify(data)

    );

}


/* ŞİRKET */

function saveCompany(){

    data.company =
        document
        .getElementById("companyInput")
        .value
        .trim();

    saveData();

    render();

}


/* MODAL */

function openModal(type){

    modalType=type;

    document
    .getElementById("modal")
    .classList.add("show");

    const title =
        document
        .getElementById("modalTitle");

    const input =
        document
        .getElementById("modalName");

    const teamBox =
        document
        .getElementById("teamSelectBox");

    // Her modal açılışında önceki modalın alanlarını sıfırla.
    teamBox.style.display = "none";
    document.getElementById("teamSiteBox").style.display = "none";
    document.getElementById("paymentFields").style.display = "none";

    input.value="";


    if(type==="site"){

        title.textContent=
            "Yeni Şantiye";

        input.placeholder=
            "Örn: Nilüfer Konut Projesi";

    }


    if(type==="team"){

        title.textContent=
            "Yeni Ekip";

        input.placeholder=
            "Örn: Kalıpçı Ekibi";
        document.getElementById("teamSiteBox").style.display = "block";

const siteSelect = document.getElementById("teamSite");

siteSelect.innerHTML = '<option value="">Şantiye seçin</option>';

data.sites.forEach(site => {
    siteSelect.innerHTML +=
        `<option value="${site.id}">${site.name}</option>`;
});

    }


    if(type==="worker"){

    
    title.textContent =
        "Yeni Çalışan";

    input.placeholder =
        "Ad Soyad";

    teamBox.style.display =
        "block";

    document.getElementById(
        "workerRole"
    ).value = "";

    document.getElementById(
        "workerPhone"
    ).value = "";

    document.getElementById(
        "workerWage"
    ).value = "";

    renderTeamSelect();

    }

    if(type==="payment"){

    title.textContent =
    "Yeni Ödeme";

    input.placeholder =
    "Ödeme tutarı";

    teamBox.style.display =
    "none";

        document.getElementById(
    "paymentFields"
).style.display = "block";

        renderPaymentWorkers();

    }


    input.focus();

}

    /* ÖDEME ÇALIŞANLARI */

function renderPaymentWorkers(){

    const select =
        document.getElementById("paymentWorker");

    if(!select) return;

    select.innerHTML = "";

    data.workers.forEach(worker => {

        const option =
            document.createElement("option");

        option.value = worker.id;

        option.textContent =
            worker.name;

        select.appendChild(option);

    });

}


/* EKİP SEÇ */

function renderTeamSelect(){

    const select =
        document
        .getElementById("workerTeam");

    if(data.teams.length===0){

        select.innerHTML=
            `<option>
                Önce ekip ekleyin
            </option>`;

        return;

    }


    select.innerHTML=
        data.teams.map(team=>`

            <option value="${team.id}">
                ${team.name}
            </option>

        `).join("");

}


/* KAYDET */

function saveModal(){

    const name =
        document
        .getElementById("modalName")
        .value
        .trim();

    if(modalType==="payment"){

    const workerId =
        document.getElementById("paymentWorker").value;

    const type =
        document.getElementById("paymentType").value;

    const amount =
        Number(
            document.getElementById("paymentAmount").value
        );

    const note =
        document.getElementById("paymentNote").value.trim();

    if(!workerId){
        alert("Lütfen çalışan seçin.");
        return;
    }

    if(!amount || amount <= 0){
        alert("Lütfen geçerli bir ödeme tutarı girin.");
        return;
    }

    data.payments.push({

        id:
        "payment_" + Date.now(),

        workerId:
        workerId,

        type:
        type,

        amount:
        amount,

        note:
        note,

        date:
        new Date().toISOString()

    });

    saveData();

    closeModal();

    render();

    return;
    }

    if(!name){

        alert("Lütfen isim girin.");

        return;

    }


    if(modalType==="site"){

        data.sites.push({

            id:
            "site_"+Date.now(),

            name:name

        });

    }


    if(modalType==="team"){

       const selectedSiteId = document.getElementById("teamSite").value;

if (!selectedSiteId) {
    alert("Lütfen şantiye seçin.");
    return;
}

data.teams.push({
    id: "team_" + Date.now(),
    name: name,
    siteId: selectedSiteId
});


    }


   
if(modalType==="worker"){

    const team =
        document
        .getElementById("workerTeam")
        .value;

    const role =
        document
        .getElementById("workerRole")
        .value
        .trim();

    const phone =
        document
        .getElementById("workerPhone")
        .value
        .trim();

    const wage =
        Number(
            document
            .getElementById("workerWage")
            .value
        ) || 0;


    if(!team){

        alert(
            "Önce bir ekip seçin."
        );

        return;

    }


    data.workers.push({

        id:
        "worker_"+Date.now(),

        name:name,

        role:role,

        phone:phone,

        dailyWage:wage,

        teamId:team,

        status:"worked",

        attendance:{}

    });

}


    saveData();

    closeModal();

    render();

}


/* KAPAT */

function closeModal(){

    document
    .getElementById("modal")
    .classList.remove("show");

}


/* RENDER */

let openStatPanel = null;

function toggleStatPanel(type) {
    openStatPanel = openStatPanel === type ? null : type;
    renderStatDetails();

    document.querySelectorAll(".stats .stat").forEach(button => {
        const isActive = button.classList.contains(
            type === "sites" ? "statSites" :
            type === "teams" ? "statTeams" :
            type === "workers" ? "statWorkers" : "statToday"
        );
        button.setAttribute(
            "aria-expanded",
            String(openStatPanel !== null && isActive && openStatPanel === type)
        );
    });
}

function deleteWorker(workerId) {
    const worker = data.workers.find(w => w.id === workerId);
    if (!worker) return;

    showDeleteConfirm(worker.name, function () {
        data.workers = data.workers.filter(w => w.id !== workerId);
        data.payments = data.payments.filter(p => p.workerId !== workerId);

        saveData();
        render();

        if (currentTeamId) {
            renderTeamDetail();
        }
    });
}

function renderStatDetails() {
    const panel = document.getElementById("statDetails");
    if (!panel) return;

    if (!openStatPanel) {
        panel.hidden = true;
        panel.innerHTML = "";
        return;
    }

    let heading = "";
    let workers = [];

    if (openStatPanel === "sites") {
        heading = "Şantiyeler";
        panel.innerHTML = "";
        const rows = data.sites.map(site => site.name);
        workers = [];

        // Şantiye listesi
        panel.hidden = false;
        const title = document.createElement("h3");
        title.textContent = heading;
        panel.appendChild(title);

        if (rows.length === 0) {
            panel.append("Henüz kayıt bulunmuyor.");
            return;
        }

        const list = document.createElement("ul");
        rows.forEach(name => {
            const item = document.createElement("li");
            item.textContent = name;
            list.appendChild(item);
        });
        panel.appendChild(list);
        return;
    }

    if (openStatPanel === "teams") {
        heading = "Ekipler";
        panel.innerHTML = "";
        const rows = data.teams.map(team => {
            const site = data.sites.find(item => item.id === team.siteId);
            return team.name + " — " + (site ? site.name : "Şantiye belirtilmedi");
        });

        panel.hidden = false;
        const title = document.createElement("h3");
        title.textContent = heading;
        panel.appendChild(title);

        if (rows.length === 0) {
            panel.append("Henüz kayıt bulunmuyor.");
            return;
        }

        const list = document.createElement("ul");
        rows.forEach(name => {
            const item = document.createElement("li");
            item.textContent = name;
            list.appendChild(item);
        });
        panel.appendChild(list);
        return;
    }

    if (openStatPanel === "workers") {
        heading = "Çalışanlar";
        workers = data.workers;
    } else if (openStatPanel === "today") {
        heading = "Bugün Çalışanlar";

        const today = new Date();
        const todayKey = today.getFullYear() + "-" +
            String(today.getMonth() + 1).padStart(2, "0") + "-" +
            String(today.getDate()).padStart(2, "0");

        workers = data.workers.filter(worker =>
            (worker.attendance && worker.attendance[todayKey] || "worked") === "worked"
        );
    }

    panel.hidden = false;
    panel.innerHTML = "";

    const title = document.createElement("h3");
    title.textContent = heading;
    panel.appendChild(title);

    if (workers.length === 0) {
        const empty = document.createElement("p");
        empty.textContent = "Henüz kayıt bulunmuyor.";
        panel.appendChild(empty);
        return;
    }

    const list = document.createElement("ul");

    workers.forEach(worker => {
        const item = document.createElement("li");
        item.style.marginBottom = "12px";

        const name = document.createElement("span");
        name.textContent = worker.name;

        const team = data.teams.find(t => t.id === worker.teamId);
        if (openStatPanel === "workers") {
            name.textContent += " — " + (team ? team.name : "Ekip yok");
        }

        const button = document.createElement("button");
        button.type= "button"
        button.textContent = "🗑️ Sil";
        button.style.cssText = `
            margin-left: 12px;
            padding: 5px 10px;
            border: 0;
            border-radius: 7px;
            background: #d93025;
            color: white;
        `;
        button.onclick = () => deleteWorker(worker.id);

        item.append(name, button);
        list.appendChild(item);
    });

    panel.appendChild(list);
}

function render(){

    document
    .getElementById("companyName")
    .textContent=
        data.company ||
        "Şirket Adı";


    document
    .getElementById("companyInput")
    .value=
        data.company || "";


    document
    .getElementById("siteCount")
    .textContent=
        data.sites.length;


    document
    .getElementById("teamCount")
    .textContent=
        data.teams.length;


    document
    .getElementById("workerCount")
    .textContent=
        data.workers.length;


    const today = new Date();
    const todayKey = today.getFullYear() + "-" +
        String(today.getMonth() + 1).padStart(2, "0") + "-" +
        String(today.getDate()).padStart(2, "0");
    const todayWorkers = data.workers.filter(worker =>
        (worker.attendance && worker.attendance[todayKey] || "worked") === "worked"
    );
    document.getElementById("todayCount").textContent = todayWorkers.length;

    renderStatDetails();
    renderSites();

    renderTeams();

}


/* ŞANTİYELER */

let selectedSiteId = null;

 
function renderSites() {
    const box = document.getElementById("sites");

    if (data.sites.length === 0) {
        box.innerHTML = `
            <div class="empty">
                <div class="emptyIcon">🏗️</div>
                Henüz şantiye eklenmedi.
            </div>
        `;
        return;
    }

    box.innerHTML = data.sites.map(site => {
        const teams = data.teams.filter(
            t => t.siteId === site.id
        );

        const isOpen = selectedSiteId === site.id;

        return `
            <div class="site">
                <div class="siteName"
                     onclick="toggleSite('${site.id}')">
                    ${site.name}
                </div>

                <div class="siteInfo">
                    <div class="badge">
                        ${teams.length} ekip
                    </div>

                    <button
                        class="deleteBtn"
                        onclick="event.stopPropagation(); deleteSite('${site.id}')">
                        🗑️ Sil
                    </button>
                </div>

                ${isOpen ? `
                    <div class="siteDetails">
                        <h3>Şantiye Detayları</h3>
                        <h4>Ekipler</h4>
                        ${teams.length
                            ? teams.map(team => `
                                <div class="teamRow">
                                    ${team.name}
                                </div>
                            `).join("")
                            : "<p>Henüz ekip yok.</p>"
                        }
                    </div>
                ` : ""}
            </div>
        `;
    }).join("");
}

   
function deleteSite(siteId) {
    const site = data.sites.find(s => s.id === siteId);
    if (!site) return;

    showDeleteConfirm(site.name, function () {
        data.sites = data.sites.filter(s => s.id !== siteId);
        data.teams = data.teams.filter(t => t.siteId !== siteId);

        if (selectedSiteId === siteId) {
            selectedSiteId = null;
        }

        saveData();
        renderSites();
        renderTeams();
    });
}

function showDeleteConfirm(siteName, onConfirm) {
    const overlay = document.createElement("div");

    overlay.style.cssText = `
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.65);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 99999;
        padding: 20px;
    `;

    const modal = document.createElement("div");

    modal.style.cssText = `
        background: #fff;
        color: #222;
        padding: 24px;
        border-radius: 16px;
        width: 100%;
        max-width: 360px;
        text-align: center;
        font-family: Arial, sans-serif;
    `;

    const title = document.createElement("h3");
    title.textContent = "Kurumsal Puantaj";
    const message = document.createElement("p");
    message.textContent = `"${siteName}" kaydını silmek istediğine emin misin?`;

    const buttons = document.createElement("div");
    buttons.style.cssText = `
        display: flex;
        gap: 10px;
        margin-top: 20px;
    `;

    const cancel = document.createElement("button");
    cancel.textContent = "Vazgeç";

    const confirm = document.createElement("button");
    confirm.textContent = "Sil";

    cancel.style.cssText = `
        flex: 1;
        padding: 12px;
        border: none;
        border-radius: 8px;
        background: #ddd;
        color: #222;
    `;

    confirm.style.cssText = `
        flex: 1;
        padding: 12px;
        border: none;
        border-radius: 8px;
        background: #d93025;
        color: white;
    `;

    cancel.onclick = () => overlay.remove();

    confirm.onclick = () => {
        overlay.remove();
        onConfirm();
    };

    buttons.append(cancel, confirm);
    modal.append(title, message, buttons);
    overlay.appendChild(modal);
    document.body.appendChild(overlay);
}
   function toggleSite(siteId) {
    selectedSiteId =
        selectedSiteId === siteId ? null : siteId;

    renderSites();
} 
 
    /* ==================================================
   EKİP DETAY SİSTEMİ
================================================== */

let currentTeamId = null;

let teamDate =
    new Date();


/* EKİBİ AÇ */

function openTeam(teamId){

    currentTeamId =
        teamId;

    const team =
        data.teams.find(
            t => t.id === teamId
        );

    if(!team)
        return;


    document
    .querySelector(".main")
    .style.display="none";


    document
    .getElementById("teamDetail")
    .style.display="block";


    document
    .getElementById("detailTeamName")
    .textContent =
        team.name;


    const site =
        data.sites.find(
            s => s.id === team.siteId
        );


    document
    .getElementById("detailSiteName")
    .textContent =
        site
        ? site.name
        : "Şantiye belirtilmedi";


    renderTeamDetail();

}


/* EKİBİ KAPAT */

function closeTeam(){

    document
    .getElementById("teamDetail")
    .style.display="none";


    document
    .querySelector(".main")
    .style.display="block";

    currentTeamId=null;

}


/* TARİH */

function formatTeamDate(){

    const months=[
        "Ocak",
        "Şubat",
        "Mart",
        "Nisan",
        "Mayıs",
        "Haziran",
        "Temmuz",
        "Ağustos",
        "Eylül",
        "Ekim",
        "Kasım",
        "Aralık"
    ];

    return (

        teamDate.getDate()
        +
        " "
        +
        months[
            teamDate.getMonth()
        ]
        +
        " "
        +
        teamDate.getFullYear()

    );

}


function getTeamDateKey(){

    return (

        teamDate.getFullYear()
        +
        "-"
        +
        String(
            teamDate.getMonth()+1
        ).padStart(2,"0")
        +
        "-"
        +
        String(
            teamDate.getDate()
        ).padStart(2,"0")

    );

}


function changeTeamDate(amount){

    teamDate.setDate(
        teamDate.getDate()+amount
    );

    renderTeamDetail();

}


/* DURUM */

function getWorkerStatus(worker){

    if(!worker.attendance)
        worker.attendance={};

    return (

        worker.attendance[
            getTeamDateKey()
        ]
        ||
        "worked"

    );

}


/* DURUM DEĞİŞTİR */

function setWorkerStatus(
    workerId,
    status
){

    const worker =
        data.workers.find(
            w => w.id === workerId
        );

    if(!worker)
        return;


    if(!worker.attendance)
        worker.attendance={};


    worker.attendance[
        getTeamDateKey()
    ] =
        status;


    saveData();

    renderTeamDetail();

}


/* EKİP EKRANI */

function renderTeamDetail(){

    const team =
        data.teams.find(
            t => t.id === currentTeamId
        );

    if(!team)
        return;


    document
    .getElementById("teamDate")
    .textContent =
        formatTeamDate();


    const workers =
        data.workers.filter(
            w =>
            w.teamId ===
            currentTeamId
        );


    let worked=0;
    let absent=0;
    let leave=0;
    let report=0;


    workers.forEach(worker=>{

        const status =
            getWorkerStatus(worker);


        if(status==="worked")
            worked++;

        if(status==="absent")
            absent++;

        if(status==="leave")
            leave++;

        if(status==="report")
            report++;

    });


    document
    .getElementById("workedCount")
    .textContent=worked;


    document
    .getElementById("absentCount")
    .textContent=absent;


     document
    .getElementById("leaveCount")
    .textContent=leave;


    document
    .getElementById("reportCount")
    .textContent=report;


    const box =
        document
        .getElementById("teamWorkers");


    if(workers.length===0){

        box.innerHTML=`

            <div class="noWorkers">

                Bu ekipte henüz çalışan yok.

                <br><br>

                <button
                onclick="openModal('worker')"
                style="
                border:0;
                background:#2563eb;
                color:white;
                border-radius:9px;
                padding:10px 14px;
                font-weight:bold;
                ">

                    + İlk Çalışanı Ekle

                </button>

            </div>

        `;

        return;

    }


    box.innerHTML =
        workers.map(worker=>{

            const status =
                getWorkerStatus(worker);


            return `

                <div class="workerRow">

                    <div class="workerName">

                        ${worker.name}

                    </div>


                    <div class="statusButtons">


                        <button
                        class="${
                            status==="worked"
                            ? "active"
                            : ""
                        }"
                        onclick="
                        setWorkerStatus(
                            '${worker.id}',
                            'worked'
                        )">

                            ✓ Çalıştı

                        </button>


                        <button
                        class="${
                            status==="absent"
                            ? "active"
                            : ""
                        }"
                        onclick="
                        setWorkerStatus(
                            '${worker.id}',
                            'absent'
                        )">

                            ✕ Gelmedi

                        </button>


                        <button
                        class="${
                            status==="leave"
                            ? "active"
                            : ""
                        }"
                        onclick="
                        setWorkerStatus(
                            '${worker.id}',
                            'leave'
                        )">

                            İzinli

                        </button>


                        <button
                        class="${
                            status==="report"
                            ? "active"
                            : ""
                        }"
                        onclick="
                        setWorkerStatus(
                            '${worker.id}',
                            'report'
                        )">

                            Raporlu

                        </button>


                        <button
                        class="${
                            status==="half"
                            ? "active"
                            : ""
                        }"
                        onclick="
                        setWorkerStatus(
                            '${worker.id}',
                            'half'
                        )">

                            ½ Gün

                        </button>


                    </div>

                </div>

            `;

        }).join("");

}

/* BAŞLAT */

  /* TELEFON GERİ TUŞU */

const kurumsalSayfaAdresi = location.href;

history.pushState(
    { kurumsal: true },
    "",
    kurumsalSayfaAdresi
);

window.addEventListener("popstate", function () {

    const modal = document.getElementById("modal");
    const teamDetail = document.getElementById("teamDetail");

    if (modal.classList.contains("show")) {
        closeModal();
    } else if (teamDetail.style.display === "block") {
        closeTeam();
    }

    history.pushState(
        { kurumsal: true },
        "",
        kurumsalSayfaAdresi
    );

});  

    
// VERİLERİ YEDEKLE
function backupData() {
  const backup = {
    backupDate: new Date().toISOString(),
    data: data
  };

  const blob = new Blob(
    [JSON.stringify(backup, null, 2)],
    { type: "application/json" }
  );

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = "puantaj-yedek-" +
    new Date().toISOString().slice(0, 10) + ".json";

  document.body.appendChild(link);
  link.click();
  link.remove();

  URL.revokeObjectURL(url);
  alert("Yedek dosyası indirildi.");
}

// YEDEKTEN GERİ YÜKLE
function restoreData(event) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();

  reader.onload = function(e) {
    try {
      const backup = JSON.parse(e.target.result);
      const restored = backup.data;
        console.log("Yedek içeriği:", backup);

      if (
        !restored ||
        !Array.isArray(restored.sites) ||
        !Array.isArray(restored.teams) ||
        !Array.isArray(restored.workers) ||
        !Array.isArray(restored.payments)
      ) {
        alert("Yedek dosyasının biçimi geçerli değil.");
        return;
      }

      const confirmed = confirm(
        "Mevcut veriler yedekteki verilerle değiştirilecek. Devam edilsin mi?"
      );

      if (!confirmed) return;

      data = restored;
      saveData();
      render();

      alert("Yedek başarıyla geri yüklendi.");
    } catch (error) {
      
        alert("Hata: " + error.message);
    } finally {
      event.target.value = "";
    }
  };

  reader.readAsText(file);
}


loadData();

render();
