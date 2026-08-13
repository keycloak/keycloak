/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.client.admin.cli;

import java.io.IOException;
import java.util.List;

import org.keycloak.client.cli.common.AttributeOperation;
import org.keycloak.client.cli.util.AttributeException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.client.admin.cli.ReflectionUtil.setAttributes;
import static org.keycloak.client.cli.util.IoUtil.readFileOrStdin;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class CmdStdinContext<T> {

    private T result;
    private String content;

    public CmdStdinContext() {}

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static CmdStdinContext<JsonNode> parseFileOrStdin(String file) {
    
        String content = readFileOrStdin(file).trim();
        JsonNode result = null;
    
        if (content.length() == 0) {
            throw new RuntimeException("Document provided by --file option is empty");
        }
    
        try {
            result = JsonSerialization.readValue(content, JsonNode.class);
        } catch (JsonParseException e) {
            throw new RuntimeException("Not a valid JSON document - " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the input document as JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Not a valid JSON document", e);
        }
    
        CmdStdinContext<JsonNode> ctx = new CmdStdinContext<>();
        ctx.setContent(content);
        ctx.setResult(result);
        return ctx;
    }

    public static <T> CmdStdinContext<JsonNode> mergeAttributes(CmdStdinContext<JsonNode> ctx, ObjectNode newObject, List<AttributeOperation> attrs) {
    
        JsonNode node = ctx.getResult();
        if (node != null && !node.isObject()) {
            throw new RuntimeException("Not a JSON object: " + node);
        }
        ObjectNode result = (ObjectNode) node;
        try {
    
            if (result == null) {
                result = newObject;
            }
    
            if (result == null) {
                throw new RuntimeException("Failed to set attribute(s) - no target object");
            }
    
            try {
                setAttributes(result, attrs);
            } catch (AttributeException e) {
                throw new RuntimeException("Failed to set attribute '" + e.getAttributeName() + "' on document type '" + result.getClass().getName() + "'", e);
            }
            ctx.setContent(JsonSerialization.writeValueAsString(result));
    
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge attributes with configuration from file", e);
        }
    
        ctx.setResult(result);
        return ctx;
    }
}
