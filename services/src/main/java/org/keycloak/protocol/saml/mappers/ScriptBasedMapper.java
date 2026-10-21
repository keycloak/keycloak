package org.keycloak.protocol.saml.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptCompilationException;
import org.keycloak.scripting.ScriptingProvider;

import org.jboss.logging.Logger;


/**
 * This class provides a mapper that uses javascript to attach a value to an attribute for SAML tokens.
 * The mapper can handle both a result that is a single value, or multiple values (an array or a list for example).
 * For the latter case, it can return the result as a single attribute with multiple values, or as multiple attributes
 * However, in all cases, the returned values must be castable to String values.
 *
 * @author Alistair Doswald
 */
public class ScriptBasedMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper, EnvironmentDependentProviderFactory {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    public static final String PROVIDER_ID = "saml-javascript-mapper";
    private static final String SINGLE_VALUE_ATTRIBUTE = "single";
    private static final Logger LOGGER = Logger.getLogger(ScriptBasedMapper.class);

    /*
     * This static property block is used to determine the elements available to the mapper. This is determinant
     * both for the frontend (gui elements in the mapper) and for the backend.
     */
    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.SCRIPT_TYPE);
        property.setLabel(ProviderConfigProperty.SCRIPT_TYPE);
        property.setName(ProviderConfigProperty.SCRIPT_TYPE);
        property.setHelpText(
                "Script to compute the attribute value. \n" + //
                        " Available variables: \n" + //
                        " 'user' - the current user.\n" + //
                        " 'realm' - the current realm.\n" + //
                        " 'clientSession' - the current clientSession.\n" + //
                        " 'userSession' - the current userSession.\n" + //
                        " 'keycloakSession' - the current keycloakSession.\n\n" +
                        "To use: the last statement is the value returned to Java.\n" +
                        "The result will be tested if it can be iterated upon (e.g. an array or a collection).\n" +
                        " - If it is not, toString() will be called on the object to get the value of the attribute\n" +
                        " - If it is, toString() will be called on all elements to return multiple attribute values.\n"//
        );
        property.setDefaultValue("/**\n" + //
                " * Available variables: \n" + //
                " * user - the current user\n" + //
                " * realm - the current realm\n" + //
                " * clientSession - the current clientSession\n" + //
                " * userSession - the current userSession\n" + //
                " * keycloakSession - the current keycloakSession\n" + //
                " */\n\n\n//insert your code here..." //
        );
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(SINGLE_VALUE_ATTRIBUTE);
        property.setLabel("Single Value Attribute");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("If true, all values will be stored under one attribute with multiple attribute values.");
        configProperties.add(property);
        AttributeStatementHelper.setConfigProperties(configProperties);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Javascript Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Evaluates a JavaScript function to produce an attribute value based on context information.";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SCRIPTS);
    }

    /**
     *  This method attaches one or many attributes to the passed attribute statement.
     *  To obtain the attribute values, it executes the mapper's script and returns attaches the returned value to the
     *  attribute.
     *  If the returned attribute is an Array or is iterable, the mapper will either return multiple attributes, or an
     *  attribute with multiple values. The variant chosen depends on the configuration of the mapper
     *
     * @param attributeStatement The attribute statements to be added to a token
     * @param mappingModel The mapping model reflects the values that are actually input in the GUI
     * @param session The current session
     * @param userSession The current user session
     * @param clientSession The current client session
     */
    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel,
                                            KeycloakSession session, UserSessionModel userSession,
                                            AuthenticatedClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String scriptSource = getScriptCode(mappingModel);
        RealmModel realm = userSession.getRealm();

        String single = mappingModel.getConfig().get(SINGLE_VALUE_ATTRIBUTE);
        boolean singleAttribute = Boolean.parseBoolean(single);

        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        ScriptModel scriptModel = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, "attribute-mapper-script_" + mappingModel.getName(), scriptSource, null);

        EvaluatableScriptAdapter script = scripting.prepareEvaluatableScript(scriptModel);
        Object attributeValue;
        try {
            attributeValue = script.eval((bindings) -> {
                bindings.put("user", user);
                bindings.put("realm", realm);
                bindings.put("clientSession", clientSession);
                bindings.put("userSession", userSession);
                bindings.put("keycloakSession", session);
            });
            //If the result is a an array or is iterable, get all values
            if (attributeValue.getClass().isArray()){
                attributeValue = Arrays.asList((Object[])attributeValue);
            }
            if (attributeValue instanceof Iterable) {
                if (singleAttribute) {
                    AttributeType singleAttributeType = AttributeStatementHelper.createAttributeType(mappingModel);
                    attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(singleAttributeType));
                    for (Object value : (Iterable)attributeValue) {
                        singleAttributeType.addAttributeValue(value);
                    }
                } else {
                    for (Object value : (Iterable)attributeValue) {
                        AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, value.toString());
                    }
                }
            } else {
                // single value case
                AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, attributeValue.toString());
            }
        } catch (Exception ex) {
            LOGGER.error("Error during execution of ProtocolMapper script", ex);
            AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, null);
        }
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {

        String scriptCode = getScriptCode(mapperModel);
        if (scriptCode == null) {
            return;
        }

        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        ScriptModel scriptModel = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, mapperModel.getName() + "-script", scriptCode, "");

        try {
            scripting.prepareEvaluatableScript(scriptModel);
        } catch (ScriptCompilationException ex) {
            throw new ProtocolMapperConfigException("error", "{0}", ex.getMessage());
        }
    }

    protected String getScriptCode(ProtocolMapperModel mappingModel) {
        return mappingModel.getConfig().get(ProviderConfigProperty.SCRIPT_TYPE);
    }

    /**
     * Creates an protocol mapper model for the this script based mapper. This mapper model is meant to be used for
     * testing, as normally such objects are created in a different manner through the keycloak GUI.
     *
     * @param name The name of the mapper (this has no functional use)
     * @param samlAttributeName The name of the attribute in the SAML attribute
     * @param nameFormat can be "basic", "URI reference" or "unspecified"
     * @param friendlyName a display name, only useful for the keycloak GUI
     * @param script the javascript to be executed by the mapper
     * @param singleAttribute If true, all groups will be stored under one attribute with multiple attribute values
     * @return a Protocol Mapper for a group mapping
     */
    public static ProtocolMapperModel create(String name, String samlAttributeName, String nameFormat, String friendlyName, String script, boolean singleAttribute) {
        ProtocolMapperModel mapper =  AttributeStatementHelper.createAttributeMapper(name, null, samlAttributeName, nameFormat, friendlyName,
                PROVIDER_ID);
        Map<String, String> config = mapper.getConfig();
        config.put(ProviderConfigProperty.SCRIPT_TYPE, script);
        config.put(SINGLE_VALUE_ATTRIBUTE, Boolean.toString(singleAttribute));
        return mapper;
    }
}
