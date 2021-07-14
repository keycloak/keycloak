export default {
  "identity-providers-help": {
    redirectURI:
      "The redirect uri to use when configuring the identity provider.",
    alias:
      "The alias uniquely identifies an identity provider and it is also used to build the redirect uri.",
    displayName: "Friendly name for Identity Providers.",
    clientId: "The client identifier registered with the identity provider.",
    clientSecret:
      "The client secret registered with the identity provider. This field is able to obtain its value from vault, use ${vault.ID} format.",
    displayOrder:
      "Number defining the order of the providers in GUI (for example, on the Login page). The lowest number will be applied first.",
    useDiscoveryEndpoint:
      "If this setting is enabled, the discovery endpoint will be used to fetch the provider config. Keycloak can load the config from the endpoint and automatically update the config if the source has any updates",
    discoveryEndpoint:
      "Import metadata from a remote IDP discovery descriptor.",
    importConfig: "Import metadata from a downloaded IDP discovery descriptor.",
    passLoginHint: "Pass login_hint to identity provider.",
    passCurrentLocale:
      "Pass the current locale to the identity provider as a ui_locales parameter.",
    logoutUrl: "End session endpoint to use to logout user from external IDP.",
    backchannelLogout: "Does the external IDP support backchannel logout?",
    disableUserInfo:
      "Disable usage of User Info service to obtain additional user information?  Default is to use this OIDC service.",
    userInfoUrl: "The User Info Url. This is optional.",
    issuer:
      "The issuer identifier for the issuer of the response. If not provided, no validation will be performed.",
    scopes:
      "The scopes to be sent when asking for authorization. It can be a space-separated list of scopes. Defaults to 'openid'.",
    prompt:
      "Specifies whether the Authorization Server prompts the End-User for reauthentication and consent.",
    acceptsPromptNone:
      "This is just used together with Identity Provider Authenticator or when kc_idp_hint points to this identity provider. In case that client sends a request with prompt=none and user is not yet authenticated, the error will not be directly returned to client, but the request with prompt=none will be forwarded to this identity provider.",
    validateSignature:
      "Enable/disable signature validation of external IDP signatures.",
    useJwksUrl:
      "If the switch is on, identity provider public keys will be downloaded from given JWKS URL. This allows great flexibility because new keys will be always re-downloaded again when identity provider generates new keypair. If the switch is off, public key (or certificate) from the Keycloak DB is used, so when the identity provider keypair changes, you always need to import the new key to the Keycloak DB as well.",
    jwksUrl:
      "URL where identity provider keys in JWK format are stored. See JWK specification for more details. If you use external Keycloak identity provider, you can use URL like 'http://broker-keycloak:8180/auth/realms/test/protocol/openid-connect/certs' assuming your brokered Keycloak is running on 'http://broker-keycloak:8180' and its realm is 'test' .",
    allowedClockSkew:
      "Clock skew in seconds that is tolerated when validating identity provider tokens. Default value is zero.",
    forwardParameters:
      "Non OpenID Connect/OAuth standard query parameters to be forwarded to external IDP from the initial application request to Authorization Endpoint. Multiple parameters can be entered, separated by comma (,).",
    clientAuthentication:
      "The client authentication method (cfr. https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication). In case of JWT signed with private key, the realm private key is used.",
    storeTokens:
      "Enable/disable if tokens must be stored after authenticating users.",
    trustEmail:
      "If enabled, email provided by this provider is not verified even if verification is enabled for the realm.",
    accountLinkingOnly:
      "If true, users cannot log in through this provider.  They can only link to this provider.  This is useful if you don't want to allow login from the provider, but want to integrate with a provider",
    hideOnLoginPage:
      "If hidden, login with this provider is possible only if requested explicitly, for example using the 'kc_idp_hint' parameter.",
    firstBrokerLoginFlowAlias:
      "Alias of authentication flow, which is triggered after first login with this identity provider. Term 'First Login' means that no Keycloak account is currently linked to the authenticated identity provider account.",
    postBrokerLoginFlowAlias:
      'Alias of authentication flow, which is triggered after each login with this identity provider. Useful if you want additional verification of each user authenticated with this identity provider (for example OTP). Leave this to "None" if you need no any additional authenticators to be triggered after login with this identity provider. Also note that authenticator implementations must assume that user is already set in ClientSession as identity provider already set it.',
    syncMode:
      "Default sync mode for all mappers. The sync mode determines when user data will be synced using the mappers. Possible values are: 'legacy' to keep the behaviour before this option was introduced, 'import' to only import the user once during first login of the user with this identity provider, 'force' to always update the user during every login with this identity provider.",
  },
};
