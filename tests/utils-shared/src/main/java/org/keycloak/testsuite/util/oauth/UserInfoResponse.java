package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.representations.UserInfo;

import java.io.IOException;

public class UserInfoResponse extends AbstractHttpResponse {

    private UserInfo userInfo;

    public UserInfoResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        userInfo = asJson(UserInfo.class);
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

}
