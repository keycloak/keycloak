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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaClientValidation;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.services.managers.ResourceAdminManager.CLIENT_SESSION_HOST_PROPERTY;

public class DefaultClientValidationProvider implements ClientValidationProvider {

    private static final Logger logger = Logger.getLogger(DefaultClientValidationProvider.class);

    // TODO Before adding more validation consider using a library for validation
    @Override
    public ValidationResult validate(ValidationContext<ClientModel> context) {
        validateClientId(context);
        validateProtocol(context);
        validateUrls(context);
        validatePairwiseInClientModel(context);
        new CibaClientValidation(context).validate();
        validateJwks(context);
        validateAcrLoaMap(context);
        validateDefaultAcrValues(context);
        validateMinimumAcrValue(context);
        validateClientSessionTimeout(context);
        validateX509Credentials(context);

        return context.toResult();
    }

    @Override
    public ValidationResult validate(ClientValidationContext.OIDCContext context) {
        validateClientId(context);
        validateProtocol(context);
        validateUrls(context);
        validatePairwiseInOIDCClient(context);
        new CibaClientValidation(context).validate();
        validateAcrLoaMap(context);
        validateDefaultAcrValues(context);
        validateMinimumAcrValue(context);
        //context.getSession().getContext().getRealm().
        return context.toResult();
    }

    private void validateClientId(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        if (StringUtil.isBlank(client.getClientId())) {
            context.addError("Client ID cannot be blank");
        }
    }

    private void validateProtocol(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        String protocol = client.getProtocol();

        // null protocol is allowed
        if (protocol == null) {
            return;
        }

        LoginProtocolFactory factory = (LoginProtocolFactory) context.getSession().getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, protocol);

        if (factory == null) {
            context.addError("protocol", "Invalid protocol: " + protocol);
            return;
        }

