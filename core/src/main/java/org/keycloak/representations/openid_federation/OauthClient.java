package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OauthClient {

    @JsonUnwrapped
    private CommonMetadata commonMetadata;

    public OauthClient(){}

    public CommonMetadata getCommonMetadata() {
        return commonMetadata;
    }

    public void setCommonMetadata(CommonMetadata commonMetadata) {
        this.commonMetadata = commonMetadata;
    }
}
