import com.ediptan.kurumsalpuantaj.AdPolicy;
public class AdPolicyTest {
    public static void main(String[] args){
        for(boolean enabled:new boolean[]{false,true})for(Boolean pro:new Boolean[]{null,false,true})for(boolean consent:new boolean[]{false,true}){
            boolean expected=enabled&&Boolean.FALSE.equals(pro)&&consent;
            if(AdPolicy.mayRequest(enabled,pro,consent)!=expected)throw new AssertionError("ad gating");
        }
        System.out.println("PASS 12 combinations: disabled ads, PRO, unknown entitlement and missing consent never request ads");
    }
}
