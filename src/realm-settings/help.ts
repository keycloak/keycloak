export default {
  "realm-settings-help": {
    fromDisplayName: "A user-friendly name for the 'From' address (optional).",
    replyToDisplayName:
      "A user-friendly name for the 'Reply-To' address (optional).",
    envelopeFrom: "An email address used for bounces (optional).",
    password:
      "SMTP password. This field is able to obtain its value from vault, use ${vault.ID} format.",
    frontendUrl:
      "Set the frontend URL for the realm. Use in combination with the default hostname provider to override the base URL for frontend requests for a specific realm.",
    requireSsl:
      "Is HTTPS required? 'None' means HTTPS is not required for any client IP address. 'External requests' means localhost and private IP addresses can access without HTTPS. 'All requests' means HTTPS is required for all IP addresses.",
    userManagedAccess:
      "If enabled, users are allowed to manage their resources and permissions using the Account Management Console.",
    endpoints: "Shows the configuration of the protocol endpoints",
    loginTheme:
      "Select theme for login, OTP, grant, registration and forgot password pages.",
    accountTheme: "Select theme for user account management pages.",
    adminConsoleTheme: "Select theme for admin console.",
    emailTheme: "Select theme for emails that are sent by the server.",
    displayName: "Display name of provider when linked in admin console",
    priority: "Priority of the provider",
    enabled: "Set if the keys are enabled",
    active: "Set if the keys can be used for signing",
    AESKeySize:
      "Size in bytes for the generated AES key. Size 16 is for AES-128, Size 24 for AES-192, and Size 32 for AES-256. WARN: Bigger keys than 128 are not allowed on some JDK implementations.",
    "save-user-events":
      "If enabled, login events are saved to the database, which makes events available to the admin and account management consoles.",
    "save-admin-events":
      "If enabled, admin events are saved to the database, which makes events available to the admin console.",
    expiration:
      "Sets the expiration for events. Expired events are periodically deleted from the database.",
    "admin-clearEvents": "Deletes all admin events in the database.",
    includeRepresentation:
      "Include JSON representation for create and update requests.",
    "user-clearEvents": "Deletes all user events in the database.",
    ellipticCurve: "Elliptic curve used in ECDSA",
    secretSize: "Size in bytes for the generated secret",
    algorithm: "Intended algorithm for the key",
    keystore: "Path to keys file",
    keystorePassword: "Password for the keys",
    keyAlias: "Alias for the private key",
    keyPassword: "Password for the private key",
    privateRSAKey: "Private RSA Key encoded in PEM format",
    x509Certificate: "X509 Certificate encoded in PEM format",
    xFrameOptions:
      "Default value prevents pages from being included by non-origin iframes <1>Learn more</1>",
    contentSecurityPolicy:
      "Default value prevents pages from being included by non-origin iframes <1>Learn more</1>",
    contentSecurityPolicyReportOnly:
      "For testing Content Security Policies <1>Learn more</1>",
    xContentTypeOptions:
      "Default value prevents Internet Explorer and Google Chrome from MIME-sniffing a response away from the declared content-type <1>Learn more</1>",
    xRobotsTag:
      "Prevent pages from appearing in search engines <1>Learn more</1>",
    xXSSProtection:
      "Prevent pages from appearing in search engines <1>Learn more</1>",
    strictTransportSecurity:
      "The Strict-Transport-Security HTTP header tells browsers to always use HTTPS. Once a browser sees this header, it will only visit the site over HTTPS for the time specified (1 year) at max-age, including the subdomains. <1>Learn more</1>",
    failureFactor: "How many failures before wait is triggered.",
    permanentLockout:
      "Lock the user permanently when the user exceeds the maximum login failures.",
    waitIncrementSeconds:
      "When failure threshold has been met, how much time should the user be locked out?",
    maxFailureWaitSeconds: "Max time a user will be locked out.",
    maxDeltaTimeSeconds: "When will failure count be reset?",
    quickLoginCheckMilliSeconds:
      "If a failure happens concurrently too quickly, lock out the user.",
    minimumQuickLoginWaitSeconds:
      "How long to wait after a quick login failure.",
    ssoSessionIdle:
      "Time a session is allowed to be idle before it expires. Tokens and browser sessions are invalidated when a session is expired.",
    ssoSessionMax:
      "Max time before a session is expired. Tokens and browser sessions are invalidated when a session is expired.",
    ssoSessionIdleRememberMe:
      "Time a remember me session is allowed to be idle before it expires. Tokens and browser sessions are invalidated when a session is expired. If not set it uses the standard SSO Session Idle value.",
    ssoSessionMaxRememberMe:
      "Max time before a session is expired when a user has set the remember me option. Tokens and browser sessions are invalidated when a session is expired. If not set it uses the standard SSO Session Max value.",
    clientSessionIdle:
      "Time a client session is allowed to be idle before it expires. Tokens are invalidated when a client session is expired. If not set it uses the standard SSO Session Idle value.",
    clientSessionMax:
      "Max time before a client session is expired. Tokens are invalidated when a session is expired. If not set it uses the standard SSO Session Max value.",
    offlineSessionIdle:
      "Time an offline session is allowed to be idle before it expires. You need to use offline token to refresh at least once within this period; otherwise offline session will expire.",
    offlineSessionMaxLimited: "Enable offline session max",
    offlineSessionMax:
      "Max time before an offline session is expired regardless of activity.",
    loginTimeout:
      "Max time a user has to complete a login. This is recommended to be relatively long, such as 30 minutes or more",
    loginActionTimeout:
      "Max time a user has to complete login related actions like update password or configure totp. This is recommended to be relatively long, such as 5 minutes or more",
    defaultSigAlg: "Default algorithm used to sign tokens for the realm",
    revokeRefreshToken:
      "If enabled a refresh token can only be used up to 'Refresh Token Max Reuse' and is revoked when a different token is used. Otherwise refresh tokens are not revoked when used and can be used multiple times.",
    refreshTokenMaxReuse:
      "Maximum number of times a refresh token can be reused. When a different token is used, revocation is immediate.",
    accessTokenLifespan:
      "Max time before an access token is expired. This value is recommended to be short relative to the SSO timeout",
    accessTokenLifespanImplicitFlow:
      "Max time before an access token issued during OpenID Connect Implicit Flow is expired. This value is recommended to be shorter than the SSO timeout. There is no possibility to refresh token during implicit flow, that's why there is a separate timeout different to 'Access Token Lifespan'",
    clientLoginTimeout:
      "Max time a client has to finish the access token protocol. This should normally be 1 minute.",
    userInitiatedActionLifespan:
      "Maximum time before an action permit sent by a user (such as a forgot password e-mail) is expired. This value is recommended to be short because it's expected that the user would react to self-created action quickly.",
    defaultAdminInitiatedActionLifespan:
      "Maximum time before an action permit sent to a user by administrator is expired. This value is recommended to be long to allow administrators to send e-mails for users that are currently offline. The default timeout can be overridden immediately before issuing the token.",
    overrideActionTokens:
      "Override default settings of maximum time before an action permit sent by a user (such as a forgot password e-mail) is expired for specific action. This value is recommended to be short because it's expected that the user would react to self-created action quickly.",
    internationalization:
      "If enabled, you can choose which locales you support for this realm and which locale is the default.",
    supportedLocales:
      "The locales to support for this realm. The user chooses one of these locales on the login screen.",
    defaultLocale:
      "The initial locale to use. It is used on the login screen and other screens in the Admin Console and Account Console.",
  },
};
