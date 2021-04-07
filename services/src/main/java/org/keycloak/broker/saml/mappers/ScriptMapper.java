package org.keycloak.broker.saml.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.saml.mappers.AbstractSAMLProtocolMapper;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptCompilationException;
import org.keycloak.scripting.ScriptingProvider;

import java.util.*;


public class ScriptMapper extends AbstractIdentityProviderMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    public static final String PROVIDER_ID = "saml-javascript-idp-mapper";
    private static final String SINGLE_VALUE_ATTRIBUTE = "single";
    private static final Logger LOGGER = Logger.getLogger(ScriptMapper.class);
    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));


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
                        " 'realm' - the current realm.\n" + //
                        " 'brokeredIdentityContext' - the brokeredIdentityContext (contains info about idp, mapper, saml assertion).\n" + //
                        " 'keycloakSession' - the current keycloakSession.\n\n" //
        );
        property.setDefaultValue("/*\n" +
                "  Available variables: \n" +
                "    realmModel - the current realm\n" +
                "    brokeredIdentityContext - the brokeredIdentityContext (contains info about idp, mapper, saml assertion)\n" +
                "    keycloakSession - the current keycloakSession\n" +
                "    userModel - the userModel, if this is not the first login of the user. If it's the first login of the user, this variable is null\n\n" +
                "  If the userModel is null, you should set the user attribute on the brokeredIdentityContext. i.e.: \n" +
                "    brokeredIdentityContext.setUserAttribute('my-attrib', 'attribval'); \n" +
                "  if the userModel is not null, then it's an update on an existing user, so add the user attribute on userModel. i.e: \n" +
                "    userModel.setSingleAttribute('my-attrib', 'newattribval') */ \n\n" +
                "//Insert your code below... \n"
        );
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(SINGLE_VALUE_ATTRIBUTE);
        property.setLabel("Single Value Attribute");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("If true, all values will be stored under one attribute with multiple attribute values.");
        configProperties.add(property);

    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Javascript Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Javascript Mapper";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateBrokeredUser(session, realm, null, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        String scriptSource = mapperModel.getConfig().get(ProviderConfigProperty.SCRIPT_TYPE);

        String single = mapperModel.getConfig().get(SINGLE_VALUE_ATTRIBUTE);
        boolean singleAttribute = Boolean.parseBoolean(single);

        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        ScriptModel scriptModel = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, "idp2user-attribute-mapper-script_" + mapperModel.getName(), scriptSource, null);

        EvaluatableScriptAdapter script = scripting.prepareEvaluatableScript(scriptModel);
        try {
            script.eval((bindings) -> {
                bindings.put("realmModel", realm);
                bindings.put("keycloakSession", session);
                bindings.put("brokeredIdentityContext", context);
                bindings.put("userModel", user);
            });
        } catch (Exception ex) {
            LOGGER.error(String.format("Error during execution of ScriptMapper's javascript (realm: %s, idp_mapper:  %s)", realm.getName(), mapperModel.getName()), ex);
        }

    }

    @Override
    public String getHelpText() {
        return "Write a custom script to create the desired mapper. i.e perform conditional mapping, attribute aggregations, etc";
    }


}
