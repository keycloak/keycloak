package org.keycloak.testsuite.util.oauth.device;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class DeviceAuthorizationResponse extends AbstractHttpResponse {

    private String deviceCode;
    private String userCode;
    private String verificationUri;
    private String verificationUriComplete;
    private int expiresIn;
    private int interval;

    public DeviceAuthorizationResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        ObjectNode responseJson = asJson();
        deviceCode = responseJson.get("device_code").asText();
        userCode = responseJson.get("user_code").asText();
        verificationUri = responseJson.get("verification_uri").asText();
        verificationUriComplete = responseJson.get("verification_uri_complete").asText();
        expiresIn = responseJson.get("expires_in").asInt();
        interval = responseJson.get("interval").asInt();
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public int getInterval() {
        return interval;
    }

}
