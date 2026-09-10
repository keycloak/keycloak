package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.representations.UserInfo;

import org.apache.http.client.methods.CloseableHttpResponse;

import static org.apache.http.HttpHeaders.WWW_AUTHENTICATE;

public class UserInfoResponse extends AbstractHttpResponse {

    private UserInfo userInfo;

    public UserInfoResponse(CloseableHttpResponse response) throws IOException {
        super(response);
        if (!isSuccess() && !isJson()) {
            // Error and error_description inside WWW-Authenticate HTTP header. See OIDC specification, section 5.3.3
            String wwwAuthenticate = getHeader(WWW_AUTHENTICATE);
            if (wwwAuthenticate != null) {
                Matcher errorMatcher = Pattern.compile("error=\"(.*?)\"").matcher(wwwAuthenticate);
                if (errorMatcher.find()) {
                    setError(errorMatcher.group(1));
                }
                Matcher errorDescMatcher = Pattern.compile("error_description=\"(.*?)\"").matcher(wwwAuthenticate);
                if (errorDescMatcher.find()) {
                    setErrorDescription(errorDescMatcher.group(1));
                }
            }
        }
    }

    @Override
    protected void parseContent() throws IOException {
        userInfo = asJson(UserInfo.class);
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

}
