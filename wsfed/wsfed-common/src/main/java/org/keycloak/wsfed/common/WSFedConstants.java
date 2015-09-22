/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.wsfed.common;

/**
 * Created on 5/5/15.
 *
 * WS-Fed parameters and other constants
 * From http://docs.oasis-open.org/wsfed/federation/v1.2/os/ws-federation-1.2-spec-os.html
 */
public interface WSFedConstants {
    /**
     * XML WSFedConstants
     */
    String TRUST_NSURI = "http://schemas.xmlsoap.org/ws/2005/02/trust";
    /*
    sp="http://schemas.xmlsoap.org/ws/2004/09/policy">
    <wsa:EndpointReference xmlns:wsa="http://www.w3.org/2005/08/addressing">
    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    xmlns:d3p1="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd
    */
    /**
     * wa
     *
     * This REQUIRED parameter specifies the action to be performed. By including the action, URIs
     * can be overloaded to perform multiple functions. For sign-in, this string MUST be "wsignin1.0".
     * Note that this serves roughly the same purpose as the WS-Addressing Action header for WS-Trust
     * SOAP RST messages.
     *
     */
    String WSFED_ACTION = "wa";

    String WSFED_SIGNIN_ACTION = "wsignin1.0";
    String WSFED_ATTRIBUTE_ACTION = "wattr1.0";

    String WSFED_SIGNOUT_ACTION = "wsignout1.0";
    String WSFED_SIGNOUT_CLEANUP_ACTION = "wsignoutcleanup1.0";

    /**
     * wreply
     *
     * This OPTIONAL parameter is the URL to which responses are directed. Note that this serves
     * roughly the same purpose as the WS-Addressing wsa:ReplyTo header for the WS-Trust SOAP
     * RST messages.
     *
     */
    String WSFED_REPLY = "wreply";

    /**
     * wctx
     *
     * This OPTIONAL parameter specifies the context information (if any) passed in with the request and
     * typically represents context from the original request. In order not to exceed URI length limitations,
     * the value of this parameter should be as small as possible.
     *
     */
    String WSFED_CONTEXT = "wctx";

    /**
     * wp
     *
     * This OPTIONAL parameter is the URL for the policy which can be obtained by using an HTTP GET
     * and identifies the policy to be used related to the action specified in "wa", buy MAY have a
     * broader scope than just the "wa". Refer to WS-Policy and WS-Trust for details on policy and
     * trust. This attribute is only used to reference policy documents. Note that this serves roughly
     * the same purpose as the Policy element in the WS-Trust SOAP RST messages.
     *
     */
    String WSFED_POLICY = "wp";

    /**
     * wct
     *
     * This OPTIONAL parameter indicates the current time at the sender for ensuring freshness.
     * This parameter is the string encoding of time using the XML Schema datetime time using UTC
     * notation. Note that this serves roughly the same purpose as the WS-Security Timestamp
     * elements in the Security headers of the SOAP RST messages.
     *
     */
    String WSFED_CURRENT_TIME = "wct";

    /**
     * wfed
     *
     * This OPTIONAL parameter indicates the federation context in which the request is made. This is
     * equivalent to the FederationId parameter in the RST message.
     *
     */
    String WSFED_FEDERATION_ID = "wfed";

    /**
     * wencoding
     *
     * This OPTIONAL parameter indicates the enconding style to be used for the XML parameter content.
     * If not specified the default behavior is to use standard URL encoding rules. This specification only
     * defines one other alternative, base64url as defined in section 5 of [RFC4648]. Support for alternate
     * encodings is expressed by assertions under the WebBinding assertion defined in this specification.
     */
    String WSFED_ENCODING = "wencoding";

    /**
     * wtrealm
     *
     * This REQUIRED parameter is the URI of the requesting realm. The wtrealm SHOULD be the security
     * realm of the resource in which nobody (except the resource or authorized delegates) can control
     * URLs. Note that this serves roughly the same purpose as the AppliesTo element in the WS-Trust
     * SOAP RST messages.
     */
    String WSFED_REALM = "wtrealm";

    /**
     * wfresh
     *
     * This OPTIONAL parameter indicates the freshness requirements. If specified, this indicates the
     * desired maximum age of the authentication specified in minutes. An IP/STS SHOULD NOT issue a
     * token with a longer lifetime. If specified as "0" it indicates a request fo the IP/STS to
     * re-prompt the user for authentication before issuing the token. Note that this servers roughly
     * the same purpose as the Freshness element in the WS-Trust SOAP RST messages.
     */
    String WSFED_FRESHNESS = "wfresh";

    /**
     * wauth
     * This OPTIONAL parameter indicates the REQUIRED authentication level. Note that this
     * parameter uses the same URIs and is equivalent to the wst:AuthenticationType element in
     * the WS-Trust SOAP RST messages.
     */
    String WSFED_AUTHENTICATION_LEVEL = "wauth";

