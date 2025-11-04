package org.keycloak.protocol.ssf.receiver;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;

public class SsfReceiverKeyModel extends ComponentModel {

    public SsfReceiverKeyModel() {}

    public SsfReceiverKeyModel(ComponentModel model) {
        super(model);
    }

    public String getKid() {
        return getConfig().getFirst("kid");
    }

    public void setKid(String kid) {
        getConfig().putSingle("kid",kid);
    }

    public String getAlgorithm() {
        return getConfig().getFirst("alg");
    }

    public void setAlgorithm(String alg) {
        getConfig().putSingle("alg",alg);
    }

    public KeyUse getKeyUse() {
        return KeyUse.valueOf(getConfig().getFirst("use"));
    }

    public void setKeyUse(KeyUse keyUse) {
        getConfig().putSingle("use",keyUse.name());
    }

    public String getPublicKey() {
        return getConfig().getFirst("publicKey");
    }

    public void setPublicKey(String publicKey) {
        getConfig().putSingle("publicKey",publicKey);
    }

    public String getType() {
        return getConfig().getFirst("type");
    }

    public void setType(String type) {
        getConfig().putSingle("type",type);
    }
}
