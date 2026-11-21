// CONFORMANCE TEST ADJUSTMENT, used only by the realms of the OpenID Connect conformance tests of this module.
//
// It adjusts what Keycloak puts in the ID token so that the OpenID Foundation conformance suite does not finish its
// modules with warnings about claims it did not request. Keycloak's behaviour is permitted by the specification, but
// the suite flags it. This is test configuration, not a recommended Keycloak setup. Script mappers run after the
// attribute and property mappers, so this sees the claims they added.

// Change 1: remove the typ claim.
// Reason: Keycloak always adds typ ("ID") to its ID tokens. OpenID Connect Core does not define this claim, and the
// suite warns about it in oidcc-server.
token.type(null);

// Change 2: remove the email and email_verified claims, unless the response type is id_token or the client requested
// the claim in the ID token with the claims parameter.
// Reason: Keycloak puts the claims of a requested scope in the ID token even when an access token is issued. OpenID
// Connect Core 5.4 returns the claims of the email scope in the ID token only when no access token is issued
// (response_type=id_token) and from the userinfo endpoint otherwise. The suite warns about them in oidcc-scope-email
// and oidcc-alternate-happy-flow. A claim the client explicitly requested in the ID token (OpenID Connect Core 5.5)
// stays in it.
var client = realm.getClientByClientId(token.getIssuedFor());
var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
if (clientSession == null || !"id_token".equals(clientSession.getNote("response_type"))) {
    var requestedInIdToken = {};
    var claimsParameter = clientSession == null ? null : clientSession.getNote("claims");
    if (claimsParameter != null) {
        try {
            var claims = JSON.parse(claimsParameter);
            if (claims != null && claims.id_token != null) {
                requestedInIdToken = claims.id_token;
            }
        } catch (e) {
            // a malformed claims parameter requests nothing; Keycloak rejects it at the authorization endpoint anyway
        }
    }
    // The email mappers put the claims in the other claims of the token, not in its email fields
    ["email", "email_verified"].forEach(function (claim) {
        if (!Object.prototype.hasOwnProperty.call(requestedInIdToken, claim)) {
            token.getOtherClaims().remove(claim);
        }
    });
}
