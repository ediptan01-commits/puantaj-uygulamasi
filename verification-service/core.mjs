export const PACKAGE='com.ediptan.kurumsalpuantaj';
export const LEGACY='kurumsal_puantaj_pro', SUBSCRIPTION='puantaj_pro';
export async function verifyPurchase(request,playGet,now=Date.now()){
    if(request?.packageName!==PACKAGE||typeof request.purchaseToken!=='string'
        ||request.purchaseToken.length<1||request.purchaseToken.length>4096
        ||!Array.isArray(request.products)||request.products.length!==1
        ||![LEGACY,SUBSCRIPTION].includes(request.products[0])){
        const error=new Error('invalid_request');error.status=400;throw error;
    }
    const token=request.purchaseToken,id=request.products[0];
    const path=id===LEGACY
        ?`applications/${PACKAGE}/purchases/products/${id}/tokens/${encodeURIComponent(token)}`
        :`applications/${PACKAGE}/purchases/subscriptionsv2/tokens/${encodeURIComponent(token)}`;
    const record=await playGet(path);
    let active=false;
    if(id===LEGACY){
        // A non-consumable, completed purchase. Pending/refunded/consumed items never grant access.
        active=record.purchaseState===0&&record.consumptionState===0;
    }else{
        const validStates=['SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_IN_GRACE_PERIOD','SUBSCRIPTION_STATE_CANCELED'];
        active=validStates.includes(record.subscriptionState)&&Array.isArray(record.lineItems)
            &&record.lineItems.some(item=>item.productId===id&&Number.isFinite(Date.parse(item.expiryTime))
                &&Date.parse(item.expiryTime)>now);
    }
    return {active,packageName:PACKAGE,productId:id,purchaseToken:token};
}