        if (!factory.allowAsClientProtocol()) {
            context.addError("protocol", "Protocol '" + protocol + "' cannot be used as a client protocol");
        }
    }

    private void validateUrls(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        boolean saml = SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol());
        // Startup imports have no request context. Use a fixed server URL for syntax checks.
        String authServerUrl = "https://localhost/auth";
        String rootUrl = ResolveRelative.resolveRootUrl(authServerUrl, authServerUrl, client.getRootUrl());

        ClientUriField.validate(client, (field, values) -> {
            // CIBA uses its dedicated validator for realm SSL requirements and required metadata.
            if (field == ClientUriField.CIBA_NOTIFICATION_ENDPOINT) {
                return;
            }
            ClientUriField.ValidationMessages messages = field.getValidationMessages(client.getProtocol());

            for (String value : values) {
                if (field == ClientUriField.POST_LOGOUT_REDIRECT_URIS
                        && ("+".equals(value) || "-".equals(value) || "*".equals(value))) {
                    continue;
                }
                if (field == ClientUriField.WEB_ORIGINS
                        && ("+".equals(value) || "*".equals(value) || "null".equals(value))) {
                    continue;
                }
                String url = switch (field) {
                    case ROOT_URL -> rootUrl;
                    // These checks have historically used a fixed root, independent of the client root.
                    case BASE_URL, BACKCHANNEL_LOGOUT_URL -> ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, authServerUrl, value);
                    case ADMIN_URL -> saml ? value : ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, rootUrl, value)
                            .replace(CLIENT_SESSION_HOST_PROPERTY, "localhost");
                    case REDIRECT_URIS, POST_LOGOUT_REDIRECT_URIS, REQUEST_URIS, JWKS_URL, FRONTCHANNEL_LOGOUT_URL ->
                            ResolveRelative.resolveRelativeUri(authServerUrl, authServerUrl, rootUrl, value);
                    default -> value;
                };
                if (field == ClientUriField.LOGO_URI) {
                    checkUriLogo(messages, url, context);
                } else {
                    boolean checkValidUrl = field != ClientUriField.REDIRECT_URIS && field != ClientUriField.POST_LOGOUT_REDIRECT_URIS
                            && field != ClientUriField.REQUEST_URIS && field != ClientUriField.WEB_ORIGINS;
                    boolean checkFragment = field == ClientUriField.ROOT_URL || field == ClientUriField.REDIRECT_URIS
                            || field == ClientUriField.POST_LOGOUT_REDIRECT_URIS;
                    checkUri(field, messages, url, context, checkValidUrl, checkFragment);
                }
            }
        });
    }

    private void checkUri(ClientUriField uriField, ClientUriField.ValidationMessages field, String url, ValidationContext<ClientModel> context, boolean checkValidUrl, boolean checkFragment) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            String urlToCheck=url;
            if(uriField==ClientUriField.BACKCHANNEL_LOGOUT_URL){
                if(checkCurlyBracketsBalanced(url))
                    // This allow user to set parametrized backchannel logout url in this format : http://{example}/{example2}
                    urlToCheck=url.replace("{","%7B").replace("}","%7D");
                else throw new MalformedURLException();
            }
            URI uri = uriField == ClientUriField.WEB_ORIGINS ? parseWebOrigin(urlToCheck) : new URI(urlToCheck);

            boolean valid = true;
            if (uri.getScheme() != null && (uri.getScheme().equalsIgnoreCase("data") || uri.getScheme().equalsIgnoreCase("javascript"))) {
                context.addError(field.fieldId(), field.scheme(), field.schemeKey());
                valid = false;
            }

            // KEYCLOAK-3421
            if (checkFragment && uri.getFragment() != null) {
                context.addError(field.fieldId(), field.fragment(), field.fragmentKey());
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
            context.addError(field.fieldId(), field.invalid(), field.invalidKey());
        }
    }

    private URI parseWebOrigin(String origin) throws URISyntaxException {
        try {
            return new URI(origin);
        } catch (URISyntaxException e) {
            // Legacy origins can omit the scheme, including numeric hosts with a port.
            URI authority = new URI("http://" + origin).parseServerAuthority();
            if (authority.getHost() != null && authority.getRawUserInfo() == null && origin.equals(authority.getRawAuthority())) {
                return authority;
            }
            throw e;
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

    private void checkUriLogo(ClientUriField.ValidationMessages field, String url, ValidationContext<ClientModel> context) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            URI uri = new URI(url);

            if (uri.getScheme() != null &&  uri.getScheme().equalsIgnoreCase("javascript")) {
                context.addError(field.fieldId(), field.scheme(), field.schemeKey());
            }

        }
        catch (URISyntaxException e) {
            context.addError(field.fieldId(), field.invalid(), field.invalidKey());
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
        if (!OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            return;
        }
        List<String> defaultAcrValues = AcrUtils.getDefaultAcrValues(client);
        Map<String, Integer> acrToLoaMap = AcrUtils.getAcrLoaMap(client);
        for (String configuredAcr : defaultAcrValues) {
            if (acrToLoaMap.containsKey(configuredAcr)) continue;
            if (LoAUtil.getLoAConfiguredInRealmBrowserFlow(client.getRealm())
                    .noneMatch(level -> configuredAcr.equals(String.valueOf(level)))) {
                context.addError("defaultAcrValues", "Default ACR values need to contain values specified in the ACR-To-Loa mapping or number levels from set realm browser flow");
            }
        }
    }

    private void validateAcrLoaMap(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        if (!SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol()) && !OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            return;
        }
        String value = client.getAttribute(Constants.ACR_LOA_MAP);
        if (value != null && StringUtil.isNotBlank(value)) {
            try {
                Map<String, Integer> map = AcrUtils.parseAcrLoaMap(value);
                if (SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
                    for (String uri : map.keySet()) {
                        new URI(uri);
                    }
                }
            } catch (IOException e) {
                context.addError(Constants.ACR_LOA_MAP, "Invalid client configuration (ACR-LOA map) for client");
            } catch (URISyntaxException e) {
                context.addError(Constants.ACR_LOA_MAP, "Invalid URI for ACR-LOA map: " + e.getInput());
            }
        }
    }

    private void validateMinimumAcrValue(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        if (!SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol()) && !OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            return;
        }
        String minimumAcrValue = AcrUtils.getMinimumAcrValue(client);
        if (minimumAcrValue != null) {
            if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
                Map<String, Integer> acrToLoaMap = AcrUtils.getAcrLoaMap(client);

                if (!acrToLoaMap.containsKey(minimumAcrValue)) {
                    if (LoAUtil.getLoAConfiguredInRealmBrowserFlow(client.getRealm())
                            .noneMatch(level -> minimumAcrValue.equals(String.valueOf(level)))) {
                        context.addError("minimumAcrValue", "Minimum ACR value needs to be value specified in the ACR-To-Loa mapping or number level from set realm browser flow");
                    }
                }
            } else {
                Map<String, Integer> acrToLoaMap = AcrUtils.getUriLoaMap(client);
                if (!acrToLoaMap.containsKey(minimumAcrValue)) {
                    context.addError("minimumAcrValue", "Minimum ACR value needs to be a URI specified in the ACR-To-Loa mapping");
                }
            }
        }
    }
    private void validateClientSessionTimeout(ValidationContext<ClientModel> context) {
        ClientModel clientModel = context.getObjectToValidate();
        if (clientModel == null ) return;
        RealmModel realmModel =  clientModel.getRealm();
        if (realmModel == null ) return;

        //Realm values
        int realmIdle = realmModel.getSsoSessionIdleTimeout();
        int realmMax = realmModel.getSsoSessionMaxLifespan();
        int realmRememberIdle = realmModel.getSsoSessionIdleTimeoutRememberMe();
        int realmRememberMax = realmModel.getSsoSessionMaxLifespanRememberMe();

        boolean rememberMeEnabled = realmModel.isRememberMe();

        Integer clientIdle = parseIntAttribute(clientModel.getAttribute(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT));
        Integer clientMax = parseIntAttribute(clientModel.getAttribute(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN));

        if(!rememberMeEnabled) {
            // Client idle Timeout validation on Remember me disabled
            if (clientIdle != null && clientIdle > realmIdle) {
                context.addError(
                        OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT,
                        "Client session idle timeout cannot exceed realm SSO session idle timeout.",
                        Messages.CLIENT_IDLE
                );
            }

            // Max Lifespan validation on Remember me disabled
            if (clientMax != null && clientMax > realmMax) {
                context.addError(
                        OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN,
                        "Client session max lifespan cannot exceed realm SSO session max lifespan.",
                        Messages.CLIENT_MAXLIFE_SPAN
                );
            }
        } else {
            int allowedMaxIdleTimeIfRememberMeEnabled = Math.max(realmIdle, realmRememberIdle);
            int allowedMaxSpanIfRememberMeEnabled = Math.max(realmMax,realmRememberMax);

            //Client idle Timeout validation on Remember me enabled
            if (clientIdle != null && clientIdle > allowedMaxIdleTimeIfRememberMeEnabled) {
                context.addError(
                        OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT,
                        "Client session idle timeout cannot exceed realm SSO session idle timeout and RememberMe idle timeout.",
                        Messages.CLIENT_IDLE_REMEMBERME
                );
            }

            // Max Lifespan validation on Remember me enabled
            if (clientMax != null && clientMax > allowedMaxSpanIfRememberMeEnabled) {
                context.addError(
                        OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN,
                        "Client session max lifespan cannot exceed realm SSO session max lifespan and RememberMe Max span.",
                        Messages.CLIENT_MAXLIFESPAN_REMEMBERME
                );
            }
        }

    }

    private void validateX509Credentials(ValidationContext<ClientModel> context) {
        ClientModel client = context.getObjectToValidate();
        if (!client.isPublicClient() && !client.isBearerOnly() && X509ClientAuthenticator.PROVIDER_ID.equals(client.getClientAuthenticatorType())) {
            // TODO: return validation error for keycloak 27.0
            if (Boolean.parseBoolean(client.getAttribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON))) {
                logger.warnf("Option '%s' is deprecated. Please configure the X.509 client authenticator to use exact Subject DN for client '%s' in realm '%s'.",
                        X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, client.getClientId(), context.getSession().getContext().getRealm().getName());
            }
            if (StringUtil.isBlank(client.getAttribute(X509ClientAuthenticator.ATTR_CA_SUBJECT_DN))) {
                logger.warnf("Option '%s' is null or empty, this configuration is deprecated, please configure it for better security for client '%s' in realm '%s'",
                        X509ClientAuthenticator.ATTR_CA_SUBJECT_DN, client.getClientId(), context.getSession().getContext().getRealm().getName());
            }
        }
    }

    private Integer parseIntAttribute(String value) {
        try {
            return (value == null || value.isEmpty()) ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