    /**
     * wreq
     *
     * This OPTIONAL parameter specifies a token request using either a wst:RequestSecurityToken element
     * or a full request message as described in WS-Trust. If this parameter is not specified, it is
     * assumed that the responding service knows the correct type of token to return. Note that this can contain
     * the same RST payload as used in WS-Trust RST messages.
     */
    String WSFED_TOKEN_REQUEST_TYPE = "wreq";

    /**
     * whr
     *
     * This OPTIONAL parameter indicates that account partner realm of the client. This parameter is used to
     * indicate the IP/STS address for the requestor. This may be specified directly as a URL or
     * indirectly as an identifier (e.g. urn: or uuid:). In the case of an identifier the recipient is expected
     * to know how to translate this (or get it translated) to a URL. When the whr parameter is used, the
     * resource, or its local IP/STS, typically removes the parameter and writes a cookie to the client browser
     * to remember this setting for future requests. Then, the request proceeds in the same way as if it had
     * not been provided. Note that this serves roughly the same purpose as federation metadata for discovering
     * IP/STS locations.
     */
    String WSFED_HOME_REALM = "whr";

    /**
     * wreqptr
     *
     * This OPTIONAL parameter specifies a URL for where to find the request expressed as a wst:RequestSecurityToken
     * element. Note that this does not have a WS-Trust parallel. The wreqptr parameter MUST NOT be included in a
     * token request if wreq is present.
     */
    String WSFED_REQUEST_URL = "wreqptr";

    /**
     * wresult
     *
     * This REQUIRED parameter specifies the result of the token issuance. This can take the form of the
     * wst:RequestSecurityTokenResponse element or Wst:RequestSecurityTokenResponseCollection element,
     * a SOAP security token request response (that is, a S:Envelope) as detailed in WS-Trust, or a
     * SOAP S:Fault element. This carries the same content as a WS-Trust RSTR element (or even the
     * actual SOAP Envelope containing the RSTR element).
     */
    String WSFED_RESULT = "wresult";

    /**
     * wresultptr - This parameter specifies a URL to which an HTTP GET can be issued. The result is a document
     * of type text/xml that contains the issuance result. This can either be the
     * wst:RequestSecurityTokenResponse element, the wst:RequestSecurityTokenResponseCollection
     * element, a SOAP response, or a SOAP S:Fault element. NOTE that this serves roughly the same
     * purpose as the WS-ReferenceToken mechanism.
     */
    String WSFED_RESULT_URL = "wresultptr";


    /**
     * No pseudonym found for the specified scope
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/NoPseudonymInScope
     */
    String WSFED_ERROR_NOPSEUDONYMINSCOPE = "fed:NoPseudonymInScope";

    /**
     * The principal is already signed in (need not be reported)
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/AlreadySignedIn
     */
    String WSFED_ERROR_ALREADYSIGNEDIN = "fed:AlreadySignedIn";

    /**
     * The principal is not signed in (need not be reported)
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/NotSignedIn
     */
    String WSFED_ERROR_NOTSIGNEDIN = "fed:NotSignedIn";

    /**
     * An improper request was made (e.g., Invalid/unauthorized pseudonym request)
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/BadRequest
     */
    String WSFED_ERROR_BADREQUEST = "fed:BadRequest";

    /**
     * No match for the specified scope
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/NoMatchInScope
     */
    String WSFED_ERROR_NOMATCHINSCOPE = "fed:NoMatchInScope";

    /**
     * Credentials provided don’t meet the freshness requirements
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/NeedFresherCredentials
     */
    String WSFED_ERROR_NEEDFRESHERCREDENTIALS = "fed:NeedFresherCredentials";

    /**
     * Specific policy applies to the request – the new policy is specified in the S12:Detail element.
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/SpecificPolicy
     */
    String WSFED_ERROR_SPECIFICPOLICY = "fed:SpecificPolicy";

    /**
     * The specified dialect for claims is not supported
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/UnsupportedClaimsDialect
     */
    String WSFED_ERROR_UNSUPPORTEDCLAIMSDIALECT = "fed:UnsupportedClaimsDialect";

    /**
     * A requested RST parameter was not accepted by the STS.  The details element contains a fed:Unaccepted element.  This element’s value is a list of the unaccepted parameters specified as QNames.
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/RstParameterNotAccepted
     */
    String WSFED_ERROR_RSTPARAMETERNOTACCEPTED = "fed:RstParameterNotAccepted";

    /**
     * A desired issuer name is not supported by the STS
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/IssuerNameNotSupported
     */
    String WSFED_ERROR_ISSUERNAMENOTSUPPORTED = "fed:IssuerNameNotSupported";

    /**
     * A wencoding value or other parameter with XML content was received in an unknown/unsupported encoding.
     * http://docs.oasis-open.org/wsfed/federation/200706/Fault/UnsupportedEncoding
     */
    String WSFED_ERROR_UNSUPPORTEDENCODING = "fed:UnsupportedEncoding";
}
