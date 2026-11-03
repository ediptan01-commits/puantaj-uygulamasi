import test from 'node:test';import assert from 'node:assert/strict';
import {verifyPurchase,PACKAGE,LEGACY,SUBSCRIPTION} from '../core.mjs';
const now=Date.parse('2026-11-02T00:00:00Z');
const request=id=>({packageName:PACKAGE,purchaseToken:'test-token',products:[id]});
const subscription=(state,expiry='2026-12-02T00:00:00Z',productId=SUBSCRIPTION)=>({subscriptionState:state,lineItems:[{productId,expiryTime:expiry}]});
test('legacy completed restores; pending, refund, consumed do not',async()=>{
 for(const state of [0,1,2])for(const consumed of [0,1]){
  const r=await verifyPurchase(request(LEGACY),async()=>({purchaseState:state,consumptionState:consumed}),now);
  assert.equal(r.active,state===0&&consumed===0);
 }
});
test('subscription lifecycle, cancellation and expiry use Play state',async()=>{
 for(const state of ['ACTIVE','IN_GRACE_PERIOD','CANCELED','EXPIRED','ON_HOLD','PAUSED','PENDING']){
  const r=await verifyPurchase(request(SUBSCRIPTION),async()=>subscription('SUBSCRIPTION_STATE_'+state),now);
  assert.equal(r.active,['ACTIVE','IN_GRACE_PERIOD','CANCELED'].includes(state));
 }
 for(const expiry of ['2026-11-01T00:00:00Z','2026-11-02T00:00:00Z','invalid']){
  assert.equal((await verifyPurchase(request(SUBSCRIPTION),async()=>subscription('SUBSCRIPTION_STATE_ACTIVE',expiry),now)).active,false);
 }
 assert.equal((await verifyPurchase(request(SUBSCRIPTION),async()=>subscription('SUBSCRIPTION_STATE_ACTIVE',undefined,'wrong-product'),now)).active,false);
});
test('wrong package, unknown product and malformed payload rejected before Play',async()=>{
 for(const r of [{...request(LEGACY),packageName:'other'},request('unknown'),{...request(LEGACY),purchaseToken:''},{...request(LEGACY),products:[LEGACY,SUBSCRIPTION]},null]){
  await assert.rejects(()=>verifyPurchase(r,()=>{throw Error('should not call Play');},now),{message:'invalid_request'});
 }
});
test('network failure cannot grant PRO and token is escaped in URL',async()=>{
 await assert.rejects(()=>verifyPurchase(request(LEGACY),async()=>{throw Error('offline');},now),/offline/);
 let path;
 await verifyPurchase({...request(LEGACY),purchaseToken:'a/b?c'},async p=>{path=p;return {purchaseState:0,consumptionState:0};},now);
 assert.ok(path.endsWith('/a%2Fb%3Fc'));
});
