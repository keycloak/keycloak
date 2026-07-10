package org.keycloak.authentication.requiredactions;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.TrustedDeviceCredentialProvider;
import org.keycloak.credential.TrustedDeviceCredentialProviderFactory;
import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.TrustedDeviceToken;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.DateTimeFormatterUtil;

public class TrustedDeviceRegister implements RequiredActionProvider, RequiredActionFactory, CredentialRegistrator {
    public static final String PROVIDER_ID = "trust_device";
    public static final String CONF_DURATION = "duration";
    private static final String AUTH_NOTE_TRUSTED_DEVICE_NAME = "trusted-device-name";

    @Override
    public String getDisplayText() {
        return "Manage Trusted Device";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> config = new ArrayList<>(RequiredActionFactory.MAX_AUTH_AGE_CONFIG_PROPERTIES);

        config.addAll(ProviderConfigurationBuilder.create()
                .property()
                .name(CONF_DURATION)
                .label("Trust duration (days)")
                .helpText("Duration the device will be trusted in number of days. After this period, the user will be prompted again to trust the device.")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(7)
                .add()
                .build());

        return config;
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        DeviceRepresentation deviceRepresentation = context
                .getSession()
                .getProvider(DeviceRepresentationProvider.class)
                .deviceRepresentation();
        String deviceName = createDeviceName(deviceRepresentation);
        context.getAuthenticationSession().setAuthNote(AUTH_NOTE_TRUSTED_DEVICE_NAME, deviceName);

        Response form = context.form()
                .setAttribute("deviceName", deviceName)
                .createForm("trusted-device-register.ftl");
        context.challenge(form);
    }

    private String createDeviceName(DeviceRepresentation deviceRep) {
        StringBuilder deviceName = new StringBuilder();

        // Handle OS (with "Unknown" check like Keycloak does)
        String os = deviceRep.getOs();
        if (os == null || os.toLowerCase().contains("unknown")) {
            deviceName.append("Unknown Operating System");
        } else {
            deviceName.append(os);
        }

        // Add OS version if not unknown
        String osVersion = deviceRep.getOsVersion();
        if (!osVersion.toLowerCase().contains("unknown")) {
            deviceName.append(" ").append(osVersion);
        }

        // Add separator and browser
        deviceName.append(" / ");

        String browser = deviceRep.getBrowser();
        deviceName.append(Objects.requireNonNullElse(browser, "Unknown Browser"));

        return deviceName.toString();
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formParameters = context.getHttpRequest()
                .getDecodedFormParameters();
        EventBuilder event = context.getEvent();

        boolean trustedDevice = "yes".equals(formParameters.getFirst("trusted-device"));

        if (trustedDevice) {
            event.event(EventType.UPDATE_CREDENTIAL);
            event.detail(Details.CREDENTIAL_TYPE, TrustedDeviceCredentialModel.TYPE);
            String deviceName = context.getAuthenticationSession().getAuthNote(AUTH_NOTE_TRUSTED_DEVICE_NAME);
            if (deviceName == null) {
                // Fallback to creating the device name again
                DeviceRepresentation deviceRepresentation = context
                        .getSession()
                        .getProvider(DeviceRepresentationProvider.class)
                        .deviceRepresentation();
                deviceName = createDeviceName(deviceRepresentation);
            }
            int durationInDays = Integer.parseInt(context.getConfig().getConfigValue(CONF_DURATION, "7"));
            int durationInSeconds = durationInDays * 24 * 60 * 60;

            TrustedDeviceToken token = createDeviceToken(context, deviceName, durationInSeconds);

            addCookie(context.getSession(), token, durationInSeconds);
        } else {
            event.event(EventType.UPDATE_CREDENTIAL_ERROR);
            event.detail(Details.CREDENTIAL_TYPE, TrustedDeviceCredentialModel.TYPE);
            event.detail(Details.REASON, "user_declined");
        }

        context.success();
    }

    private TrustedDeviceToken createDeviceToken(RequiredActionContext context, String deviceName, int duration) {
        UserModel user = context.getUser();
        // Generate a random 32 byte deviceId
        String deviceId = Base64Url.encode(SecretGenerator.getInstance().randomBytes(SecretGenerator.SECRET_LENGTH_256_BITS));
        TrustedDeviceCredentialProvider trustedDeviceCredentialProvider = (TrustedDeviceCredentialProvider) context.getSession().getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.PROVIDER_ID);

        long exp = Time.currentTime() + duration;
        Locale userLocale = context.getSession().getProvider(LocaleSelectorProvider.class).resolveLocale(context.getRealm(), user);
        String credentialName = String.format("%s (Expires: %s)", deviceName,
                DateTimeFormatterUtil.getDateTimeFromMillis(exp * 1000L, userLocale));

        TrustedDeviceCredentialModel trustedDeviceCredentialModel = TrustedDeviceCredentialModel.create(
                credentialName, deviceId, exp);

        CredentialModel credential = trustedDeviceCredentialProvider.createCredential(context.getRealm(), user, trustedDeviceCredentialModel);
        user.credentialManager().moveStoredCredentialTo(credential.getId(), null);
        TrustedDeviceToken token = new TrustedDeviceToken(credential.getId(), deviceId, exp);
        token.issuer(context.getUriInfo().getBaseUri().toString());
        token.subject(user.getId());

        return token;
    }

    private void addCookie(KeycloakSession session, TrustedDeviceToken token, int maxAge) {
        String encodedToken = session.tokens().encode(token);
        session.getProvider(CookieProvider.class).set(
                CookieType.TRUSTED_DEVICE,
                encodedToken,
                maxAge
        );
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        return TrustedDeviceCredentialModel.TYPE;
    }
}
