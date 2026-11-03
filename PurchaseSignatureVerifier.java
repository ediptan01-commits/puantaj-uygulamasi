package com.ediptan.kurumsalpuantaj;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/** Verifies the exact Google Play signed payload with this application's licensing public key. */
public final class PurchaseSignatureVerifier {
    private PurchaseSignatureVerifier(){}
    public static boolean verify(String json,byte[] signature,byte[] publicKey){
        if(json==null||json.isEmpty()||signature==null||signature.length==0||publicKey==null)return false;
        try{
            java.security.PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
            for(String algorithm:new String[]{"SHA256withRSA","SHA1withRSA"}){
                Signature verifier=Signature.getInstance(algorithm);
                verifier.initVerify(key);verifier.update(json.getBytes(StandardCharsets.UTF_8));
                if(verifier.verify(signature))return true;
            }
        }catch(Exception ignored){return false;}
        return false;
    }
    public static boolean validKey(byte[] publicKey){
        try{return ((java.security.interfaces.RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey))).getModulus().bitLength()>=2048;}
        catch(Exception ignored){return false;}
    }
}
