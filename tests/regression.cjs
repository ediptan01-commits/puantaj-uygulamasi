const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),path=require('path');
const root=path.join(__dirname,'..');
const source=fs.readFileSync(path.join(root,'kurumsal.js'),'utf8');
let csv,pdf;
const elements=new Map();
const element=id=>{if(!elements.has(id))elements.set(id,{style:{},classList:{add(){},remove(){}},textContent:''});return elements.get(id);};
const context={console,Date,location:{href:'file:///android_asset/kurumsal.html'},history:{pushState(){}},window:{addEventListener(){},PuantajNative:{saveText(...args){csv=args;},printHtml(html){pdf=html;}}},
 document:{getElementById:element,querySelector:()=>element('buy')},
 localStorage:{getItem:()=> '1',setItem(){throw Error('PRO must not persist in local storage');}},setTimeout(){}};
vm.createContext(context);
vm.runInContext(source.replace(/loadData\(\);\s*render\(\);\s*$/,''),context);
assert.equal(vm.runInContext('isProUnlocked()',context),false,'old local storage cannot unlock PRO');
vm.runInContext('onProEntitlementChanged("true")',context);
assert.equal(vm.runInContext('isProUnlocked()',context),false,'string must not unlock');
vm.runInContext('onProEntitlementChanged(true)',context);
assert.equal(vm.runInContext('isProUnlocked()',context),true);
vm.runInContext('onProEntitlementChanged(false)',context);
assert.equal(vm.runInContext('isProUnlocked()',context),false);
vm.runInContext(`data.company='Örnek';data.workers=[{id:'w1',name:'<Ali>',attendance:{}}];exportReportCSV();printReportPDF();`,context);
assert.ok(csv[2].includes('<Ali>'),'CSV remains free without PRO');
assert.ok(pdf.includes('&lt;Ali&gt;'),'PDF remains free and escapes worker name');
const workflow=fs.readFileSync(path.join(root,'.github/workflows/build-android.yml'),'utf8');
assert.ok(workflow.includes('applicationId "com.ediptan.kurumsalpuantaj"'));
assert.ok(workflow.includes('SIGNING_KEY_BASE64'));
assert.ok(!workflow.includes('gh release create'));
assert.ok(!workflow.includes('keytool -genkeypair'));
assert.equal(JSON.parse(fs.readFileSync(path.join(root,'billing-config.json'))).salesEnabled,false);
console.log('PASS: storage bypass, strict entitlement, revoke, free CSV/PDF, escaping, package, existing key, sales disabled, no release publishing');
