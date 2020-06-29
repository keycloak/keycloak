package org.keycloak.models.credential;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class CredentialModelTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void canCreateDefaultCredentialModel() {
        PasswordCredentialModel model = PasswordCredentialModel.createFromValues("pbkdf2", new byte[32], 1000, "secretValue");

        assertThat(model.getPasswordCredentialData(), notNullValue());
        assertThat(model.getPasswordCredentialData().getAlgorithm(), equalTo("pbkdf2"));
        assertThat(model.getPasswordCredentialData().getHashIterations(), equalTo(1000));
        assertThat(model.getPasswordCredentialData().getAlgorithmData(), equalTo(Collections.emptyMap()));

        assertThat(model.getPasswordSecretData(), notNullValue());
        assertThat(model.getPasswordSecretData().getAlgorithmData(), equalTo(Collections.emptyMap()));
        assertThat(model.getPasswordSecretData().getValue(), equalTo("secretValue"));
        assertThat(Arrays.equals(model.getPasswordSecretData().getSalt(), new byte[32]), is(true));
    }

    @Test
    public void canCreatedExtendedCredentialModel() throws IOException {
        PasswordCredentialData credentialData = new PasswordCredentialData(1000, "bcrypt", Collections.singletonMap("cost", "18"));
        PasswordSecretData secretData = new PasswordSecretData("secretValue", "AAAAAAAAAAAAAAAA", Collections.singletonMap("salt2", "BBBBBBBBBBBBBBBB"));
        PasswordCredentialModel model = new PasswordCredentialModel(credentialData, secretData);

        assertThat(model.getPasswordCredentialData(), notNullValue());
        assertThat(model.getPasswordCredentialData().getAlgorithm(), equalTo("bcrypt"));
        assertThat(model.getPasswordCredentialData().getHashIterations(), equalTo(1000));
        assertThat(model.getPasswordCredentialData().getAlgorithmData(), equalTo(Collections.singletonMap("cost", "18")));

        assertThat(model.getPasswordSecretData(), notNullValue());
        assertThat(model.getPasswordSecretData().getAlgorithmData(), equalTo(Collections.singletonMap("salt2", "BBBBBBBBBBBBBBBB")));
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

        assertThat(pcdRoundtrip.getAlgorithmData(), equalTo(pcdOriginal.getAlgorithmData()));
        assertThat(pcdRoundtrip.getAlgorithm(), equalTo(pcdOriginal.getAlgorithm()));
        assertThat(pcdRoundtrip.getHashIterations(), equalTo(pcdOriginal.getHashIterations()));

        PasswordSecretData psdOriginal = model.getPasswordSecretData();
        PasswordSecretData psdRoundtrip = mapper.readValue(mapper.writeValueAsString(psdOriginal), PasswordSecretData.class);

        assertThat(psdRoundtrip.getValue(), equalTo(psdOriginal.getValue()));
        assertThat(psdRoundtrip.getSalt(), equalTo(psdOriginal.getSalt()));
        assertThat(psdRoundtrip.getAlgorithmData(), equalTo(psdRoundtrip.getAlgorithmData()));
    }

    @Test
    public void roudtripToJsonExtendedCredentialModel() throws IOException {
        PasswordCredentialData credentialData = new PasswordCredentialData(1000, "bcrypt", Collections.singletonMap("cost", "18"));
        PasswordSecretData secretData = new PasswordSecretData("secretValue", "AAAAAAAAAAAAAAAA", Collections.singletonMap("salt2", "BBBBBBBBBBBBBBBB"));
        PasswordCredentialModel model = new PasswordCredentialModel(credentialData, secretData);

        roundTripAndVerify(model);
    }

}
