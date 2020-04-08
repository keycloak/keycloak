package org.keycloak.models;

import java.io.Serializable;

import org.jboss.logging.Logger;

public class CIBAPolicy implements Serializable {

    protected static final Logger logger = Logger.getLogger(CIBAPolicy.class);

    protected String backchannelTokenDeliveryMode = "poll";
    protected int expiresIn = 120;
    protected int interval = 0;
    protected String authRequestedUserHint = "login_hint";

    public CIBAPolicy() {
    }

    public static CIBAPolicy DEFAULT_POLICY = new CIBAPolicy();

    public String getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(String backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getAuthRequestedUserHint() {
        return authRequestedUserHint;
    }

    public void setAuthRequestedUserHint(String authRequestedUserHint) {
        this.authRequestedUserHint = authRequestedUserHint;
    }
}
