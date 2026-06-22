package org.keycloak.representations;

import org.keycloak.TokenCategory;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 * @version $Revision: 1 $
 */
public class IDJAG extends AccessToken {

    @JsonProperty("client_id")
    protected String client_id;

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.IDJAG;
    }

}
