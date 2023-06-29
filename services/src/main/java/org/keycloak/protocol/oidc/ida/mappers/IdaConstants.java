package org.keycloak.protocol.oidc.ida.mappers;

public class IdaConstants {
    public static final String IDA_PROVIDER_ID = "oidc-ida-mapper";

    public static final String ERROR_MESSAGE_REQUEST_CLAIMS_ERROR = "The request claims are empty.";
    public static final String ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR = "Could not connect the IDA External Store.";
    public static final String ERROR_MESSAGE_REQUEST_CLAIMS_JSON_SYNTAX_ERROR_ERROR = "The request claims have syntax error in json.";

    public static final String ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR = "The user claims of IDA external store have syntax error in json.";

    public static final String ERROR_MESSAGE_IDA_EXTERNAL_STORE_NOT_SPECIFIED = "The IDA External Store has not been specified.";

    public static final String USERINFO = "userinfo";
    public static final String VERIFIED_CLAIMS = "verified_claims";

    static final String IDA_DISPLAY_TYPE = "OpenID Connect for Identity Assurance 1.0(OIDC4IDA)";
    static final String IDA_HELP_TEXT = "Adds Verified Claims to an OpenID Connect UserInfo response or an ID Token";

}
