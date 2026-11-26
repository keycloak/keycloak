/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUB;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;
import static org.keycloak.OID4VCConstants.RESERVED_CLAIM_NAMES;
import static org.keycloak.VCFormat.MSO_MDOC;

/**
 * Fetches credential claims from an external GraphQL endpoint during credential issuance.
 *
 * <p>An Object response is spread into per-field top-level claims (see #53956). Because GraphQL can't return
 * a field that wasn't requested, the query's selection set at Response Path tells us exactly what fields it
 * can produce — without running the query (see {@link GraphQLSelectionSetParser}). That lets this mapper
 * reject reserved claim names and declare its claims accurately in issuer metadata at config time, instead of
 * only finding out at issuance. A query the parser can't resolve this way (fragments, directives, ...) is
 * rejected as invalid configuration, so every saved mapper is guaranteed resolvable — metadata stays
 * authoritative rather than a best-effort guess.
 *
 * @author <a href="https://github.com/tdiesler">Thomas Diesler</a>
 */
public class OID4VCGraphQLClaimMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-graphql-claim-mapper";
    public static final String GRAPHQL_ENDPOINT = "graphqlEndpoint";
    public static final String GRAPHQL_QUERY = "graphqlQuery";
    public static final String GRAPHQL_VARIABLE_MAPPING = "graphqlVariableMapping";
    public static final String RESPONSE_PATH = "responsePath";

    // Message keys for ProtocolMapperConfigException. AdminRoot's ProtocolMappersResource.validateModel looks
    // these up via Properties.getProperty(key, defaultMessage) — Properties is backed by Hashtable, which NPEs
    // on a null key, so every exception thrown here must carry one (see OID4VCMapper's own MAPPER_*_ERROR keys).
    public static final String MAPPER_MISSING_CLAIM_NAME_ERROR = "oid4vciGraphqlMissingClaimNameError";
    public static final String MAPPER_MISSING_ENDPOINT_ERROR = "oid4vciGraphqlMissingEndpointError";
    public static final String MAPPER_MISSING_QUERY_ERROR = "oid4vciGraphqlMissingQueryError";
    public static final String MAPPER_MUTATION_NOT_SUPPORTED_ERROR = "oid4vciGraphqlMutationNotSupportedError";
    public static final String MAPPER_UNSUPPORTED_VARIABLE_SOURCE_ERROR = "oid4vciGraphqlUnsupportedVariableSourceError";
    public static final String MAPPER_RESERVED_SPREAD_FIELD_ERROR = "oid4vciGraphqlReservedSpreadFieldError";
    public static final String MAPPER_UNRESOLVABLE_QUERY_ERROR = "oid4vciGraphqlUnresolvableQueryError";

    private static final Logger LOG = Logger.getLogger(OID4VCGraphQLClaimMapper.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private KeycloakSession session;

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty claimNameConfig = new ProviderConfigProperty();
        claimNameConfig.setName(CLAIM_NAME);
        claimNameConfig.setLabel("Claim Name");
        claimNameConfig.setHelpText("Name of the claim in the credential subject, for scalar or array responses. "
                + "Not used when the response is an object — each field is spread into its own top-level claim.");
        claimNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(claimNameConfig);

        ProviderConfigProperty endpointConfig = new ProviderConfigProperty();
        endpointConfig.setName(GRAPHQL_ENDPOINT);
        endpointConfig.setLabel("GraphQL Endpoint");
        endpointConfig.setHelpText("URL of the external GraphQL service.");
        endpointConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(endpointConfig);

        ProviderConfigProperty queryConfig = new ProviderConfigProperty();
        queryConfig.setName(GRAPHQL_QUERY);
        queryConfig.setLabel("GraphQL Query");
        queryConfig.setHelpText("GraphQL query template. Variables are resolved from the user session.");
        queryConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(queryConfig);

        ProviderConfigProperty variableMappingConfig = new ProviderConfigProperty();
        variableMappingConfig.setName(GRAPHQL_VARIABLE_MAPPING);
        variableMappingConfig.setLabel("Variable Mapping");
        variableMappingConfig.setHelpText("Maps GraphQL variables to user attribute sources (e.g. 'id=user:username'). " +
                "Prefix 'user:' resolves from user attributes.");
        variableMappingConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(variableMappingConfig);

        ProviderConfigProperty responsePathConfig = new ProviderConfigProperty();
        responsePathConfig.setName(RESPONSE_PATH);
        responsePathConfig.setLabel("Response Path");
        responsePathConfig.setHelpText("Dot-separated path into the GraphQL response data (e.g. 'booking').");
        responsePathConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(responsePathConfig);
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        var mapper = new OID4VCGraphQLClaimMapper();
        mapper.session = session;
        return mapper;
    }

    @Override
    public String getDisplayType() {
        return "GraphQL Claim Mapper";
    }

    @Override
    public String getHelpText() {
        return "Fetches credential claims from an external GraphQL endpoint.";
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }

    /**
     * For a Scalar/Array response, the configured Claim Name is the one claim this mapper produces, same as
     * any other mapper. For an Object response, the fields it spreads are parsed statically from the query's
     * selection set at Response Path (see {@link GraphQLSelectionSetParser}) and declared individually.
     * {@link #validateMapperConfig} already rejects a query the parser can't resolve, so the unresolvable
     * case here only covers a config that bypassed validation (e.g. direct realm import).
     */
    @Override
    public List<List<String>> getMetadataAttributePaths() {
        var config = mapperModel.getConfig();
        if (config == null) {
            return Collections.emptyList();
        }
        var resolution = GraphQLSelectionSetParser.resolve(config.get(GRAPHQL_QUERY), config.get(RESPONSE_PATH));
        return switch (resolution.kind()) {
            case OBJECT -> resolution.fieldNames().stream()
                    .map(fieldName -> prefixMetadataAttributePath(List.of(fieldName)))
                    .toList();
            // Confirmed Scalar/Array: Claim Name is accurate, same as any other mapper.
            case SCALAR -> super.getMetadataAttributePaths();
            // Unreachable for a validated config; declaring Claim Name here would be a guess that might be
            // wrong if the response turns out to be an Object, so declare nothing rather than risk that.
            case UNRESOLVABLE -> Collections.emptyList();
        };
    }

    /**
     * Rejects shape-level misconfiguration (missing claim name/endpoint/query, a mutation or subscription
     * document, or an unrecognized variable-mapping source prefix) before the mapper is saved, so a broken
     * mapper never reaches issuance in the first place.
     */
    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client,
                              ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        super.validateConfig(session, realm, client, mapperModel);
        validateMapperConfig(mapperModel.getConfig());
    }

    /**
     * Defense-in-depth backstop for configs that bypassed {@link #validateConfig}, e.g. direct realm import.
     * Run centrally by the issuer endpoint before any mapper's claims are set, so a misconfigured mapper fails
     * the credential request with a client-visible {@code invalid_credential_request} instead of the opaque
     * server error that an uncaught exception from {@link #setClaim} would otherwise produce.
     *
     * <p>Includes format-aware reserved-claim check for spread fields (exempting mDoc as in the parent class's
     * {@link OID4VCMapper#validateAgainstSensitiveMappings}).
     */
    @Override
    public void validate() throws ProtocolMapperConfigException {
        super.validate();
        validateMapperConfig(mapperModel.getConfig());

        // Reserved-claim check for spread fields: now that format is known (set via setMapperModel at issuance
        // time), apply the same format-aware exemptions as the parent class (e.g. mDoc is exempt).
        var config = mapperModel.getConfig();
        if (config != null) {
            var query = config.get(GRAPHQL_QUERY);
            var responsePath = config.get(RESPONSE_PATH);
            var resolution = GraphQLSelectionSetParser.resolve(query, responsePath);
            if (resolution.kind() == GraphQLSelectionSetParser.Resolution.Kind.OBJECT) {
                for (var fieldName : resolution.fieldNames()) {
                    if (isReservedClaim(fieldName)) {
                        throw new ProtocolMapperConfigException("GraphQL response field '" + fieldName
                                + "' is a reserved claim name and cannot be used.", MAPPER_RESERVED_SPREAD_FIELD_ERROR);
                    }
                }
            }
        }
    }

    private void validateMapperConfig(Map<String, String> config) throws ProtocolMapperConfigException {
        // ProtocolMapperModel.getConfig() can be null: RepresentationToModel.toModel() passes the admin
        // request's config straight through removeEmptyString(), which returns null for a null input (e.g.
        // an admin API request with "config": null or no config at all). Normalize so the missing-field
        // checks below report a proper 400 instead of an uncaught NullPointerException turning into a 500.
        if (config == null) {
            config = Collections.emptyMap();
        }
        var claimName = config.get(CLAIM_NAME);
        var endpoint = config.get(GRAPHQL_ENDPOINT);
        var query = config.get(GRAPHQL_QUERY);
        var variableMapping = config.get(GRAPHQL_VARIABLE_MAPPING);
        var responsePath = config.get(RESPONSE_PATH);

        if (claimName == null || claimName.isBlank()) {
            throw new ProtocolMapperConfigException(
                    "GraphQL mapper misconfigured: claim name missing", MAPPER_MISSING_CLAIM_NAME_ERROR);
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new ProtocolMapperConfigException(
                    "GraphQL mapper misconfigured: endpoint missing", MAPPER_MISSING_ENDPOINT_ERROR);
        }
        if (query == null || query.isBlank()) {
            throw new ProtocolMapperConfigException(
                    "GraphQL mapper misconfigured: query missing", MAPPER_MISSING_QUERY_ERROR);
        }
        if (isUnsupportedDocument(query)) {
            throw new ProtocolMapperConfigException(
                    "GraphQL mapper only supports a single query operation; the document must start with 'query'",
                    MAPPER_MUTATION_NOT_SUPPORTED_ERROR);
        }
        // Validate response path doesn't contain blank segments (whitespace-only segments like "  " pass
        // split but fail at runtime navigation and are now rejected by the parser as UNRESOLVABLE).
        if (responsePath != null && !responsePath.isBlank()) {
            for (var segment : responsePath.split("\\.")) {
                if (segment.trim().isEmpty()) {
                    throw new ProtocolMapperConfigException(
                            "GraphQL mapper misconfigured: response path contains blank segments",
                            MAPPER_UNRESOLVABLE_QUERY_ERROR);
                }
            }
        }
        // Metadata relies on resolving the query's selection set at Response Path statically — a query that
        // can't be resolved this way is invalid configuration for this mapper, not a degraded-but-tolerated
        // case, so every saved config is guaranteed resolvable.
        var resolution = GraphQLSelectionSetParser.resolve(query, responsePath);
        if (resolution.kind() == GraphQLSelectionSetParser.Resolution.Kind.UNRESOLVABLE) {
            throw new ProtocolMapperConfigException(
                    "GraphQL mapper misconfigured: query and response path could not be resolved "
                            + "(fragments and directives are not supported)", MAPPER_UNRESOLVABLE_QUERY_ERROR);
        }
        // Reserved-claim check for spread fields is skipped during config validation because the mapper's
        // credential format isn't known yet at configuration time (a mapper can be used with multiple formats).
        // The runtime check in setClaim() applies the format-aware exemption (e.g. mDoc is exempt because its
        // reserved claims live in namespaced data elements that mappers cannot override).
        if (variableMapping != null) {
            for (var mapping : variableMapping.split(",")) {
                var parts = mapping.trim().split("=", 2);
                if (parts.length != 2) continue;
                var source = parts[1].trim();
                if (!source.startsWith(SOURCE_PREFIX_USER)) {
                    throw new ProtocolMapperConfigException("Unsupported variable source: '" + source
                            + "'. Expected prefix 'user:' (e.g. 'user:username')", MAPPER_UNSUPPORTED_VARIABLE_SOURCE_ERROR);
                }
            }
        }
    }

    @Override
    public void setClaim(VerifiableCredential verifiableCredential, UserSessionModel userSessionModel) {
    }

    @Override
    public void setClaim(Map<String, Object> claims, UserSessionModel userSessionModel) {
        var config = mapperModel.getConfig();
        if (config == null) {
            config = Collections.emptyMap();
        }
        var claimName = config.get(CLAIM_NAME);
        var endpoint = config.get(GRAPHQL_ENDPOINT);
        var query = config.get(GRAPHQL_QUERY);
        var variableMapping = config.get(GRAPHQL_VARIABLE_MAPPING);
        var responsePath = config.get(RESPONSE_PATH);

        if (claimName == null || claimName.isBlank()) {
            throw new RuntimeException("GraphQL mapper misconfigured: claim name missing");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new RuntimeException("GraphQL mapper misconfigured: endpoint missing");
        }
        if (query == null || query.isBlank()) {
            throw new RuntimeException("GraphQL mapper misconfigured: query missing");
        }
        if (isUnsupportedDocument(query)) {
            throw new RuntimeException("GraphQL mapper only supports a single query operation; the document must start with 'query'");
        }

        try {
            var variables = resolveVariables(userSessionModel, variableMapping);
            var responseData = executeQuery(endpoint, query, variables);

            var node = navigatePath(responseData, responsePath);
            if (node == null) {
                throw new RuntimeException("No data at response path '" + responsePath + "' from endpoint '" + endpoint + "'");
            }

            if (node.isObject()) {
                // Spread each field into its own top-level claim (claim.name is not used here) so each
                // field gets its own independent SD-JWT disclosure, instead of bundling the whole object
                // into one atomic, all-or-nothing disclosure (see #53956).
                var it = node.fields();
                while (it.hasNext()) {
                    var field = it.next();
                    var fieldName = field.getKey();
                    var value = field.getValue();
                    if (value.isObject()) {
                        throw new RuntimeException("GraphQL mapper does not support nested objects, "
                                + "but field '" + fieldName + "' is " + value.getNodeType());
                    }
                    assertNotReservedClaim(fieldName);
                    claims.put(fieldName, value.isArray() ? unwrapArray(value) : unwrap(value));
                }
            } else if (node.isArray()) {
                claims.put(claimName, unwrapArray(node));
            } else {
                claims.put(claimName, unwrap(node));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("GraphQL claim mapper failed for endpoint '" + endpoint + "'", ex);
        }
    }

    private JsonNode executeQuery(String endpoint, String query, Map<String, Object> variables) throws IOException {
        var requestBody = new HashMap<String, Object>();
        requestBody.put("query", query);
        if (!variables.isEmpty()) {
            requestBody.put("variables", variables);
        }

        LOG.debugf("GraphQL request to %s: %s", endpoint, JSON.writeValueAsString(requestBody));

        try (var response = SimpleHttp.create(session).doPost(endpoint).json(requestBody).asResponse()) {
            int status = response.getStatus();
            if (status != 200) {
                throw new RuntimeException("GraphQL request failed: HTTP " + status + " " + response.asString());
            }

            var responseJson = response.asJson();
            var errors = responseJson.get("errors");
            if (errors != null && errors.isArray() && !errors.isEmpty()) {
                throw new RuntimeException("GraphQL errors: " + errors);
            }

            return responseJson.get("data");
        }
    }

    private JsonNode navigatePath(JsonNode data, String path) {
        if (data == null || path == null || path.isEmpty()) {
            return data;
        }
        var node = data;
        for (var segment : path.split("\\.")) {
            if (node == null) return null;
            node = node.get(segment);
        }
        return node;
    }

    // Allow-list rather than block-list: a denylist of "mutation"/"subscription" prefixes is trivially
    // bypassed by a leading comment or fragment definition preceding the actual operation (see #53956
    // review). Requiring the document to start with the explicit 'query' keyword rejects everything else —
    // mutations, subscriptions, the anonymous shorthand selection set form, and anything preceded by a
    // comment or fragment definition — by construction, with no need to parse or strip those constructs.
    // The shorthand form ('{ ... }' with no keyword) is deliberately not supported: this mapper's purpose is
    // to call an external endpoint parameterized by a variable (e.g. 'query($id: String!) { ... }'), and the
    // shorthand form can't carry variable definitions anyway. This intentionally covers only a single query
    // operation, not multiple operations or leading fragment definitions; the initial scope matches the
    // mapper's documented single-operation query shape and can be relaxed later.
    private static final Pattern QUERY_KEYWORD = Pattern.compile("^query\\b", Pattern.CASE_INSENSITIVE);

    private static boolean isUnsupportedDocument(String query) {
        return !QUERY_KEYWORD.matcher(query.stripLeading()).find();
    }

    private static final String SOURCE_PREFIX_USER = "user:";

    private Map<String, Object> resolveVariables(UserSessionModel userSessionModel, String variableMapping) {
        if (variableMapping == null || variableMapping.isEmpty()) {
            return Collections.emptyMap();
        }
        var user = userSessionModel.getUser();
        var variables = new HashMap<String, Object>();
        for (var mapping : variableMapping.split(",")) {
            var parts = mapping.trim().split("=", 2);
            if (parts.length != 2) continue;
            var varName = parts[0].trim();
            var source = parts[1].trim();
            if (source.startsWith(SOURCE_PREFIX_USER)) {
                var userAttr = source.substring(SOURCE_PREFIX_USER.length());
                var values = KeycloakModelUtils.resolveAttribute(user, userAttr, false);
                if (!values.isEmpty()) {
                    variables.put(varName, values.iterator().next());
                }
            } else {
                throw new RuntimeException("Unsupported variable source: '" + source
                        + "'. Expected prefix 'user:' (e.g. 'user:username')");
            }
        }
        return variables;
    }

    /**
     * Mirrors {@link OID4VCMapper#validateAgainstSensitiveMappings}'s reserved-claim check (including its
     * 'id'-targets-'sub' normalization and mDoc format exemption), applied to a spread field name instead
     * of the configured Claim Name.
     */
    private boolean isReservedClaim(String fieldName) {
        // mDoc is exempt because its reserved claims live in semantically-equivalent locations that claim
        // mappers cannot reach (docType, MobileSecurityObject payload, issuerAuth, DeviceKeyInfo), so they
        // cannot be overridden by spread fields (see OID4VCMapper.validateAgainstSensitiveMappings).
        if (MSO_MDOC.equals(format)) {
            return false;
        }
        var normalized = CLAIM_NAME_SUBJECT_ID.equals(fieldName) ? CLAIM_NAME_SUB : fieldName;
        return RESERVED_CLAIM_NAMES.contains(normalized) && !getAllowedReservedClaims().contains(normalized);
    }

    /**
     * Runtime check for reserved claims in spread fields. Applies format-aware exemptions (e.g. mDoc) that
     * can't be determined at config time because the mapper's credential format isn't known until issuance.
     */
    private void assertNotReservedClaim(String fieldName) {
        if (isReservedClaim(fieldName)) {
            throw new RuntimeException("GraphQL response field '" + fieldName
                    + "' is a reserved claim name and cannot be used.");
        }
    }

    private List<Object> unwrapArray(JsonNode arrayNode) {
        var list = new ArrayList<>();
        for (var element : arrayNode) {
            list.add(unwrap(element));
        }
        return list;
    }

    private Object unwrap(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        throw new RuntimeException("GraphQL mapper only supports scalar claim values, "
                + "but got " + node.getNodeType());
    }
}
