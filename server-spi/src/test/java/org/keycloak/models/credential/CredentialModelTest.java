package org.keycloak.models.credential;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.dto.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class CredentialModelTest {

    private static ObjectMapper mapper = new ObjectMapper();
    @BeforeClass
    public static void setup() {
        mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Test
    public void canDeserializeMinimalJson() {
        CredentialModel model = new CredentialModel();
        model.setCredentialData("{\"hashIterations\": 10000, \"algorithm\": \"custom\"}");
        model.setSecretData("{\"value\": \"the value\", \"salt\": \"saltValu\"}");

        PasswordCredentialModel decoded = PasswordCredentialModel.createFromCredentialModel(model);
        assertThat(decoded, notNullValue());
        assertThat(decoded.getPasswordCredentialData(), notNullValue());
        assertThat(decoded.getPasswordCredentialData().getAlgorithm(), equalTo("custom"));
        assertThat(decoded.getPasswordCredentialData().getHashIterations(), equalTo(10000));
        assertThat(decoded.getPasswordCredentialData().getAdditionalParameters(), equalTo(Collections.emptyMap()));

        assertThat(decoded.getPasswordSecretData(), notNullValue());
        assertThat(decoded.getPasswordSecretData().getValue(), equalTo("the value"));
        assertThat(decoded.getPasswordSecretData().getSalt(), notNullValue());
        String base64Salt = Base64.getEncoder().encodeToString(decoded.getPasswordSecretData().getSalt());
        assertThat(base64Salt, equalTo("saltValu"));
        assertThat(decoded.getPasswordSecretData().getAdditionalParameters(), equalTo(Collections.emptyMap()));

    }

    @Test
    public void canCreateDefaultCredentialModel() {
        PasswordCredentialModel model = PasswordCredentialModel.createFromValues("pbkdf2", new byte[32], 1000, "secretValue");

        assertThat(model.getPasswordCredentialData(), notNullValue());
        assertThat(model.getPasswordCredentialData().getAlgorithm(), equalTo("pbkdf2"));
        assertThat(model.getPasswordCredentialData().getHashIterations(), equalTo(1000));
        assertThat(model.getPasswordCredentialData().getAdditionalParameters(), equalTo(Collections.emptyMap()));

        assertThat(model.getPasswordSecretData(), notNullValue());
        assertThat(model.getPasswordSecretData().getAdditionalParameters(), equalTo(Collections.emptyMap()));
        assertThat(model.getPasswordSecretData().getValue(), equalTo("secretValue"));
        assertThat(Arrays.equals(model.getPasswordSecretData().getSalt(), new byte[32]), is(true));
    }

    @Test
    public void canCreatedExtendedCredentialModel() throws IOException {
        PasswordCredentialData credentialData = new PasswordCredentialData(1000, "bcrypt", Collections.singletonMap("cost", Collections.singletonList("18")));
        PasswordSecretData secretData = new PasswordSecretData("secretValue", "AAAAAAAAAAAAAAAA", Collections.singletonMap("salt2", Collections.singletonList("BBBBBBBBBBBBBBBB")));
        PasswordCredentialModel model = PasswordCredentialModel.createFromValues(credentialData, secretData);

        assertThat(model.getPasswordCredentialData(), notNullValue());
        assertThat(model.getPasswordCredentialData().getAlgorithm(), equalTo("bcrypt"));
        assertThat(model.getPasswordCredentialData().getHashIterations(), equalTo(1000));
        assertThat(model.getPasswordCredentialData().getAdditionalParameters(), equalTo(Collections.singletonMap("cost", Collections.singletonList("18"))));

        assertThat(model.getPasswordSecretData(), notNullValue());
        assertThat(model.getPasswordSecretData().getAdditionalParameters(), equalTo(Collections.singletonMap("salt2", Collections.singletonList("BBBBBBBBBBBBBBBB"))));
        assertThat(model.getPasswordSecretData().getValue(), equalTo("secretValue"));
        assertThat(Arrays.equals(model.getPasswordSecretData().getSalt(), new byte[12]), is(true));
    }

    @Test
    public void roundtripToJsonDefaultCredentialModel() throws IOException {
        PasswordCredentialModel model = PasswordCredentialModel.createFromValues("pbkdf2", new byte[32], 1000, "secretValue");
        roundTripAndVerify(model);
    }


    private void roundTripAndVerify(PasswordCredentialModel model) throws IOException {
        PasswordCredentialData pcdOriginal = model.getPasswordCredentialData();
        PasswordCredentialData pcdRoundtrip = mapper.readValue(mapper.writeValueAsString(pcdOriginal), PasswordCredentialData.class);

        assertThat(pcdRoundtrip.getAdditionalParameters(), equalTo(pcdOriginal.getAdditionalParameters()));
        assertThat(pcdRoundtrip.getAlgorithm(), equalTo(pcdOriginal.getAlgorithm()));
        assertThat(pcdRoundtrip.getHashIterations(), equalTo(pcdOriginal.getHashIterations()));

        PasswordSecretData psdOriginal = model.getPasswordSecretData();
        PasswordSecretData psdRoundtrip = mapper.readValue(mapper.writeValueAsString(psdOriginal), PasswordSecretData.class);

        assertThat(psdRoundtrip.getValue(), equalTo(psdOriginal.getValue()));
        assertThat(psdRoundtrip.getSalt(), equalTo(psdOriginal.getSalt()));
        assertThat(psdRoundtrip.getAdditionalParameters(), equalTo(psdRoundtrip.getAdditionalParameters()));
    }

    @Test
    public void roudtripToJsonExtendedCredentialModel() throws IOException {
        PasswordCredentialData credentialData = new PasswordCredentialData(1000, "bcrypt", Collections.singletonMap("cost", Collections.singletonList("18")));
        PasswordSecretData secretData = new PasswordSecretData("secretValue", "AAAAAAAAAAAAAAAA", Collections.singletonMap("salt2", Collections.singletonList("BBBBBBBBBBBBBBBB")));
        PasswordCredentialModel model = PasswordCredentialModel.createFromValues(credentialData, secretData);

        roundTripAndVerify(model);
    }

    // Test OTPCredentialModel
    @Test
    public void canDeserializeMinimalJsonOTPCredential() {
        CredentialModel model = new CredentialModel();
        model.setCredentialData("{\"subType\": \"totp\", \"digits\": 6, \"counter\": 0, \"period\": 30, \"algorithm\": \"HmacSHA1\"}");
        model.setSecretData("{\"value\": \"secretValue\"}");

        OTPCredentialModel decoded = OTPCredentialModel.createFromCredentialModel(model);
        assertThat(decoded, notNullValue());
        assertThat(decoded.getOTPCredentialData(), notNullValue());
        assertThat(decoded.getOTPCredentialData().getAlgorithm(), equalTo("HmacSHA1"));
        assertThat(decoded.getOTPCredentialData().getDigits(), equalTo(6));
        assertThat(decoded.getOTPCredentialData().getPeriod(), equalTo(30));
        assertThat(decoded.getOTPCredentialData().getCounter(), equalTo(0));

        assertThat(decoded.getOTPSecretData(), notNullValue());
        assertThat(decoded.getOTPSecretData().getValue(), equalTo("secretValue"));
    }

    @Test
    public void canCreateTOTPCredentialModel() {
        OTPCredentialModel model = OTPCredentialModel.createTOTP("secretValue", 6, 30, "HmacSHA1");

        assertThat(model.getOTPCredentialData(), notNullValue());
        assertThat(model.getOTPCredentialData().getAlgorithm(), equalTo("HmacSHA1"));
        assertThat(model.getOTPCredentialData().getDigits(), equalTo(6));
        assertThat(model.getOTPCredentialData().getPeriod(), equalTo(30));
        assertThat(model.getOTPCredentialData().getCounter(), equalTo(0));

        assertThat(model.getOTPSecretData(), notNullValue());
        assertThat(model.getOTPSecretData().getValue(), equalTo("secretValue"));
    }

    @Test
    public void canCreateHOTPCredentialModel() {
        OTPCredentialModel model = OTPCredentialModel.createHOTP("secretValue", 6, 1, "HmacSHA1");

        assertThat(model.getOTPCredentialData(), notNullValue());
        assertThat(model.getOTPCredentialData().getAlgorithm(), equalTo("HmacSHA1"));
        assertThat(model.getOTPCredentialData().getDigits(), equalTo(6));
        assertThat(model.getOTPCredentialData().getPeriod(), equalTo(0));
        assertThat(model.getOTPCredentialData().getCounter(), equalTo(1));

        assertThat(model.getOTPSecretData(), notNullValue());
        assertThat(model.getOTPSecretData().getValue(), equalTo("secretValue"));
    }

    @Test
    public void roundtripToJsonTOTPCredentialModel() throws IOException {
        OTPCredentialModel model = OTPCredentialModel.createTOTP("secretValue", 6, 30, "HmacSHA1");
        roundTripAndVerifyOTP(model);
    }

    @Test
    public void roundtripToJsonHOTPCredentialModel() throws IOException {
        OTPCredentialModel model = OTPCredentialModel.createHOTP("secretValue", 6, 1, "HmacSHA1");
        roundTripAndVerifyOTP(model);
    }

    private void roundTripAndVerifyOTP(OTPCredentialModel model) throws IOException {
        OTPCredentialData ocdOriginal = model.getOTPCredentialData();
        OTPCredentialData ocdRoundtrip = mapper.readValue(mapper.writeValueAsString(ocdOriginal), OTPCredentialData.class);

        assertThat(ocdRoundtrip.getAlgorithm(), equalTo(ocdOriginal.getAlgorithm()));
        assertThat(ocdRoundtrip.getDigits(), equalTo(ocdOriginal.getDigits()));
        assertThat(ocdRoundtrip.getCounter(), equalTo(ocdOriginal.getCounter()));
        assertThat(ocdRoundtrip.getPeriod(), equalTo(ocdOriginal.getPeriod()));
        assertThat(ocdRoundtrip.getSecretEncoding(), equalTo(ocdOriginal.getSecretEncoding()));

        OTPSecretData osdOriginal = model.getOTPSecretData();
        OTPSecretData osdRoundtrip = mapper.readValue(mapper.writeValueAsString(osdOriginal), OTPSecretData.class);

        assertThat(osdRoundtrip.getValue(), equalTo(osdOriginal.getValue()));
    }

    // Test WebAuthnCredentialModel
    @Test
    public void canDeserializeMinimalJsonWebAuthnCredential() throws IOException {
        CredentialModel model = new CredentialModel();
        model.setCredentialData("{\"aaguid\": \"aaguid-value\", \"credentialId\": \"credentialId-value\", \"counter\": 1234, \"attestationStatement\": \"attestationStatement-value\", \"credentialPublicKey\": \"credentialPublicKey-value\", \"attestationStatementFormat\": \"attestationStatementFormat-value\", \"transports\": []}");
        model.setSecretData("{}");

        WebAuthnCredentialModel decoded = WebAuthnCredentialModel.createFromCredentialModel(model);
        assertThat(decoded, notNullValue());
        assertThat(decoded.getWebAuthnCredentialData(), notNullValue());
        assertThat(decoded.getWebAuthnCredentialData().getAaguid(), equalTo("aaguid-value"));
        assertThat(decoded.getWebAuthnCredentialData().getCredentialId(), equalTo("credentialId-value"));
        assertThat(decoded.getWebAuthnCredentialData().getCounter(), equalTo(1234L));
        assertThat(decoded.getWebAuthnCredentialData().getAttestationStatement(), equalTo("attestationStatement-value"));
        assertThat(decoded.getWebAuthnCredentialData().getCredentialPublicKey(), equalTo("credentialPublicKey-value"));
        assertThat(decoded.getWebAuthnCredentialData().getAttestationStatementFormat(), equalTo("attestationStatementFormat-value"));
        assertThat(decoded.getWebAuthnCredentialData().getTransports(), equalTo(Collections.emptySet()));

        assertThat(decoded.getWebAuthnSecretData(), notNullValue());
    }

    @Test
    public void canCreateWebAuthnCredentialModel() {
        String credentialType = WebAuthnCredentialModel.TYPE_TWOFACTOR;
        String userLabel = "TestUserLabel";
        String aaguid = "aaguid-value";
        String credentialId = "credentialId-value";
        String attestationStatement = "attestationStatement-value";
        String credentialPublicKey = "credentialPublicKey-value";
        long counter = 1234L;
        String attestationStatementFormat = "attestationStatementFormat-value";
        Set<String> transports = Collections.emptySet();

        WebAuthnCredentialModel model = WebAuthnCredentialModel.create(credentialType, userLabel, aaguid, credentialId, attestationStatement, credentialPublicKey, counter, attestationStatementFormat, transports);

        assertThat(model.getWebAuthnCredentialData(), notNullValue());
        assertThat(model.getWebAuthnCredentialData().getAaguid(), equalTo(aaguid));
        assertThat(model.getWebAuthnCredentialData().getCredentialId(), equalTo(credentialId));
        assertThat(model.getWebAuthnCredentialData().getCounter(), equalTo(counter));
        assertThat(model.getWebAuthnCredentialData().getAttestationStatement(), equalTo(attestationStatement));
        assertThat(model.getWebAuthnCredentialData().getCredentialPublicKey(), equalTo(credentialPublicKey));
        assertThat(model.getWebAuthnCredentialData().getAttestationStatementFormat(), equalTo(attestationStatementFormat));
        assertThat(model.getWebAuthnCredentialData().getTransports(), equalTo(transports));

        assertThat(model.getWebAuthnSecretData(), notNullValue());
    }

    @Test
    public void roundtripToJsonWebAuthnCredentialModel() throws IOException {
        String credentialType = WebAuthnCredentialModel.TYPE_TWOFACTOR;
        String userLabel = "TestUserLabel";
        String aaguid = "aaguid-value";
        String credentialId = "credentialId-value";
        String attestationStatement = "attestationStatement-value";
        String credentialPublicKey = "credentialPublicKey-value";
        long counter = 1234L;
        String attestationStatementFormat = "attestationStatementFormat-value";
        Set<String> transports = Collections.emptySet();

        WebAuthnCredentialModel model = WebAuthnCredentialModel.create(credentialType, userLabel, aaguid, credentialId, attestationStatement, credentialPublicKey, counter, attestationStatementFormat, transports);
        roundTripAndVerifyWebAuthn(model);
    }

    private void roundTripAndVerifyWebAuthn(WebAuthnCredentialModel model) throws IOException {
        WebAuthnCredentialData wcdOriginal = model.getWebAuthnCredentialData();
        WebAuthnCredentialData wcdRoundtrip = mapper.readValue(mapper.writeValueAsString(wcdOriginal), WebAuthnCredentialData.class);

        assertThat(wcdRoundtrip.getAaguid(), equalTo(wcdOriginal.getAaguid()));
        assertThat(wcdRoundtrip.getCredentialId(), equalTo(wcdOriginal.getCredentialId()));
        assertThat(wcdRoundtrip.getCounter(), equalTo(wcdOriginal.getCounter()));
        assertThat(wcdRoundtrip.getAttestationStatement(), equalTo(wcdOriginal.getAttestationStatement()));
        assertThat(wcdRoundtrip.getCredentialPublicKey(), equalTo(wcdOriginal.getCredentialPublicKey()));
        assertThat(wcdRoundtrip.getAttestationStatementFormat(), equalTo(wcdOriginal.getAttestationStatementFormat()));
        assertThat(wcdRoundtrip.getTransports(), equalTo(wcdOriginal.getTransports()));

        WebAuthnSecretData wsdOriginal = model.getWebAuthnSecretData();
        WebAuthnSecretData wsdRoundtrip = mapper.readValue(mapper.writeValueAsString(wsdOriginal), WebAuthnSecretData.class);

        assertThat(wsdRoundtrip, notNullValue());
    }

    @Test
    public void canUpdateCounter() throws IOException {
        String credentialType = WebAuthnCredentialModel.TYPE_TWOFACTOR;
        String userLabel = "TestUserLabel";
        String aaguid = "aaguid-value";
        String credentialId = "credentialId-value";
        String attestationStatement = "attestationStatement-value";
        String credentialPublicKey = "credentialPublicKey-value";
        long counter = 1234L;
        String attestationStatementFormat = "attestationStatementFormat-value";
        Set<String> transports = Collections.emptySet();

        WebAuthnCredentialModel model = WebAuthnCredentialModel.create(credentialType, userLabel, aaguid, credentialId, attestationStatement, credentialPublicKey, counter, attestationStatementFormat, transports);

        long newCounter = 5678L;
        model.updateCounter(newCounter);

        assertThat(model.getWebAuthnCredentialData().getCounter(), equalTo(newCounter));
    }


}
