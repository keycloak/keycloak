package org.keycloak.testsuite.oidc;

import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.ida.mappers.connector.IdaHttpConnector;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.Profile.Feature;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_REQUEST_CLAIMS_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_REQUEST_CLAIMS_JSON_SYNTAX_ERROR_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.IDA_PROVIDER_ID;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;


@EnableFeature(value = Feature.IDA, skipRestart = true)
public class IdaProtocolMapperTest extends AbstractKeycloakTest {

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        oauth.clientId("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void getIdTokenAccessTokenUserinfoTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void getIdTokenTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "false", "false");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();

        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), null);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void getAccessTokenTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("false", "true", "false");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims

        checkResponse(idToken.getOtherClaims(), null);

        String accessToken = response.getAccessToken();

        //check accessTokenClaims
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void getUserInfoTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("false", "false", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        checkResponse(idToken.getOtherClaims(), null);

        //check accessTokenClaims
        String accessToken = response.getAccessToken();
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), null);

        //check userinfoClaims
        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void anotherUserNameTest() throws Exception {
        ProtocolMappersResource protocolMappers =  setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "john-doh@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Bob");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Bob");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Bob");
            setExpectedTrustFramework("authority_claims_example_framework");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void valueRequestMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{\"value\":\"Sarah\"}}",
                "{\"trust_framework\":{\"value\":\"uk_tfida\"}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void valueRequestNotMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{}}", "{\"given_name\":{\"value\":\"Unknown\"}}",
                "{\"trust_framework\":{\"value\":\"unknown\"}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void valuesRequestMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{\"values\":[\"Unknown\",\"Sarah\"]}}",
                "{\"trust_framework\":{\"values\":[\"Unknown\",\"uk_tfida\"]}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void valuesRequestNotMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{}}", "{\"given_name\":{\"values\":[\"Unknown0\",\"Unknown1\"]}}",
                "{\"trust_framework\":{\"values\":[\"Unknown0\",\"unknown1\"]}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void maxAgeRequestMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{},\"time\":{\"max_age\": 2000000000}}", "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{},\"birthdate\":{\"max_age\": 2000000000}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedTime("2021-05-11T14:29Z");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedTime("2021-05-11T14:29Z");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedBirthdate("1976-03-11");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void maxAgeRequestNotMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{},\"time\":{\"max_age\": 10}}", "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{},\"birthdate\":{\"max_age\": 10}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        checkResponse(idToken.getOtherClaims(), null);

        String accessToken = response.getAccessToken();

        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), null);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void nestRequestMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{}}", "{\"address\":{\"locality\":{}}}",
                "{\"assurance_process\":{\"policy\":{\"value\":\"gpg45\"}}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
            setExpectedLocality("Edinburgh");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
            setExpectedLocality("Edinburgh");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedPolicy("gpg45");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void nestRequestNotMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{}}", "{\"address\":{\"locality\":{\"value\":\"unknown\"}}}",
                "{\"assurance_process\":{\"policy\":{\"value\":\"unknown\"}}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedTrustFramework("uk_tfida");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void assuranceDetailsTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{},\"assurance_process\":{\"assurance_details\":{}}}", "{\"given_name\":{}}",
                "{\"trust_framework\":{},\"assurance_process\":{\"assurance_details\":{}}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedAssuranceDetailsSize(3);
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedAssuranceDetailsSize(3);
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedAssuranceDetailsSize(3);
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void arrayMatchTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims("{\"trust_framework\":{},\"evidence\":[{\"type\":{\"value\":\"electronic_record\"},\"check_details\":[{\"check_method\":{},\"organization\":{\"value\":\"TheCreditBureau\"},\"txn\":{}}]}]}", "{\"given_name\":{}}",
                "{\"trust_framework\":{},\"evidence\":[{\"type\":{\"value\":\"electronic_record\"},\"check_details\":[{\"check_method\":{},\"organization\":{\"value\":\"TheCreditBureau\"},\"txn\":{}}]}]}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        ExpectedClaims expectedIdTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedType("electronic_record");
            setExpectedOrganization("TheCreditBureau");
        }};
        checkResponse(idToken.getOtherClaims(), expectedIdTokenClaims);

        String accessToken = response.getAccessToken();
        ExpectedClaims expectedAccessTokenClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedType("electronic_record");
            setExpectedOrganization("TheCreditBureau");
        }};
        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), expectedAccessTokenClaims);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        ExpectedClaims expectedUserInfoClaims = new ExpectedClaims() {{
            setExpectedGivenName("Sarah");
            setExpectedTrustFramework("uk_tfida");
            setExpectedType("electronic_record");
            setExpectedOrganization("TheCreditBureau");
        }};
        checkResponse(userInfo.getOtherClaims(), expectedUserInfoClaims);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void verifiedClaimsArrayTest() throws Exception {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("{\"userinfo\":{\"verified_claims\":[{\"verification\":{\"trust_framework\":null}},{\"claims\":{\"family_name\":null}}]},\"id_token\":{\"verified_claims\":[{\"verification\":{\"assurance_level\":null}},{\"claims\":{\"given_name\":null}}]}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        //check idTokenClaims
        List<Map<String, Object>> verifiedClaims = (List<Map<String, Object>>) idToken.getOtherClaims().get("verified_claims");
        Map<String, Object> verifiedClaim1 = verifiedClaims.get(0);
        Assert.assertEquals("medium", ((Map<String, Object>)verifiedClaim1.get("verification")).get("assurance_level"));
        Map<String, Object> verifiedClaim2 = verifiedClaims.get(1);
        Assert.assertEquals("Sarah", ((Map<String, Object>)verifiedClaim2.get("claims")).get("given_name"));

        UserInfo userInfo = oauth.doUserInfoRequest(response.getAccessToken());
        //check userinfo
        verifiedClaims = (List<Map<String, Object>>) userInfo.getOtherClaims().get("verified_claims");
        verifiedClaim1 = verifiedClaims.get(0);
        Assert.assertEquals("uk_tfida", ((Map<String, Object>)verifiedClaim1.get("verification")).get("trust_framework"));
        verifiedClaim2 = verifiedClaims.get(1);
        Assert.assertEquals("Meredyth", ((Map<String, Object>)verifiedClaim2.get("claims")).get("family_name"));

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void externalStoreConnectionErrorTest() throws Exception {
        ProtocolMapperRepresentation idaProtocolMapper = ModelToRepresentation.toRepresentation(createClaimMapper("ida"));
        Map<String, String> config = new HashMap<>();
        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        config.put(IdaHttpConnector.IDA_EXTERNAL_STORE_NAME, "http://unknown");

        idaProtocolMapper.setConfig(config);
        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(idaProtocolMapper));

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.SERVER_ERROR, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void externalStoreConnectionError2Test() throws Exception {
        ProtocolMapperRepresentation idaProtocolMapper = ModelToRepresentation.toRepresentation(createClaimMapper("ida"));
        Map<String, String> config = new HashMap<>();
        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        config.put(IdaHttpConnector.IDA_EXTERNAL_STORE_NAME, "http://192.168.0.1");

        idaProtocolMapper.setConfig(config);
        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(idaProtocolMapper));

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.SERVER_ERROR, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void requestClaimsEmptyTest() {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_REQUEST_CLAIMS_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void requestClaimsEmptyErrorTest() {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_REQUEST_CLAIMS_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void requestClaimsNotIncludeIdTokenErrorTest() {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("{\"verified_claims\":{\"claims\":{\"given_name\":null}}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        checkResponse(idToken.getOtherClaims(), null);

        String accessToken = response.getAccessToken();

        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), null);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void requestClaimsSyntaxErrorTest() {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("\"id_token\":{\"verified_claims\":{\"claims\":{\"given_name\":null}}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_REQUEST_CLAIMS_JSON_SYNTAX_ERROR_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void requestClaimsLetterErrorTest() {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("{\"USERINFO\":{\"verified_claims\":{\"verification\":{\"trust_framework\":null},\"claims\":{\"given_name\":null}}},\"id_tokens\":{\"verified_claims\":{\"claims\":{\"given_name\":null}}}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        checkResponse(idToken.getOtherClaims(), null);

        String accessToken = response.getAccessToken();

        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), null);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void requestClaimsLetterError2Test() {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");
        oauth.setStringClaims("{\"userinfo\":{\"VERIFIED_CLAIMS\":{\"verification\":{\"trust_framework\":null},\"claims\":{\"given_name\":null}}},\"id_token\":{\"verified_claim\":{\"claims\":{\"given_name\":null}}}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        //check idTokenClaims
        checkResponse(idToken.getOtherClaims(), null);

        String accessToken = response.getAccessToken();

        //check accessTokenClaims
        checkResponse(oauth.verifyToken(accessToken).getOtherClaims(), null);

        UserInfo userInfo = oauth.doUserInfoRequest(accessToken);

        //check userinfoClaims
        checkResponse(userInfo.getOtherClaims(), null);
        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void userClaimsEmptyErrorTest() throws IOException {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "keycloak-user@localhost", "password");

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.SERVER_ERROR, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void userClaimsSyntaxErrorTest() throws IOException {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "topGroupUser", "password");

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode());
        Assert.assertEquals(OAuthErrorException.SERVER_ERROR, response.getError());
        Assert.assertEquals(ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR, response.getErrorDescription());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void userClaimsLetterErrorTest() throws IOException {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "level2GroupUser", "password");
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        deleteIdaProtocolMapper(protocolMappers);
    }

    @Test
    public void userClaimsLetterError2Test() throws IOException {
        ProtocolMappersResource protocolMappers = setIdaProtocolMapper("true", "true", "true");

        setRequestClaims(null, "{\"given_name\":{}}",
                "{\"trust_framework\":{}}", "{\"given_name\":{}}");

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "rolerichuser", "password");
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        deleteIdaProtocolMapper(protocolMappers);
    }

    private void checkResponse(Map<String, Object> otherClaims, ExpectedClaims expectedClaims) {
        if (expectedClaims == null) {
            Assert.assertTrue(otherClaims.isEmpty());
            return;
        }

        Map<String, Object> verifiedClaims = (Map<String, Object>) otherClaims.get("verified_claims");
        if (expectedClaims.expectedGivenName != null) {
            Assert.assertEquals(expectedClaims.expectedGivenName,
                    ((Map<String, Object>) verifiedClaims.get("claims")).get("given_name"));
        }
        if (expectedClaims.expectedTrustFramework != null) {
            Assert.assertEquals(expectedClaims.expectedTrustFramework,
                    ((Map<String, Object>) verifiedClaims.get("verification")).get("trust_framework"));
        }
        if (expectedClaims.expectedTime != null) {
            Assert.assertEquals(expectedClaims.expectedTime,
                    ((Map<String, Object>) verifiedClaims.get("verification")).get("time"));
        }
        if (expectedClaims.expectedBirthdate != null) {
            Assert.assertEquals(expectedClaims.expectedBirthdate,
                    ((Map<String, Object>) verifiedClaims.get("claims")).get("birthdate"));
        }
        if (expectedClaims.expectedPolicy != null) {
            Assert.assertEquals(expectedClaims.expectedPolicy,
                    ((Map<String, Object>) ((Map<String, Object>) verifiedClaims.get("verification")).get("assurance_process")).get("policy"));
        }
        if (expectedClaims.expectedLocality != null) {
            Assert.assertEquals(expectedClaims.expectedLocality,
                    ((Map<String, Object>) ((Map<String, Object>) verifiedClaims.get("claims")).get("address")).get("locality"));
        }
        if (expectedClaims.expectedAssuranceDetailsSize != null) {
            Assert.assertEquals(expectedClaims.expectedAssuranceDetailsSize.intValue(),
                    ((List) ((Map<String, Object>) ((Map<String, Object>) verifiedClaims.get("verification")).get("assurance_process")).get("assurance_details")).size());
        }
        if (expectedClaims.expectedType != null) {
            Assert.assertEquals(expectedClaims.expectedType,
                    ((Map<String, Object>) ((List) ((Map<String, Object>) verifiedClaims.get("verification")).get("evidence")).get(0)).get("type"));
        }
        if (expectedClaims.expectedOrganization != null) {
            Assert.assertEquals(expectedClaims.expectedOrganization,
                    ((Map<String, Object>) ((List) ((Map<String, Object>) ((List) ((Map<String, Object>) verifiedClaims.get("verification")).get("evidence")).get(0)).get("check_details")).get(0)).get("organization"));
        }

    }

    private ProtocolMappersResource setIdaProtocolMapper(String isIdTokenTrue, String isAccessTokenTrue, String isUserinfoTrue) {
        ProtocolMapperRepresentation idaProtocolMapper = ModelToRepresentation.toRepresentation(createClaimMapper("ida"));
        Map<String, String> config = new HashMap<>();
        config.put(IdaHttpConnector.IDA_EXTERNAL_STORE_NAME, TestApplicationResourceUrls.getVerifiedClaimsUri());
        if (isIdTokenTrue != null) {
            config.put("id.token.claim", isIdTokenTrue);
        }
        if (isAccessTokenTrue != null) {
            config.put("access.token.claim", isAccessTokenTrue);
        }
        if (isUserinfoTrue != null) {
            config.put("userinfo.token.claim", isUserinfoTrue);
        }
        idaProtocolMapper.setConfig(config);
        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(idaProtocolMapper));
        return protocolMappers;
    }

    private void deleteIdaProtocolMapper(ProtocolMappersResource protocolMappers) {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtil.getMapperByNameAndProtocol(protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "ida");
        if (mapper != null) {
            protocolMappers.delete(mapper.getId());
        }
    }

    private static ProtocolMapperModel createClaimMapper(String name) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(IDA_PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(Collections.emptyMap());
        return mapper;
    }

    private void setRequestClaims(String idTokenVerification, String idTokenClaims, String userInfoVerification, String userInfoClaims) throws IOException {
        ClaimsRepresentation claimsRepresentation = new ClaimsRepresentation();

        // Set idtoken claims
        Map<String, ClaimsRepresentation.ClaimValue> idTokenVerifiedClaims = new HashMap<>();
        ClaimsRepresentation.ClaimValue idTokenClaimValue = new ClaimsRepresentation.ClaimValue();

        if (idTokenVerification != null) {
            idTokenClaimValue.setVerification(JsonSerialization.readValue(idTokenVerification, Map.class));
        }
        if (idTokenClaims != null) {
            idTokenClaimValue.setClaims(JsonSerialization.readValue(idTokenClaims, Map.class));
        }
        idTokenVerifiedClaims.put("verified_claims", idTokenClaimValue);
        claimsRepresentation.setIdTokenClaims(idTokenVerifiedClaims);

        // Set userinfo claims
        Map<String, ClaimsRepresentation.ClaimValue> userInfoVerifiedClaims = new HashMap<>();
        ClaimsRepresentation.ClaimValue userInfoClaimValue = new ClaimsRepresentation.ClaimValue();
        if (userInfoVerification != null) {
            userInfoClaimValue.setVerification(JsonSerialization.readValue(userInfoVerification, Map.class));
        }
        if (userInfoClaims != null) {
            userInfoClaimValue.setClaims(JsonSerialization.readValue(userInfoClaims, Map.class));
        }
        userInfoVerifiedClaims.put("verified_claims", userInfoClaimValue);
        claimsRepresentation.setUserinfoClaims(userInfoVerifiedClaims);

        oauth.claims(claimsRepresentation);
    }

    private OAuthClient.AccessTokenResponse browserLogin(String clientSecret, String username, String password) {
        OAuthClient.AuthorizationEndpointResponse authsEndpointResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(authsEndpointResponse.getCode(), clientSecret);
    }

    private static class ExpectedClaims {
        private String expectedGivenName = null;
        private String expectedTrustFramework = null;
        private String expectedFamilyName = null;
        private String expectedTime = null;
        private String expectedLocality = null;
        private Integer expectedAssuranceDetailsSize = null;
        private String expectedType = null;
        private String expectedOrganization = null;

        private String expectedBirthdate = null;

        private String expectedPolicy = null;

        public void setExpectedGivenName(String expectedGivenName) {
            this.expectedGivenName = expectedGivenName;
        }

        public void setExpectedTrustFramework(String expectedTrustFramework) {
            this.expectedTrustFramework = expectedTrustFramework;
        }

        public void setExpectedTime(String expectedTime) {
            this.expectedTime = expectedTime;
        }

        public void setExpectedLocality(String expectedLocality) {
            this.expectedLocality = expectedLocality;
        }

        public void setExpectedAssuranceDetailsSize(Integer expectedAssuranceDetailsSize) {
            this.expectedAssuranceDetailsSize = expectedAssuranceDetailsSize;
        }

        public void setExpectedType(String expectedType) {
            this.expectedType = expectedType;
        }

        public void setExpectedOrganization(String expectedOrganization) {
            this.expectedOrganization = expectedOrganization;
        }

        public void setExpectedBirthdate(String expectedBirthdate) {
            this.expectedBirthdate = expectedBirthdate;
        }

        public void setExpectedPolicy(String expectedPolicy) {
            this.expectedPolicy = expectedPolicy;
        }
    }
}
