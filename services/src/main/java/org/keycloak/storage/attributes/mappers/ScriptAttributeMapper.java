/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptingProvider;
import org.keycloak.util.JsonSerialization;

import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Attribute mapper to perform custom javascript logic to attributes received from an external data store.
 */
public class ScriptAttributeMapper extends AbstractAttributeMapper {

    public ScriptAttributeMapper(ComponentModelScope config){
        super(config);
    }

    @Override
    public void transform(KeycloakSession session, Map<String, Object> source, Map<String, String> dest) {
        String destKey = getDestAttributeKey();
        String value = runScript(session, source);

        if (value == null){
            logger.warnf("user attribute mapper '%s': script mapper returned a value of null", config.getComponentName());
        }

        dest.put(destKey, value);
    }

    private String getDestAttributeKey(){
        return config.get(ScriptAttributeMapperFactory.CONFIG_ATTRIBUTE_DEST);
    }

    private String getScriptSource(){
        return config.get(ScriptAttributeMapperFactory.CONFIG_SCRIPT_SOURCE);
    }

    /**
     * Helper function to run the provided script and retrieve the results
     * @param session the keycloak session
     * @param source the attributes received from the external attribute store
     * @return the attribute value to set on the user
     */
    private String runScript(KeycloakSession session, Map<String, Object> source){
        ScriptingProvider scripting = session.getProvider(ScriptingProvider.class);
        RealmModel realm = session.getContext().getRealm();

        // create the script context
        ScriptModel scriptModel = scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, "attribute-mapper-script_" + config.getComponentName(), getScriptSource(), null);
        EvaluatableScriptAdapter script = scripting.prepareEvaluatableScript(scriptModel);

        // run the script and return the exported value
        Object attributeValue;
        try {
            attributeValue = script.eval((bindings) -> {
                bindings.put("keycloakSession", session);
                bindings.put("realm", realm);
                bindings.put("sourceAttributes", source);
            });

            // parse result as JSON
            JsonNode parsed = new ObjectMapper().convertValue(attributeValue, JsonNode.class);

            // return as string unless it is a nested type (ie array or object)
            if (parsed.isValueNode()) {
                return parsed.asText();
            } else {
                return JsonSerialization.writeValueAsString(parsed);
            }
        } catch (Exception ex) {
            logger.error("error during execution of ScriptAttributeMapper script", ex);
            return null;
        }
    }
}
