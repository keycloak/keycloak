/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.attributeQuery.server;

import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemException;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.*;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.attributeQuery.SamlAttributeQueryUtils;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.SAML2AttributeQueryResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.Urls;
import org.w3c.dom.Document;

import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Implementation of the {@link AttributeQueryServerProvider} to provide functionality for responding to SAML2 attribute
 * query requests.
 */
public class DefaultSamlAttributeQueryServerProvider implements AttributeQueryServerProvider {


    protected static final Logger logger = Logger.getLogger(DefaultSamlAttributeQueryServerProvider.class);

    private AttributeQueryContext context;

    public void close() {}

    /**
     * Respond to the given attribute query request described by the context
     * @param context the context the request is taking place in
     * @return the SOAP response to the attribute query request
     */
    public Response respond(AttributeQueryContext context) {
        this.context = context;

        // load configs
        List<SamlAttributeQueryServerConfig> configs = SamlAttributeQueryServerConfig.loadAllAndVerify(context.getSession(), context.getRealm()).collect(Collectors.toList());
        if (configs.isEmpty()) {
            logger.error("no valid attribute query clients found");
            return Soap.createFault().reason("invalid configuration").detail("SAML attribute query endpoint is improperly configured").build();
        }

        // parse attribute query request
        AttributeQueryType attributeQueryType;
        try {
            attributeQueryType = SamlAttributeQueryUtils.parseAttributeQueryRequest(((SAMLDocumentHolder) context.getSamlDocumentHolder()).getSamlObject());
        } catch(ParsingException ex){
            logger.errorf("failed to parse attribute query request: %s", ex.getMessage());
            return Soap.createFault().reason("invalid request").detail("invalid request").build();
        }

        // find first valid configuration
        SamlAttributeQueryServerConfig trustedConfig = configs.stream().filter(c -> doesValidateRequest(c, (SAMLDocumentHolder) context.getSamlDocumentHolder(), attributeQueryType)).findFirst().orElse(null);
        if (trustedConfig == null) {
            logger.error("no configurations match received request");
            return Soap.createFault().reason("invalid request").detail("Invalid request").build();
        }

        // try and find user
        UserModel user = SamlAttributeQueryUtils.getUserFromSubject(context.getSession(), trustedConfig, ((NameIDType)attributeQueryType.getSubject().getSubType().getBaseID()).getValue());
        if (user == null){
            logger.error("user cannot be found");
            return Soap.createFault().reason("invalid request").detail("Invalid request").build();
        }

        // generate response
        try {
            return Soap.createMessage().addToBody(generateResponse(user, attributeQueryType, trustedConfig)).build();
        } catch (ProcessingException ex){
            logger.errorf("error building response: %s", ex);
            return Soap.createFault().reason("server error").detail("error building response").build();
        }
    }

    /**
     * helper function to determine if the given config can successfully validate the request. This checks to see if the
     * provided configuration can decrypt, verify the signature, and verify the contents of the request
     * @param config the configuration
     * @param holder the attribute query document
     * @param req the request extracted from the document
     * @return true if the config can successfully validate the request
     */
    private boolean doesValidateRequest(SamlAttributeQueryServerConfig config, SAMLDocumentHolder holder, AttributeQueryType req){
        try {
            SamlAttributeQueryUtils.decryptAndVerifyRequest(context.getSession(), config, holder, req);
            return true;
        } catch (ProcessingException | VerificationException ignored){
            return false;
        }
    }

    /**
     * Generate an attribute query response for the provided user with the provided configuration. The configuration is
     * used to determine what attributes to include in the response, as well as how to sign and encrypt the response
     * document.
     * @param user The user to build the response for
     * @param request the attribute query request this is in response to
     * @param trustedConfig the configuration
     * @return the attribute query response document with attributes from the provided user
     * @throws ProcessingException thrown when the response cannot be created
     */
    private Document generateResponse(UserModel user, AttributeQueryType request, SamlAttributeQueryServerConfig trustedConfig) throws ProcessingException {
        ClientModel client = context.getRealm().getClientByClientId(trustedConfig.getClientId());
        SamlClient samlClient = new SamlClient(client);

        ResponseType rt = new SAML2AttributeQueryResponseBuilder()
            .status(JBossSAMLURIConstants.STATUS_SUCCESS.getUri())
            .issuer(Urls.realmIssuer(context.getSession().getContext().getUri().getBaseUri(), context.getSession().getContext().getRealm().getName()))
            .inResponseTo(request.getID())
            .attributes(filterAttributes(user.getAttributes(), trustedConfig.getFilters()))
            .subject(user.getUsername())
            .tokenLifespan(getTokenLifespan(context.getRealm(), samlClient))
            .audienceRestriction(trustedConfig.getAudience())
            .createAttributeQueryResponse();

        Document samlDocument;
        try {
            samlDocument = SAML2Response.convert(rt);
        } catch (ConfigurationException | ParsingException | ProcessingException ex){
            throw new ProcessingException("unable to convert response type into SAML document");
        }

        try {
            SamlAttributeQueryUtils.signAndEncryptDoc(context.getSession(), trustedConfig, samlClient, samlDocument);
        } catch (PemException ex){
            throw new ProcessingException("unable to sign and encrypt document");
        }

        return samlDocument;
    }

    /**
     * Filter attributes using the provided regex expressions. An attribute matching any of the provided regex expressions
     * will be included in the return attributes
     * @param attributes The attribute set to filter
     * @param filters The regex expressions
     * @return The filtered attributes
     */
    private Map<String, List<String>> filterAttributes(Map<String, List<String>> attributes, List<String> filters){
        if (filters == null || filters.isEmpty()) {
            return attributes;
        }

        return attributes.entrySet().stream().filter(entry -> filters.stream().anyMatch(filter -> {
            Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(entry.getKey());
            return matcher.find();
        })).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));
    }

    /**
     * Get the token lifespan configuration in seconds for the realm or SAML client. SAML client takes precedence.
     * @param realm the keycloak realm
     * @param samlClient the SAML client
     * @return the lifespan in seconds
     */
    private int getTokenLifespan(RealmModel realm, SamlClient samlClient){
        int assertionLifespan = samlClient.getAssertionLifespan();
        return assertionLifespan <= 0 ? realm.getAccessCodeLifespan() : assertionLifespan;
    }
}
