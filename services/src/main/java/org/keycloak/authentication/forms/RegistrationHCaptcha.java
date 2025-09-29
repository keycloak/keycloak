package org.keycloak.authentication.forms;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

public class RegistrationHCaptcha extends AbstractRegistrationRecaptcha {

    private static final Logger LOGGER = Logger.getLogger(RegistrationHCaptcha.class);

    public static final String PROVIDER_ID = "registration-hcaptcha-action";
    public static final String SECRET_KEY = "secret.key";

    @Override
    public String getDisplayType() {
        return "hCaptcha";
    }

    @Override
    public String getHelpText() {
        return "Adds hCaptcha to the form.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected boolean validateConfig(Map<String, String> config) {
        return !StringUtil.isNullOrEmpty(config.get(SITE_KEY))
                && !StringUtil.isNullOrEmpty(config.get(SECRET_KEY));
    }

    @Override
    protected boolean validate(ValidationContext context, String captcha, Map<String, String> config) {
        LOGGER.trace("Verifying hCaptcha token");
        CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();

        HttpPost post = new HttpPost("https://hcaptcha.com/siteverify");
        List<NameValuePair> formparams = new LinkedList<>();
        formparams.add(new BasicNameValuePair("secret", config.get(SECRET_KEY)));
        formparams.add(new BasicNameValuePair("response", captcha));
        if (context.getConnection().getRemoteAddr() != null) {
            formparams.add(new BasicNameValuePair("remoteip", context.getConnection().getRemoteAddr()));
        }

        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8);
            post.setEntity(form);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                InputStream content = response.getEntity().getContent();
                try {
                    Map json = JsonSerialization.readValue(content, Map.class);
                    return Boolean.TRUE.equals(json.get("success"));
                } finally {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        return false;
    }

    @Override
    protected String getScriptUrl(Map<String, String> config, String userLanguageTag) {
        return "https://hcaptcha.com/1/api.js?hl=" + userLanguageTag;
    }

    @Override
    protected String resolveAction(Map<String, String> config) {
        return null;
    }

    @Override
    protected boolean isInvisible(Map<String, String> config) {
        return false;
    }

    @Override
    protected String getResponseFieldName() {
        return H_CAPTCHA_RESPONSE;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(SITE_KEY)
                .label("hCaptcha Site Key")
                .helpText("The site key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SECRET_KEY)
                .label("hCaptcha Secret")
                .helpText("The secret key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(true)
                .add()
                .build();
    }
}
