package org.keycloak.broker.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptingProvider;


public class ScriptMapper extends AbstractIdentityProviderMapper implements EnvironmentDependentProviderFactory {

    private static final List<ProviderConfigProperty> configProperties;
    public static final String PROVIDER_ID = "javascript-idp-mapper";
    public static final String SINGLE_VALUE_ATTRIBUTE = "single";
    private static final Logger LOGGER = Logger.getLogger(ScriptMapper.class);
    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));


    /*
     * This static property block is used to determine the elements available to the mapper. This is determinant
     * both for the frontend (gui elements in the mapper) and for the backend.
     */
    static {
        configProperties = new ArrayList<>();
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.SCRIPT_TYPE);
        property.setLabel(ProviderConfigProperty.SCRIPT_TYPE);
        property.setName(ProviderConfigProperty.SCRIPT_TYPE);
        property.setHelpText(
                "Script to compute the attribute value. \n" + //
                        " Available variables: \n" + //
                        " 'realm' - the current realm.\n" + //
                        " 'brokeredIdentityContext' - the brokeredIdentityContext (contains info about idp, mapper, saml assertion, claims).\n" + //
                        " 'keycloakSession' - the current keycloakSession.\n" + //
                        " 'userModel' - User model.\\n\n" //
        );
        property.setDefaultValue("/*\n" +
                "  Available variables: \n" +
                "    realmModel - the current realm\n" +
                "    brokeredIdentityContext - the brokeredIdentityContext (contains info about idp, mapper, saml assertion, claims)\n" +
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
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.SCRIPTS);
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

        String scriptSource = getScriptCode(mapperModel);

        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        ScriptModel scriptModel = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, "idp2user-attribute-mapper-script_" + mapperModel.getName(), scriptSource, null);

        EvaluatableScriptAdapter script = scripting.prepareEvaluatableScript(scriptModel);
        try {
            script.eval(bindings -> {
                bindings.put("realmModel", realm);
                bindings.put("keycloakSession", session);
                bindings.put("brokeredIdentityContext", context);
                bindings.put("userModel", user);
            });
        } catch (Exception ex) {
            LOGGER.error(String.format("Error during execution of ScriptMapper's javascript (realm: %s, idp_mapper:  %s)", realm.getName(), mapperModel.getName()), ex);
        }

    }

    protected String getScriptCode(IdentityProviderMapperModel mapperModel){
        return mapperModel.getConfig().get(ProviderConfigProperty.SCRIPT_TYPE);
    }

    @Override
    public String getHelpText() {
        return "Write a custom script to create the desired mapper. i.e perform conditional mapping, attribute aggregations, etc";
    }


}

