package org.keycloak.social.microsoft;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.util.JsonSerialization;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MicrosoftAzureClient {

    private static final Logger logger = Logger.getLogger(MicrosoftAzureClaims.class);

    private final String encodedAccessToken;

    private final String tenantId;

    public MicrosoftAzureClient(final String issuer, final String encodedAccessToken) {
        this.encodedAccessToken = encodedAccessToken;

        String[] parts = issuer.split("/");
        tenantId = parts[parts.length-2];
    }

    public List<String> getUserGroups(String userId) {
        final String graphApiUrl = String.format("https://graph.microsoft.com/v1.0/%s/users/%s/getMemberObjects", tenantId, userId);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPost postRequest = new HttpPost(graphApiUrl);
            postRequest.setHeader("Content-Type", "application/json");
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer "+encodedAccessToken);

            String body = "{\"securityEnabledOnly\": true}";
            StringEntity requestEntity = new StringEntity(body, "UTF-8");
            postRequest.setEntity(requestEntity);

            final CloseableHttpResponse response = httpClient.execute(postRequest);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                    logger.error("Failed to retrieve group memberships from AzureAD. Make sure the application has User.Read and GroupMember.Read.All permissions");
                }
                throw new RuntimeException();
            }

            HttpEntity responseEntity = response.getEntity();
            String responseJson = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

            MicrosoftGraphGroups groups = JsonSerialization.readValue(responseJson, MicrosoftGraphGroups.class);
            return groups.getValue();
        } catch (Exception e) {
            throw new IdentityBrokerException("unable to query microsoft graph api");
        }
    }
}
