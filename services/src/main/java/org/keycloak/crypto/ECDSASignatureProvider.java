package org.keycloak.crypto;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ECDSASignatureProvider implements SignatureProvider {

    private final KeycloakSession session;
    private final String algorithm;

    public ECDSASignatureProvider(KeycloakSession session, String algorithm) {
        this.session = session;
        this.algorithm = algorithm;
    }

    @Override
    public SignatureSignerContext signer() throws SignatureException {
        return new ServerECDSASignatureSignerContext(session, algorithm);
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return new ServerECDSASignatureVerifierContext(session, kid, algorithm);
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }

    public static byte[] concatenatedRSToASN1DER(final byte[] signature, int signLength) throws IOException {
        int len = signLength / 2;
        int arraySize = len + 1;

        byte[] r = new byte[arraySize];
        byte[] s = new byte[arraySize];
        System.arraycopy(signature, 0, r, 1, len);
        System.arraycopy(signature, len, s, 1, len);
        BigInteger rBigInteger = new BigInteger(r);
        BigInteger sBigInteger = new BigInteger(s);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DERSequenceGenerator seqGen = new DERSequenceGenerator(bos);

        seqGen.addObject(new ASN1Integer(rBigInteger.toByteArray()));
        seqGen.addObject(new ASN1Integer(sBigInteger.toByteArray()));
        seqGen.close();
        bos.close();

        return bos.toByteArray();
    }

    public static byte[] asn1derToConcatenatedRS(final byte[] derEncodedSignatureValue, int signLength) throws IOException {
        int len = signLength / 2;

        ASN1InputStream asn1InputStream = new ASN1InputStream(derEncodedSignatureValue);
        ASN1Primitive asn1Primitive = asn1InputStream.readObject();
        asn1InputStream.close();

        ASN1Sequence asn1Sequence = (ASN1Sequence.getInstance(asn1Primitive));
        ASN1Integer rASN1 = (ASN1Integer) asn1Sequence.getObjectAt(0);
        ASN1Integer sASN1 = (ASN1Integer) asn1Sequence.getObjectAt(1);
        X9IntegerConverter x9IntegerConverter = new X9IntegerConverter();
        byte[] r = x9IntegerConverter.integerToBytes(rASN1.getValue(), len);
        byte[] s = x9IntegerConverter.integerToBytes(sASN1.getValue(), len);

        byte[] concatenatedSignatureValue = new byte[signLength];
        System.arraycopy(r, 0, concatenatedSignatureValue, 0, len);
        System.arraycopy(s, 0, concatenatedSignatureValue, len, len);

        return concatenatedSignatureValue;
    }

    public enum ECDSA {
        ES256(64),
        ES384(96),
        ES512(132);

        private final int signatureLength;

        ECDSA(int signatureLength) {
            this.signatureLength = signatureLength;
        }

        public int getSignatureLength() {
            return this.signatureLength;
        }
    }
}
