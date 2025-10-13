package org.keycloak.broker.spiffe;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderShowInAccountConsole;
import org.keycloak.models.RealmModel;

import java.util.regex.Pattern;

import static org.keycloak.common.util.UriUtils.checkUrl;

public class SpiffeIdentityProviderConfig extends IdentityProviderModel {

    public static final String BUNDLE_ENDPOINT_KEY = "bundleEndpoint";
    public static final String TRUST_DOMAIN_KEY = "trustDomain";

    private static final Pattern TRUST_DOMAIN_PATTERN = Pattern.compile("spiffe://[a-z0-9.\\-_]*");

    public SpiffeIdentityProviderConfig() {
        getConfig().put(IdentityProviderModel.SHOW_IN_ACCOUNT_CONSOLE, IdentityProviderShowInAccountConsole.NEVER.name());
    }

    public SpiffeIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    @Override
    public boolean isHideOnLogin() {
        return true;
    }

    public int getAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    public String getTrustDomain() {
        return getConfig().get(TRUST_DOMAIN_KEY);
    }

    public String getBundleEndpoint() {
        return getConfig().get(BUNDLE_ENDPOINT_KEY);
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);

        String trustDomain = getTrustDomain();
        if (trustDomain == null || trustDomain.isBlank() || !TRUST_DOMAIN_PATTERN.matcher(trustDomain).matches()) {
            throw new IllegalArgumentException("Invalid SPIFFE Trust Domain format. Must match 'spiffe://...'");
        }

        checkUrl(realm.getSslRequired(), getBundleEndpoint(), BUNDLE_ENDPOINT_KEY);
    }
}
