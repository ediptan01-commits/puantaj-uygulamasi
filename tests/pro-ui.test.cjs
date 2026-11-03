const test=require('node:test'),assert=require('node:assert/strict'),fs=require('fs'),vm=require('vm'),path=require('path');
const source=fs.readFileSync(path.join(__dirname,'../pro-ui.js'),'utf8');
function setup(individual=false,bridge=true){
 const prefix=individual?'individualPro':'pro';
 const elements={};
 for(const suffix of ['Modal','Status','Plan','Buy'])elements[prefix+suffix]={textContent:'',style:{},classList:{add(value){this.value=value;}},addEventListener(name,handler){this[name]=handler;}};
 elements[prefix+'Plan'].options=[{value:'aylik'},{value:'yillik'}];elements[prefix+'Plan'].value='aylik';
 const calls=[],states=[];
 const context={document:{getElementById:id=>elements[id]||null},window:{onProEntitlementChanged:x=>states.push(x)}};
 if(bridge)context.window.PlayBilling={isPro:()=>calls.push('restore'),getPlans:()=>calls.push('plans'),manageSubscription:()=>calls.push('manage')};
 vm.runInNewContext(source,context);
 return {window:context.window,el:s=>elements[prefix+s],calls,states};
}
for(const individual of [false,true]){
 test(`${individual?'Individual':'Corporate'}: plan prices, closed sales, PRO, revoked status`,()=>{
  const {window:w,el,calls,states}=setup(individual);
  (individual?w.openIndividualPro:w.openPro)();
  assert.deepEqual(calls,['restore','plans']);assert.equal(el('Modal').classList.value,'show');
  assert.equal(el('Buy').disabled,true);
  w.onProCatalog([{plan:'aylik',price:'₺49,99',purchasable:false},{plan:'yillik',price:'₺499,99',purchasable:false}]);
  assert.ok(el('Plan').options[0].textContent.includes('₺49,99 / ay'));
  assert.equal(el('Buy').disabled,true,'no sales while preparation config is false');
  // Future enabled catalog can offer only its eligible plan, never arbitrary client pricing.
  w.onProCatalog([{plan:'yillik',price:'₺499,99',purchasable:true}]);
  assert.equal(el('Buy').disabled,true);
  el('Plan').value='yillik';el('Plan').change();assert.equal(el('Buy').disabled,false);
  w.onProEntitlementChanged(true);assert.equal(el('Buy').disabled,true);
  w.onProEntitlementChanged(false);assert.equal(el('Buy').disabled,false);
  assert.deepEqual(states,[true,false]);
  w.manageProSubscription();assert.equal(calls.at(-1),'manage');
 });
}
test('web-only view does not offer payment and handles absent Play app',()=>{
 const {window:w,el}=setup(false,false);w.openPro();
 assert.equal(el('Buy').disabled,true);assert.match(el('Status').textContent,/Android/);
 w.manageProSubscription();assert.match(el('Status').textContent,/Google Play/);
});
