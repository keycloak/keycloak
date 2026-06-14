package org.keycloak.representations;

import org.keycloak.TokenCategory;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */
public class IDJAG extends AccessToken {

    @JsonProperty("client_id")
    protected String clientId;

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.IDJAG;
    }

}
