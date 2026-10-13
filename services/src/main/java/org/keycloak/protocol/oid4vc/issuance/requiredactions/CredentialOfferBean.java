package org.keycloak.protocol.oid4vc.issuance.requiredactions;

import java.io.IOException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.utils.QRCodeUtils;

import com.google.zxing.WriterException;

public class CredentialOfferBean {

    private final String uri;
    private final String qrCode;

    public CredentialOfferBean(KeycloakSession session, String nonce) throws WriterException, IOException {
        this.uri = OID4VCUtil.getOfferAsUri(session, nonce);
        this.qrCode = QRCodeUtils.encodeAsQRString(this.uri, 246, 246);
    }

    public String getUri() {
        return uri;
    }

    public String getQrCode() {
        return qrCode;
    }
}
