// ID token claims for the OpenID Connect conformance tests. The suite warns about claims in the ID token that the
// client did not request or that it does not know. Script mappers run after the attribute and property mappers.
//
// Keycloak always adds typ to the ID token.
token.type(null);

// OpenID Connect Core 5.4: the claims of the email scope are returned in the ID token only when no access token is
// issued, i.e. for response_type=id_token, and from the userinfo endpoint otherwise.
var client = realm.getClientByClientId(token.getIssuedFor());
var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
if (clientSession == null || !"id_token".equals(clientSession.getNote("response_type"))) {
    // The email mappers put the claims in the other claims of the token, not in its email fields
    token.getOtherClaims().remove("email");
    token.getOtherClaims().remove("email_verified");
}
