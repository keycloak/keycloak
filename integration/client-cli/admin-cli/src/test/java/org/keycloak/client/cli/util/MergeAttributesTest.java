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
package org.keycloak.client.cli.util;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.client.admin.cli.CmdStdinContext;
import org.keycloak.client.admin.cli.ReflectionUtil;
import org.keycloak.client.cli.common.AttributeOperation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.keycloak.client.cli.common.AttributeOperation.Type.DELETE;
import static org.keycloak.client.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.cli.util.OutputUtil.MAPPER;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class MergeAttributesTest {

    @Test
    public void testMergeAttrs() throws Exception {

        List<AttributeOperation> attrs = new LinkedList<>();
        attrs.add(new AttributeOperation(SET, "realm", "nurealm"));
        attrs.add(new AttributeOperation(SET, "enabled", "true"));
        attrs.add(new AttributeOperation(SET, "revokeRefreshToken", "true"));
        attrs.add(new AttributeOperation(SET, "accessTokenLifespan", "900"));
        attrs.add(new AttributeOperation(SET, "smtpServer.host", "localhost"));
        attrs.add(new AttributeOperation(SET, "extra.key1", "somevalue"));
        attrs.add(new AttributeOperation(SET, "extra.key2", "[\"somevalue\"]"));
        attrs.add(new AttributeOperation(SET, "extra.key3[1]", "second item"));
        attrs.add(new AttributeOperation(SET, "extra.key4", "\"true\""));
        attrs.add(new AttributeOperation(SET, "extra.key5", "\"1000\""));
        attrs.add(new AttributeOperation(DELETE, "id"));
        attrs.add(new AttributeOperation(DELETE, "attributes.\"_browser_header.xFrameOptions\""));

        String localJSON = "{\n" +
                "  \"id\" : \"24e5d572-756a-435b-8b2b-edbd0a7aa93d\",\n" +
                "  \"realm\" : \"demorealm\",\n" +
                "  \"notBefore\" : 0,\n" +
                "  \"revokeRefreshToken\" : false,\n" +
                "  \"accessTokenLifespan\" : 300,\n" +
                "  \"defaultRoles\" : [ \"offline_access\", \"uma_authorization\" ],\n" +
                "  \"smtpServer\" : { },\n" +
                "  \"attributes\" : {\n" +
                "    \"_browser_header.xFrameOptions\" : \"SAMEORIGIN\",\n" +
                "    \"_browser_header.contentSecurityPolicy\" : \"frame-src 'self'\"\n" +
                "  }\n" +
                "}";

        ObjectNode localNode = MAPPER.readValue(localJSON.getBytes(StandardCharsets.UTF_8), ObjectNode.class);
        CmdStdinContext<JsonNode> ctx = new CmdStdinContext<>();
        ctx.setResult(localNode);

        ctx = CmdStdinContext.mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
        System.out.println(ctx);

        String remoteJSON = "{\n" +
                "  \"id\" : \"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\n" +
                "  \"realm\" : \"demorealm\",\n" +
                "  \"notBefore\" : 0,\n" +
                "  \"revokeRefreshToken\" : false,\n" +
                "  \"accessTokenLifespan\" : 300,\n" +
                "  \"defaultRoles\" : [ \"uma_authorization\" ],\n" +
                "  \"remote\" : \"value\",\n" +
                "  \"attributes\" : {\n" +
                "    \"_browser_header.xFrameOptions\" : \"SAMEORIGIN\",\n" +
                "    \"_browser_header.x\" : \"ORIGIN\",\n" +
                "    \"_browser_header.contentSecurityPolicy\" : \"frame-src 'self'\"\n" +
                "  }\n" +
                "}";

        ObjectNode remoteNode = MAPPER.readValue(remoteJSON.getBytes(StandardCharsets.UTF_8), ObjectNode.class);
        CmdStdinContext<ObjectNode> ctxremote = new CmdStdinContext<>();
        ctxremote.setResult(remoteNode);

        ReflectionUtil.merge(ctx.getResult(), ctxremote.getResult());
        System.out.println(ctx);

        //ctx = mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
    }
}
