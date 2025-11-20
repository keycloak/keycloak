package org.keycloak.authentication.authenticators.client;

import java.util.Collections;
import java.util.List;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.services.Urls;

public class FederatedJWTClientValidator extends AbstractJWTClientValidator {

    private final String expectedTokenIssuer;
    private final int allowedClockSkew;
    private final boolean reusePermitted;
    private int maximumExpirationTime = 300;

    public FederatedJWTClientValidator(ClientAuthenticationFlowContext context, SignatureValidator signatureValidator, String expectedTokenIssuer, int allowedClockSkew, boolean reusePermitted) throws Exception {
        super(context, signatureValidator, null);
        this.expectedTokenIssuer = expectedTokenIssuer;
        this.allowedClockSkew = allowedClockSkew;
        this.reusePermitted = reusePermitted;
    }

    @Override
    protected String getExpectedTokenIssuer() {
        return expectedTokenIssuer;
    }

    @Override
    protected List<String> getExpectedAudiences() {
        return Collections.singletonList(Urls.realmIssuer(context.getUriInfo().getBaseUri(), realm.getName()));
    }

    @Override
    protected boolean isMultipleAudienceAllowed() {
        return false;
    }

    @Override
    protected int getAllowedClockSkew() {
        return allowedClockSkew;
    }

    @Override
    protected int getMaximumExpirationTime() {
        return maximumExpirationTime;
    }

    public void setMaximumExpirationTime(int maximumExpirationTime) {
        this.maximumExpirationTime = maximumExpirationTime;
    }

    @Override
    protected boolean isReusePermitted() {
        return reusePermitted;
    }

    @Override
    protected String getExpectedSignatureAlgorithm() {
        return null; // TODO Hard-coded to no expected signature algorithm for now, but should be configurable
    }

    public void setExpectedClientAssertionType(String clientAssertionType) {
        this.expectedClientAssertionType = clientAssertionType;
    }
}
