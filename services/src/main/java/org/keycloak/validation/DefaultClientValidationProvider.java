/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.validation;

import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.grants.ciba.CibaClientValidation;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.utils.StringUtil;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

public class DefaultClientValidationProvider implements ClientValidationProvider {
    private enum FieldMessages {
        ROOT_URL("rootUrl",
                "Root URL is not a valid URL", "clientRootURLInvalid",
                "Root URL must not contain an URL fragment", "clientRootURLFragmentError",
                "Root URL uses an illegal scheme", "clientRootURLIllegalSchemeError"),

        BASE_URL("baseUrl",
                "Base URL is not a valid URL", "clientBaseURLInvalid",
                null, null,
                "Base URL uses an illegal scheme", "clientBaseURLIllegalSchemeError"),

        REDIRECT_URIS("redirectUris",
                "A redirect URI is not a valid URI", "clientRedirectURIsInvalid",
                "Redirect URIs must not contain an URI fragment", "clientRedirectURIsFragmentError",
                "A redirect URI uses an illegal scheme", "clientRedirectURIsIllegalSchemeError"),

        BACKCHANNEL_LOGOUT_URL("backchannelLogoutUrl",
                "Backchannel logout URL is not a valid URL", "backchannelLogoutUrlIsInvalid",
                null, null,
                "Backchannel logout URL uses an illegal scheme", "backchannelLogoutUrlIllegalSchemeError"),

        LOGO_URI(ClientModel.LOGO_URI,
                "Logo URL is not a valid URL", "logoURLInvalid",
                null, null,
                "Logo URL uses an illegal scheme", "logoURLIllegalSchemeError"),

        POLICY_URI(ClientModel.POLICY_URI,
                "Policy URL is not a valid URL", "policyURLInvalid",
                null, null,
                "Policy URL uses an illegal scheme", "policyURLIllegalSchemeError"),

        TOS_URI(ClientModel.TOS_URI,
                "Terms of service URL is not a valid URL", "tosURLInvalid",
                null, null,
                "Terms of service URL uses an illegal scheme", "tosURLIllegalSchemeError"),

        ADMIN_URL("masterSamlProcessingUrl",
                "Master SAML Processing URL is not a valid URL", "adminUrlURLInvalid",
                null, null,
                "Master SAML Processing URL uses an illegal scheme", "adminUrlURLIllegalSchemeError"),

        SAML_ASSERTION_CONSUMER_URL_POST_URI(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                "Assertion Consumer Service POST Binding URL is not a valid URL", "samlAssertionConsumerUrlPostURLInvalid",
                null, null,
                "Assertion Consumer Service POST Binding URL uses an illegal scheme", "samlAssertionConsumerUrlPostURLIllegalSchemeError"),

        SAML_ASSERTION_CONSUMER_URL_REDIRECT_URI(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE,
                "Assertion Consumer Service Redirect Binding URL is not a valid URL", "samlAssertionConsumerUrlRedirectURLInvalid",
                null, null,
                "Assertion Consumer Service Redirect Binding URL uses an illegal scheme", "samlAssertionConsumerUrlRedirectURLIllegalSchemeError"),

        SAML_ASSERTION_CONSUMER_URL_ARTIFACT_URI(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE,
                "Artifact Binding URL is not a valid URL", "samlAssertionConsumerUrlArtifactURLInvalid",
                null, null,
                "Artifact Binding URL uses an illegal scheme", "samlAssertionConsumerUrlArtifactURLIllegalSchemeError"),

        SAML_SINGLE_LOGOUT_SERVICE_URL_POST_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                "Logout Service POST Binding URL is not a valid URL", "samlLogoutServiceUrlPostURLInvalid",
                null, null,
                "Logout Service POST Binding URL uses an illegal scheme", "samlLogoutServiceUrlPostURLIllegalSchemeError"),

        SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE,
                "Logout Service ARTIFACT Binding URL is not a valid URL", "samlLogoutServiceUrlArtifactURLInvalid",
                null, null,
                "Logout Service ARTIFACT Binding URL uses an illegal scheme", "samlLogoutServiceUrlArtifactURLIllegalSchemeError"),

        SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE,
                "Logout Service Redirect Binding URL is not a valid URL", "samlLogoutServiceUrlRedirectURLInvalid",
                null, null,
                "Logout Service Redirect Binding URL uses an illegal scheme", "samlLogoutServiceUrlRedirectURLIllegalSchemeError"),

        SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_URI(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE,
                "Logout Service SOAP Binding URL is not a valid URL", "samlLogoutServiceUrlSoapURLInvalid",
                null, null,
                "Logout Service SOAP Binding URL uses an illegal scheme", "samlAssertionConsumerUrlPostURLIllegalSchemeError"),

        SAML_ARTIFACT_RESOLUTION_SERVICE_URL_URI(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE,
                "Artifact Resolution Service is not a valid URL", "samlAssertionConsumerUrlPostURLInvalid",
                null, null,
                "Artifact Resolution Service uses an illegal scheme", "samlAssertionConsumerUrlPostURLIllegalSchemeError"),

        SAML_METADATA_DESCRIPTOR_URL(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                "Metadata descriptor URL is not a valid URL", SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                null, null,
                "Metadata descriptor URL uses an illegal scheme", SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL);

        private String fieldId;

        private String invalid;
        private String invalidKey;

        private String fragment;
        private String fragmentKey;

        private String scheme;
        private String schemeKey;

        FieldMessages(String fieldId, String invalid, String invalidKey, String fragment, String fragmentKey, String scheme, String schemeKey) {
            this.fieldId = fieldId;
            this.invalid = invalid;
            this.invalidKey = invalidKey;
            this.fragment = fragment;
            this.fragmentKey = fragmentKey;
            this.scheme = scheme;
            this.schemeKey = schemeKey;
        }

        public String getFieldId() {
            return fieldId;
        }

        public String getInvalid() {
            return invalid;
        }

        public String getInvalidKey() {
            return invalidKey;
        }

        public String getFragment() {
            return fragment;
        }

        public String getFragmentKey() {
            return fragmentKey;
        }

        public String getScheme() {
            return scheme;
        }

        public String getSchemeKey() {
            return schemeKey;
        }
    }

    // TODO Before adding more validation consider using a library for validation
    @Override
    public ValidationResult validate(ValidationContext<ClientModel> context) {
        validateClientId(context);
        validateUrls(context);
        validatePairwiseInClientModel(context);
        new CibaClientValidation(context).validate();
        validateJwks(context);
        validateDefaultAcrValues(context);
        validateMinimumAcrValue(context);

        return context.toResult();
    }

    @Override
    public ValidationResult validate(ClientValidationContext.OIDCContext context) {
        validateClientId(context);
        validateUrls(context);
        validatePairwiseInOIDCClient(context);
        new CibaClientValidation(context).validate();
        validateDefaultAcrValues(context);
        validateMinimumAcrValue(context);

        return context.toResult();
    }

    private void validateClientId(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        if (StringUtil.isBlank(client.getClientId())) {
            context.addError("Client ID cannot be blank");
        }
    }

    private void validateUrls(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();

        // Use a fake URL for validating relative URLs as we may not be validating clients in the context of a request (import at startup)
        String authServerUrl = "https://localhost/auth";

        String rootUrl = ResolveRelative.resolveRootUrl(authServerUrl, authServerUrl, client.getRootUrl());

        // don't need to use actual rootUrl here as it'd interfere with others URL validations
        String baseUrl = ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, authServerUrl, client.getBaseUrl());

        String backchannelLogoutUrl = OIDCAdvancedConfigWrapper.fromClientModel(client).getBackchannelLogoutUrl();
        String resolvedBackchannelLogoutUrl =
                ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, authServerUrl, backchannelLogoutUrl);

        checkUri(FieldMessages.ROOT_URL, rootUrl, context, true, true);
        checkUri(FieldMessages.BASE_URL, baseUrl, context, true, false);
        checkUri(FieldMessages.BACKCHANNEL_LOGOUT_URL, resolvedBackchannelLogoutUrl, context, true, false);
        client.getRedirectUris().stream()
                .map(u -> ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, rootUrl, u))
                .forEach(u -> checkUri(FieldMessages.REDIRECT_URIS, u, context, false, true));
        checkUriLogo(FieldMessages.LOGO_URI, client.getAttribute(ClientModel.LOGO_URI), context);
        checkUri(FieldMessages.POLICY_URI, client.getAttribute(ClientModel.POLICY_URI), context, true, false);
        checkUri(FieldMessages.TOS_URI, client.getAttribute(ClientModel.TOS_URI), context, true, false);

        // extra validation URLs for SAML clients
        if (SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            checkUri(FieldMessages.ADMIN_URL, client.getManagementUrl(), context, true, false);
            checkUri(FieldMessages.SAML_ASSERTION_CONSUMER_URL_POST_URI, client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_ASSERTION_CONSUMER_URL_REDIRECT_URI, client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_URI, client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_URI, client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_URI, client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_URI, client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_URI, client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_URI, client.getAttribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE), context, true, false);
            checkUri(FieldMessages.SAML_METADATA_DESCRIPTOR_URL, client.getAttribute(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL), context, true, false);
        }
    }


    private void checkUri(FieldMessages field, String url, ValidationContext<ClientModel> context, boolean checkValidUrl, boolean checkFragment) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            String urlToCheck=url;
            if(field==FieldMessages.BACKCHANNEL_LOGOUT_URL){
                if(checkCurlyBracketsBalanced(url))
                    // This allow user to set parametrized backchannel logout url in this format : http://{example}/{example2}
                    urlToCheck=url.replace("{","%7B").replace("}","%7D");
                else throw new MalformedURLException();
            }
            URI uri = new URI(urlToCheck);

            boolean valid = true;
            if (uri.getScheme() != null && (uri.getScheme().equals("data") || uri.getScheme().equals("javascript"))) {
                context.addError(field.getFieldId(), field.getScheme(), field.getSchemeKey());
                valid = false;
            }

            // KEYCLOAK-3421
            if (checkFragment && uri.getFragment() != null) {
                context.addError(field.getFieldId(), field.getFragment(), field.getFragmentKey());
                valid = false;
            }

            // Don't check if URL is valid if there are other problems with it; otherwise it could lead to duplicit errors.
            // This cannot be moved higher because it acts on differently based on environment (e.g. sometimes it checks
            // scheme, sometimes it doesn't).
            if (checkValidUrl && valid) {
                uri.toURL(); // throws an exception
            }
        }

        catch (MalformedURLException | IllegalArgumentException | URISyntaxException e) {
            context.addError(field.getFieldId(), field.getInvalid(), field.getInvalidKey());
        }
    }

    /**
     * Check if url has curly brackets in correct position ('{' before '}')
     * @param url to check
     * @return true if curly brackets are balanced, else false
     */
    public static boolean checkCurlyBracketsBalanced(String url)
    {
        Deque<Character> stack
                = new ArrayDeque<>();

        for(char singleLetter:url.toCharArray()){
            if (singleLetter == '{')
            {
                // Push the element in the stack
                stack.push(singleLetter);
                continue;
            }
            if(stack.isEmpty() && (singleLetter=='}')) return false;
            char check;
            if(singleLetter=='}'){
                check=stack.pop();
                if(check!='{') return false;
            }

        }


        return stack.isEmpty();
    }

    private void checkUriLogo(FieldMessages field, String url, ValidationContext<ClientModel> context) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            URI uri = new URI(url);

            if (uri.getScheme() != null &&  uri.getScheme().equals("javascript")) {
                context.addError(field.getFieldId(), field.getScheme(), field.getSchemeKey());
            }

        }
        catch (URISyntaxException e) {
            context.addError(field.getFieldId(), field.getInvalid(), field.getInvalidKey());
        }
    }

    private void validatePairwiseInClientModel(ValidationContext<ClientModel> context) {
        List<ProtocolMapperRepresentation> foundPairwiseMappers = PairwiseSubMapperUtils.getPairwiseSubMappers(toRepresentation(context.getObjectToValidate(), context.getSession()));

        for (ProtocolMapperRepresentation foundPairwise : foundPairwiseMappers) {
            String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(foundPairwise);
            validatePairwise(context, sectorIdentifierUri);
        }
    }

    private void validatePairwiseInOIDCClient(ClientValidationContext.OIDCContext context) {
        OIDCClientRepresentation oidcRep = context.getOIDCClient();

        SubjectType subjectType = SubjectType.parse(oidcRep.getSubjectType());
        String sectorIdentifierUri = oidcRep.getSectorIdentifierUri();

        // If sector_identifier_uri is in oidc config, then always validate it
        if (SubjectType.PAIRWISE == subjectType || (sectorIdentifierUri != null && !sectorIdentifierUri.isEmpty())) {
            validatePairwise(context, oidcRep.getSectorIdentifierUri());
        }
    }

    private void validatePairwise(ValidationContext<ClientModel> context, String sectorIdentifierUri) {
        ClientModel client = context.getObjectToValidate();
        String rootUrl = client.getRootUrl();
        Set<String> redirectUris = new HashSet<>();
        if (client.getRedirectUris() != null) redirectUris.addAll(client.getRedirectUris());

        try {
            PairwiseSubMapperValidator.validate(context.getSession(), rootUrl, redirectUris, sectorIdentifierUri);
        } catch (ProtocolMapperConfigException e) {
            context.addError("pairWise", e.getMessage(), e.getMessageKey());
        }
    }

    private void validateJwks(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();

        if (Boolean.parseBoolean(client.getAttribute(OIDCConfigAttributes.USE_JWKS_URL))
            && Boolean.parseBoolean(client.getAttribute(OIDCConfigAttributes.USE_JWKS_STRING))) {
            context.addError("jwksUrl", "Illegal to use both jwks_uri and jwks_string", "duplicatedJwksSettings");
        }
    }

    private void validateDefaultAcrValues(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        List<String> defaultAcrValues = AcrUtils.getDefaultAcrValues(client);
        Map<String, Integer> acrToLoaMap = AcrUtils.getAcrLoaMap(client);
        if (acrToLoaMap.isEmpty()) {
            acrToLoaMap = AcrUtils.getAcrLoaMap(client.getRealm());
        }
        for (String configuredAcr : defaultAcrValues) {
            if (acrToLoaMap.containsKey(configuredAcr)) continue;
            if (LoAUtil.getLoAConfiguredInRealmBrowserFlow(client.getRealm())
                    .noneMatch(level -> configuredAcr.equals(String.valueOf(level)))) {
                context.addError("defaultAcrValues", "Default ACR values need to contain values specified in the ACR-To-Loa mapping or number levels from set realm browser flow");
            }
        }
    }

    private void validateMinimumAcrValue(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        String minimumAcrValue = AcrUtils.getMinimumAcrValue(client);
        if (minimumAcrValue != null) {
            Map<String, Integer> acrToLoaMap = AcrUtils.getAcrLoaMap(client);
            if (acrToLoaMap.isEmpty()) {
                acrToLoaMap = AcrUtils.getAcrLoaMap(client.getRealm());
            }

            if(!acrToLoaMap.containsKey(minimumAcrValue)) {
                if (LoAUtil.getLoAConfiguredInRealmBrowserFlow(client.getRealm())
                        .noneMatch(level -> minimumAcrValue.equals(String.valueOf(level)))) {
                    context.addError("minimumAcrValue", "Minimum ACR value needs to be value specified in the ACR-To-Loa mapping or number level from set realm browser flow");
                }
            }
        }
    }
}
