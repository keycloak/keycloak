package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptExecutionException;
import org.keycloak.scripting.ScriptingProvider;

import java.util.*;

public class ScriptMapperHelper {
    public static void setConfigProperties(List<ProviderConfigProperty> configProperties) {
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
    }

    public static Object executeScript(ProtocolMapperModel mappingModel, 
                                       KeycloakSession session, 
                                       UserSessionModel userSession, 
                                       AuthenticatedClientSessionModel clientSession,
                                       String providerId) throws ScriptExecutionException {
        UserModel user = userSession.getUser();
        RealmModel realm = userSession.getRealm();

        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        String scriptSource = mappingModel.getConfig().get(ProviderConfigProperty.SCRIPT_TYPE);
        ScriptModel scriptModel = scripting.createScript(realm.getId(), 
                                                         ScriptModel.TEXT_JAVASCRIPT, 
                                                         providerId + "_script_" + mappingModel.getName(), 
                                                         scriptSource, 
                                                         null);
        EvaluatableScriptAdapter script = scripting.prepareEvaluatableScript(scriptModel);

        return script.eval((bindings) -> {
            bindings.put("user", user);
            bindings.put("realm", realm);
            bindings.put("clientSession", clientSession);
            bindings.put("userSession", userSession);
            bindings.put("keycloakSession", session);
        });
    }
}
