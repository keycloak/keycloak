package org.keycloak.protocol.saml;

import org.junit.Test;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;

import javax.ws.rs.core.MultivaluedHashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.keycloak.protocol.saml.SamlSingleSignOnUrlUtils.createSingleSignOnServiceUrl;

public class SamlSingleSignOnUrlUtilsTest {
    private static final String SAML_SSO_URL = "https://whatever.adfs.net/adfs/test";

    @Test(expected = IllegalArgumentException.class)
    public void createSingleSignOnServiceUrlWithoutConfigurationThrowsAnException() {
        createSingleSignOnServiceUrl(null, loginHint("test@test.com"));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintEnabledWithoutHint() {
        SAMLIdentityProviderConfig configuration = withHintEnabled();
        String url = createSingleSignOnServiceUrl(configuration, noLoginHint());
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintEnabledWithEmptyHint() {
        SAMLIdentityProviderConfig configuration = withHintEnabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("    "));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintEnabledWithValidHint() {
        SAMLIdentityProviderConfig configuration = withHintEnabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("  a-valid@email.com    "));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl()+"?login_hint=a-valid%40email.com&username=a-valid%40email.com")));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintEnabledWithValidHintAndExistingHintInUrl() {
        SAMLIdentityProviderConfig configuration = withExistingLoginHintAndHintEnabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("  a-valid@email.com    "));
        assertThat(url, is(equalTo(SAML_SSO_URL+"?login_hint=a-valid%40email.com&username=a-valid%40email.com")));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintEnabledWithInvalidHint() {
        SAMLIdentityProviderConfig configuration = withHintEnabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("a-invalidŒ@email.com"));
        assertThat(url, is(equalTo(SAML_SSO_URL+"?login_hint=a-invalid%C5%92%40email.com&username=a-invalid%C5%92%40email.com")));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintEnabledWithUnencodedCharacters() {
        SAMLIdentityProviderConfig configuration = withHintEnabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("with-unencoded?=@characters&.com"));
        assertThat(url, is(equalTo(SAML_SSO_URL+"?login_hint=with-unencoded%3D%40characters%26.com&username=with-unencoded%3D%40characters%26.com")));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintDisabledWithoutHint() {
        SAMLIdentityProviderConfig configuration = withHintDisabled();
        String url = createSingleSignOnServiceUrl(configuration, noLoginHint());
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintDisabledWithEmptyHint() {
        SAMLIdentityProviderConfig configuration = withHintDisabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("    "));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintDisabledWithValidHint() {
        SAMLIdentityProviderConfig configuration = withHintDisabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("  a-valid@email.com    "));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintDisabledWithValidHintAndExistingHintInUrl() {
        SAMLIdentityProviderConfig configuration = withExistingLoginHintAndHintDisabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("  a-valid@email.com    "));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintDisabledWithInvalidHint() {
        SAMLIdentityProviderConfig configuration = withHintDisabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("a-invalidŒ@email.com"));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    @Test
    public void createSingleSignOnServiceUrlWithLoginHintDisabledWithUnencodedCharacters() {
        SAMLIdentityProviderConfig configuration = withHintDisabled();
        String url = createSingleSignOnServiceUrl(configuration, loginHint("with-unencoded?=@characters&.com"));
        assertThat(url, is(equalTo(configuration.getSingleSignOnServiceUrl())));
    }

    private SAMLIdentityProviderConfig withHintDisabled() {
        SAMLIdentityProviderConfig configuration = new SAMLIdentityProviderConfig();
        configuration.setLoginHint(false);
        configuration.setSingleSignOnServiceUrl(SAML_SSO_URL);
        return configuration;
    }

    private SAMLIdentityProviderConfig withHintEnabled() {
        SAMLIdentityProviderConfig configuration = new SAMLIdentityProviderConfig();
        configuration.setLoginHint(true);
        configuration.setSingleSignOnServiceUrl(SAML_SSO_URL);
        return configuration;
    }

    private SAMLIdentityProviderConfig withExistingLoginHintAndHintEnabled() {
        SAMLIdentityProviderConfig configuration = new SAMLIdentityProviderConfig();
        configuration.setLoginHint(true);
        configuration.setSingleSignOnServiceUrl(SAML_SSO_URL+"?login_hint=another@email.com&username=another@email.com");
        return configuration;
    }

    private SAMLIdentityProviderConfig withExistingLoginHintAndHintDisabled() {
        SAMLIdentityProviderConfig configuration = new SAMLIdentityProviderConfig();
        configuration.setLoginHint(false);
        configuration.setSingleSignOnServiceUrl(SAML_SSO_URL+"?login_hint=another@email.com&username=another@email.com");
        return configuration;
    }

    private MultivaluedHashMap noLoginHint() {
        MultivaluedHashMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.putSingle("not_an_hint", "nothing");
        return queryParameters;
    }

    private MultivaluedHashMap<String, String> loginHint(String hint) {
        MultivaluedHashMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.putSingle("login_hint", hint);
        return queryParameters;
    }
}