package org.keycloak.protocol.oidc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BackchannelLogoutResponse {

    private boolean localLogoutSucceeded;
    private List<DownStreamBackchannelLogoutResponse> clientResponses = new ArrayList<>();

    public List<DownStreamBackchannelLogoutResponse> getClientResponses() {
        return clientResponses;
    }

    public void addClientResponses(DownStreamBackchannelLogoutResponse clientResponse) {
        this.clientResponses.add(clientResponse);
    }

    public boolean getLocalLogoutSucceeded() {
        return localLogoutSucceeded;
    }

    public void setLocalLogoutSucceeded(boolean localLogoutSucceeded) {
        this.localLogoutSucceeded = localLogoutSucceeded;
    }

    public static class DownStreamBackchannelLogoutResponse {
        protected boolean withBackchannelLogoutUrl;
        protected Integer responseCode;

        public boolean isWithBackchannelLogoutUrl() {
            return withBackchannelLogoutUrl;
        }

        public void setWithBackchannelLogoutUrl(boolean withBackchannelLogoutUrl) {
            this.withBackchannelLogoutUrl = withBackchannelLogoutUrl;
        }

        public Optional<Integer> getResponseCode() {
            return Optional.ofNullable(responseCode);
        }

        public void setResponseCode(Integer responseCode) {
            this.responseCode = responseCode;
        }
    }
}

