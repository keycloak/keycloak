// ID token claims for the OpenID Connect conformance tests. The suite warns about claims in the ID token that the
// client did not request or that it does not know. Script mappers run after the attribute and property mappers.
//
// Keycloak always adds typ to the ID token.
token.type(null);

// OpenID Connect Core 5.4: the claims of the email scope are returned in the ID token only when no access token is
// issued, i.e. for response_type=id_token, and from the userinfo endpoint otherwise. A claim the client explicitly
// requested in the ID token with the claims parameter (OpenID Connect Core 5.5) stays in it.
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
