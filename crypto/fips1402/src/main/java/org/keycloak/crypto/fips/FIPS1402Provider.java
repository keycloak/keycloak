package org.keycloak.crypto.fips;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.math.ec.ECCurve;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoProviderTypes;
import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.common.crypto.PemUtilsProvider;


/**
 * Integration based on FIPS 140-2
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402Provider implements CryptoProvider {

    private Map<String, Supplier<?>> providers = new HashMap<>();

    public FIPS1402Provider() {
        providers.put(CryptoProviderTypes.BC_SECURITY_PROVIDER, BouncyCastleFipsProvider::new);
        providers.put(CryptoProviderTypes.AES_KEY_WRAP_ALGORITHM_PROVIDER, FIPSAesKeyWrapAlgorithmProvider::new);
    }

    @Override
    public SecureRandom getSecureRandom() throws NoSuchAlgorithmException, NoSuchProviderException {
        return SecureRandom.getInstance("DEFAULT","BCFIPS");
    }

    @Override
    public <T> T getAlgorithmProvider(Class<T> clazz, String algorithm) {
        Object o = providers.get(algorithm).get();
        if (o == null) {
            throw new IllegalArgumentException("Not found provider of algorithm: " + algorithm);
        }
        return clazz.cast(o);
    }

    @Override
    public CertificateUtilsProvider getCertificateUtils() {
        return new BCFIPSCertificateUtilsProvider();
    }

    @Override
    public PemUtilsProvider getPemUtils() {
        return new BCFIPSPemUtilsProvider();
    }

    /* Create EC Params using BC FipS APIs.
     * 
     * @see org.keycloak.common.crypto.CryptoProvider#createECParams(java.lang.String)
     */
    @Override
    public ECParameterSpec createECParams(String curveName) {
        X9ECParameters params = ECNamedCurveTable.getByName(curveName);
        ECField field ;
        ECCurve ecCurve = params.getCurve();
        if (ecCurve instanceof ECCurve.F2m) {
            ECCurve.F2m f2m = (ECCurve.F2m) ecCurve;
            field = new ECFieldF2m(f2m.getM(), new int[] { f2m.getK1(), f2m.getK2(), f2m.getK3()});
        }
        else
        if (ecCurve instanceof ECCurve.Fp) {
            ECCurve.Fp fp = (ECCurve.Fp) ecCurve;
            field = new ECFieldFp(fp.getQ());
        }
        else
            throw new RuntimeException("Unsupported curve");


        EllipticCurve c = new EllipticCurve(field,
                ecCurve.getA().toBigInteger(),
                ecCurve.getB().toBigInteger(),
                params.getSeed());
        ECPoint point = new ECPoint( params.getG().getXCoord().toBigInteger(), params.getG().getYCoord().toBigInteger());
        return new ECParameterSpec( c,point, params.getN(), params.getH().intValue());
    }
}
