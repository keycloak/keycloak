export default {
  "clients-help": {
    enableDisable:
      "Disabled clients cannot initiate a login or have obtained access tokens.",
    clientType:
      "'OpenID Connect' allows Clients to verify the identity of the End-User based on the authentication performed by an Authorization Server.'SAML' enables web-based authentication and authorization scenarios including cross-domain single sign-on (SSO) and uses security tokens containing assertions to pass information.",
    serviceAccount:
      "Allows you to authenticate this client to Keycloak and retrieve access token dedicated to this client. In terms of OAuth2 specification, this enables support of 'Client Credentials Grant' for this client.",
    authentication:
      "This defines the type of the OIDC client. When it's ON, the OIDC type is set to confidential access type. When it's OFF, it is set to public access type",
    authorization:
      "Enable/Disable fine-grained authorization support for a client",
    directAccess:
      "This enables support for Direct Access Grants, which means that client has access to username/password of user and exchange it directly with Keycloak server for access token. In terms of OAuth2 specification, this enables support of 'Resource Owner Password Credentials Grant' for this client.",
    standardFlow:
      "This enables standard OpenID Connect redirect based authentication with authorization code. In terms of OpenID Connect or OAuth2 specifications, this enables support of 'Authorization Code Flow' for this client.",
    implicitFlow:
      "This enables support for OpenID Connect redirect based authentication without authorization code. In terms of OpenID Connect or OAuth2 specifications, this enables support of 'Implicit Flow' for this client.",
    rootURL: "Root URL appended to relative URLs",
    validRedirectURIs:
      "Valid URI pattern a browser can redirect to after a successful login or logout. Simple wildcards are allowed such as 'http://example.com/*'. Relative path can be specified too such as /my/relative/path/*. Relative paths are relative to the client root URL, or if none is specified the auth server root URL is used. For SAML, you must set valid URI patterns if you are relying on the consumer service URL embedded with the login request.",
    nameIdFormat: "The name ID format to use for the subject.",
    forceNameIdFormat:
      "Ignore requested NameID subject format and use admin console configured one.",
    forcePostBinding: "Always use POST binding for responses.",
    forceArtifactBinding:
      "Should response messages be returned to the client through the SAML ARTIFACT binding system?",
    includeAuthnStatement:
      "Should a statement specifying the method and timestamp be included in login responses?",
    includeOneTimeUseCondition:
      "Should a OneTimeUse Condition be included in login responses?",
    optimizeLookup:
      "When signing SAML documents in REDIRECT binding for SP that is secured by Keycloak adapter, should the ID of the signing key be included in SAML protocol message in <Extensions> element? This optimizes validation of the signature as the validating party uses a single key instead of trying every known key for validation.",
    signDocuments: "Should SAML documents be signed by the realm?",
    signAssertions:
      "Should assertions inside SAML documents be signed? This setting is not needed if document is already being signed.",
    signatureAlgorithm: "The signature algorithm to use to sign documents.",
    signatureKeyName:
      "Signed SAML documents contain identification of signing key in KeyName element. For Keycloak / RH-SSO counterparty, use KEY_ID, for MS AD FS use CERT_SUBJECT, for others check and use NONE if no other option works.",
    canonicalization: "Canonicalization Method for XML signatures.",
    webOrigins:
      "Allowed CORS origins. To permit all origins of Valid Redirect URIs, add '+'. This does not include the '*' wildcard though. To permit all origins, explicitly add '*'.",
    homeURL:
      "Default URL to use when the auth server needs to redirect or link back to the client.",
    adminURL:
      "URL to the admin interface of the client. Set this if the client supports the adapter REST API. This REST API allows the auth server to push revocation policies and other administrative tasks. Usually this is set to the base URL of the client.",
    clientId:
      "Specifies ID referenced in URI and tokens. For example 'my-client'. For SAML this is also the expected issuer value from authn requests",
    clientName:
      "Specifies display name of the client. For example 'My Client'. Supports keys for localized values as well. For example: ${my_client}",
    description:
      "Specifies description of the client. For example 'My Client for TimeSheets'. Supports keys for localized values as well. For example: ${my_client_description}",
    loginTheme:
      "Select theme for login, OTP, grant, registration, and forgot password pages.",
    encryptAssertions:
      "Should SAML assertions be encrypted with client's public key using AES?",
    clientSignature:
      "Will the client sign their saml requests and responses? And should they be validated?",
    downloadType: "this is information about the download type",
    details: "this is information about the details",
    createToken: "An initial access token can only be used to create clients",
    expiration: "Specifies how long the token should be valid",
    count: "Specifies how many clients can be created using the token",
    "client-authenticator-type":
      "Client Authenticator used for authentication of this client against Keycloak server",
    "registration-access-token":
      "The registration access token provides access for clients to the client registration service.",
    "signature-algorithm":
      "JWA algorithm, which the client needs to use when signing a JWT for authentication. If left blank, the client is allowed to use any algorithm.",
    subject:
      'A regular expression for validating Subject DN in the Client Certificate. Use "(.*?)(?:$)" to match all kind of expressions.',
    evaluateExplain:
      "This page allows you to see all protocol mappers and role scope mappings",
    effectiveProtocolMappers:
      "Contains all default client scopes and selected optional scopes. All protocol mappers and role scope mappings of all those client scopes will be used when generating access token issued for your client",
    effectiveRoleScopeMappings:
      "Selected Optional Client Scopes, which will be used when issuing access token for this client. You can see above what value of OAuth Scope Parameter needs to be used when you want to have these optional client scopes applied when the initial OpenID Connect Authentication request will be sent from your client adapter",
    generatedAccessToken: "Example access token",
    scopeParameter:
      "You can copy/paste this value of scope parameter and use it in initial OpenID Connect Authentication Request sent from this client adapter. Default client scopes and selected optional client scopes will be used when generating token issued for this client",
    user: "Optionally select user, for whom the example access token will be generated. If you do not select a user, example access token will not be generated during evaluation",
    notBefore:
      "Revoke any tokens issued before this time for this client. To push the policy, you should set an effective admin URL in the Settings tab first.",
    notBeforeIntro:
      "In order to successfully push a revocation policy to the client, you need to set an Admin URL under the <1>Settings</1> tab for this client first",
    notBeforeTooltip: "The admin URL should be set in the Settings tab first.",
    nodeReRegistrationTimeout:
      "Interval to specify max time for registered clients cluster nodes to re-register. If cluster node will not send re-registration request to Keycloak within this time, it will be unregistered from Keycloak",
    fineGrainOpenIdConnectConfiguration:
      "This section is used to configure advanced settings of this client related to OpenID Connect protocol.",
    fineGrainSamlEndpointConfig:
      "This section to configure exact URLs for Assertion Consumer and Single Logout Service.",
    accessTokenSignatureAlgorithm:
      "JWA algorithm used for signing access tokens.",
    idTokenSignatureAlgorithm: "JWA algorithm used for signing ID tokens.",
    idTokenEncryptionKeyManagementAlgorithm:
      "JWA Algorithm used for key management in encrypting ID tokens. This option is needed if you want encrypted ID tokens. If left empty, ID Tokens are just signed, but not encrypted.",
    idTokenEncryptionContentEncryptionAlgorithm:
      "JWA Algorithm used for content encryption in encrypting ID tokens. This option is needed just if you want encrypted ID tokens. If left empty, ID Tokens are just signed, but not encrypted.",
    userInfoSignedResponseAlgorithm:
      "JWA algorithm used for signed User Info Endpoint response. If set to 'unsigned', User Info Response won't be signed and will be returned in application/json format.",
    requestObjectSignatureAlgorithm:
      "JWA algorithm, which client needs to use when sending OIDC request object specified by 'request' or 'request_uri' parameters. If set to 'any', Request object can be signed by any algorithm (including 'none' ).",
    requestObjectRequired:
      'Specifies if the client needs to provide a request object with their authorization requests, and what method they can use for this. If set to "not required", providing a request object is optional. In all other cases, providing a request object is mandatory. If set to "request", the request object must be provided by value. If set to "request_uri", the request object must be provided by reference. If set to "request or request_uri", either method can be used.',
    openIdConnectCompatibilityModes:
      "This section is used to configure settings for backward compatibility with older OpenID Connect / OAuth 2 adaptors. It's useful especially if your client uses older version of Keycloak / RH-SSO adapter.",
    excludeSessionStateFromAuthenticationResponse:
      "If this is on, the parameter 'session_state' will not be included in OpenID Connect Authentication Response. It is useful if your client uses older OIDC / OAuth2 adapter, which does not support 'session_state' parameter.",
    "advancedSettingsOpenid-connect":
      "This section is used to configure advanced settings of this client related to OpenID Connect protocol",
    advancedSettingsSaml:
      "This section is used to configure advanced settings of this client",
    assertionLifespan:
      'Lifespan set in the SAML assertion conditions. After that time the assertion will be invalid. The "SessionNotOnOrAfter" attribute is not modified and continue using the "SSO Session Max" time defined at realm level.',
    accessTokenLifespan:
      "Max time before an access token is expired. This value is recommended to be short relative to the SSO timeout.",
    oAuthMutual:
      "This enables support for OAuth 2.0 Mutual TLS Certificate Bound Access Tokens, which means that keycloak bind an access token and a refresh token with a X.509 certificate of a token requesting client exchanged in mutual TLS between keycloak's Token Endpoint and this client. These tokens can be treated as Holder-of-Key tokens instead of bearer tokens.",
    keyForCodeExchange:
      "Choose which code challenge method for PKCE is used. If not specified, keycloak does not applies PKCE to a client unless the client sends an authorization request with appropriate code challenge and code exchange method.",
    assertionConsumerServicePostBindingURL:
      "SAML POST Binding URL for the client's assertion consumer service (login responses). You can leave this blank if you do not have a URL for this binding.",
    assertionConsumerServiceRedirectBindingURL:
      "SAML Redirect Binding URL for the client's assertion consumer service (login responses). You can leave this blank if you do not have a URL for this binding.",
    logoutServicePostBindingURL:
      "SAML POST Binding URL for the client's single logout service. You can leave this blank if you are using a different binding",
    logoutServiceRedirectBindingURL:
      "SAML Redirect Binding URL for the client's single logout service. You can leave this blank if you are using a different binding.",
    logoutServiceArtifactBindingUrl:
      "SAML ARTIFACT Binding URL for the client's single logout service. You can leave this blank if you are using a different binding.",
    artifactBindingUrl:
      "URL to send the HTTP ARTIFACT messages to. You can leave this blank if you are using a different binding. This value should be set when forcing ARTIFACT binding together with IdP initiated login.",
    artifactResolutionService:
      "SAML Artifact resolution service for the client. This is the endpoint to which Keycloak will send a SOAP ArtifactResolve message. You can leave this blank if you do not have a URL for this binding.",
    authenticationOverrides: "Override realm authentication flow bindings.",
    browserFlow: "Select the flow you want to use for browser authentication.",
    directGrant:
      "Select the flow you want to use for direct grant authentication.",
    useJwksUrl:
      "If the switch is on, client public keys will be downloaded from given JWKS URL. This allows great flexibility because new keys will be always re-downloaded again when client generates new keypair. If the switch is off, public key (or certificate) from the Keycloak DB is used, so when client keypair changes, you always need to import new key (or certificate) to the Keycloak DB as well.",
    certificate:
      "Client Certificate for validate JWT issued by client and signed by Client private key from your keystore.",
    jwksUrl:
      "URL where client keys in JWK format are stored. See JWK specification for more details. If you use Keycloak client adapter with \"jwt\" credential, you can use URL of your app with '/k_jwks' suffix. For example 'http://www.myhost.com/myapp/k_jwks' .",
    generateKeysDescription:
      "If you generate new keys, you can download the keystore with the private key automatically and save it on your client's side. Keycloak server will save just the certificate and public key, but not the private key.",
    archiveFormat: "Java keystore or PKCS12 archive format.",
    keyAlias: "Archive alias for your private key and certificate.",
    keyPassword: "Password to access the private key in the archive",
    storePassword: "Password to access the archive itself",
    consentRequired: "If enabled, users have to consent to client access.",
    displayOnClient:
      "Applicable only if 'Consent Required' is on for this client. If this switch is off, the consent screen will contain just the consents corresponding to configured client scopes. If on, there will be also one item on the consent screen about this client itself.",
    consentScreenText:
      "Applicable only if 'Display Client On Consent Screen' is on for this client. Contains the text which will be on the consent screen about permissions specific just for this client.",
    import:
      "Import a JSON file containing authorization settings for this resource server.",
    policyEnforcementMode:
      "The policy enforcement mode dictates how policies are enforced when evaluating authorization requests. 'Enforcing' means requests are denied by default even when there is no policy associated with a given resource. 'Permissive' means requests are allowed even when there is no policy associated with a given resource. 'Disabled' completely disables the evaluation of policies and allows access to any resource.",
    decisionStrategy:
      "The decision strategy dictates how permissions are evaluated and how a final decision is obtained. 'Affirmative' means that at least one permission must evaluate to a positive decision in order to grant access to a resource and its scopes. 'Unanimous' means that all permissions must evaluate to a positive decision in order for the final decision to be also positive.",
    allowRemoteResourceManagement:
      "Should resources be managed remotely by the resource server? If false, resources can be managed only from this admin console.",
    resourceName:
      "A unique name for this resource. The name can be used to uniquely identify a resource, useful when querying for a specific resource.",
    displayName:
      "A unique name for this resource. The name can be used to uniquely identify a resource, useful when querying for a specific resource.",
    type: "The type of this resource. It can be used to group different resource instances with the same type.",
    uris: "Set of URIs which are protected by resource.",
    scopes: "The scopes associated with this resource.",
    iconUri: "A URI pointing to an icon.",
    ownerManagedAccess:
      "If enabled, the access to this resource can be managed by the resource owner.",
    resourceAttribute: "The attributes associated wth the resource.",
    resetActions:
      "Set of actions to execute when sending the user a Reset Actions Email. 'Verify email' sends an email to the user to verify their email address. 'Update profile' requires user to enter in new personal information. 'Update password' requires user to enter in a new password. 'Configure OTP' requires setup of a mobile password generator.",
    lifespan: "Maximum time before the action permit expires.",
    scopeName:
      "A unique name for this scope. The name can be used to uniquely identify a scope, useful when querying for a specific scope.",
    scopeDisplayName:
      "A unique name for this scope. The name can be used to uniquely identify a scope, useful when querying for a specific scope.",
  },
};
