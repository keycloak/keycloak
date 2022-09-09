package org.keycloak.protocol.saml.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.scripting.ScriptCompilationException;
import org.keycloak.scripting.ScriptExecutionException;
import org.keycloak.scripting.ScriptingProvider;

import java.util.List;
import java.util.ArrayList;

public class NameIdScriptBasedMapper extends AbstractSAMLProtocolMapper implements SAMLNameIdMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    public static final String PROVIDER_ID = "saml-javascript-nameid-mapper";
    private static final Logger LOGGER = Logger.getLogger(NameIdScriptBasedMapper.class);

    static {
        ScriptMapperHelper.setConfigProperties(configProperties);
        NameIdMapperHelper.setConfigProperties(configProperties);
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
        return "Javascript Mapper for NameID";
    }

    @Override
    public String getDisplayCategory() {
        return NameIdMapperHelper.NAMEID_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Evaluates a JavaScript function to produce a NameID value based on context information.";
    }

    @Override
    public String mapperNameId(String nameIdFormat, ProtocolMapperModel mappingModel, KeycloakSession session,
                               UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        Object nameId;
        try {
            nameId = ScriptMapperHelper.executeScript(mappingModel, session, userSession, clientSession, PROVIDER_ID);
            return String.valueOf(nameId);
        } catch (ScriptExecutionException ex) {
            LOGGER.error("Error during execution of ProtocolMapper script", ex);
            return "";
        }
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, 
                               ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        String scriptCode = mapperModel.getConfig().get(ProviderConfigProperty.SCRIPT_TYPE);
        if (scriptCode == null) {
            return;
        }

        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        ScriptModel scriptModel = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, 
                                                         mapperModel.getName() + "-script", scriptCode, "");

        try {
            scripting.prepareEvaluatableScript(scriptModel);
        } catch (ScriptCompilationException ex) {
            throw new ProtocolMapperConfigException("error", "{0}", ex.getMessage());
        }
    }
}
