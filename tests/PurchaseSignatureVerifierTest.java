import com.ediptan.kurumsalpuantaj.PurchaseSignatureVerifier;
import java.security.*;
import java.nio.charset.StandardCharsets;
public class PurchaseSignatureVerifierTest {
    public static void main(String[] args)throws Exception{
        // Ephemeral test fixtures only; no application signing or upload key is created or saved.
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);
        KeyPair fixture=generator.generateKeyPair(),other=generator.generateKeyPair();
        String json="{\"packageName\":\"com.ediptan.kurumsalpuantaj\",\"productId\":\"puantaj_pro\"}";
        for(String algorithm:new String[]{"SHA1withRSA","SHA256withRSA"}){
            Signature signer=Signature.getInstance(algorithm);signer.initSign(fixture.getPrivate());signer.update(json.getBytes(StandardCharsets.UTF_8));byte[] signature=signer.sign();
            if(!PurchaseSignatureVerifier.verify(json,signature,fixture.getPublic().getEncoded()))throw new AssertionError("valid signature rejected");
            if(PurchaseSignatureVerifier.verify(json+" ",signature,fixture.getPublic().getEncoded()))throw new AssertionError("modified data accepted");
            if(PurchaseSignatureVerifier.verify(json,signature,other.getPublic().getEncoded()))throw new AssertionError("wrong key accepted");
            signature[0]^=1;if(PurchaseSignatureVerifier.verify(json,signature,fixture.getPublic().getEncoded()))throw new AssertionError("modified signature accepted");
        }
        if(PurchaseSignatureVerifier.verify(json,new byte[0],fixture.getPublic().getEncoded()))throw new AssertionError("empty signature accepted");
        if(PurchaseSignatureVerifier.validKey(new byte[]{1,2,3}))throw new AssertionError("bad key accepted");
        System.out.println("PASS Google purchase signature verification: authentic, tampered, wrong key, empty, malformed");
    }
}
