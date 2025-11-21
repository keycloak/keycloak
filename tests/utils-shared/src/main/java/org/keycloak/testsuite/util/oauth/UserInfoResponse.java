package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.representations.UserInfo;

import org.apache.http.client.methods.CloseableHttpResponse;

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
