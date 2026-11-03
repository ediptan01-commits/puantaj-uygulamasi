package com.ediptan.kurumsalpuantaj;

/** Unknown entitlement never permits an ad request. No feature depends on ads. */
public final class AdPolicy {
    private AdPolicy(){}
    public static boolean mayRequest(boolean enabled,Boolean pro,boolean consent){
        return enabled && Boolean.FALSE.equals(pro) && consent;
    }
}
