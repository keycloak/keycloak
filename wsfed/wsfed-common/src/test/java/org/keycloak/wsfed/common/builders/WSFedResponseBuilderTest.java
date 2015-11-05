/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.wsfed.common.builders;

import org.keycloak.wsfed.common.WSFedConstants;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.keycloak.wsfed.common.TestHelpers.*;

public class WSFedResponseBuilderTest {

    @Test
    public void testAccessors() throws Exception {
        WSFedResponseBuilder builder = new WSFedResponseBuilder()
                .setAction("ACTION")
                .setDestination("DESTINATION")
                .setRealm("REALM")
                .setContext("CONTEXT")
                .setReplyTo("REPLYTO")
                .setMethod("METHOD");

        assertEquals("ACTION", builder.getAction());
        assertEquals("DESTINATION", builder.getDestination());
        assertEquals("REALM", builder.getRealm());
        assertEquals("CONTEXT", builder.getContext());
        assertEquals("REPLYTO", builder.getReplyTo());
        assertEquals("METHOD", builder.getMethod());
    }

    @Test
    public void testEscapeAttribute() throws Exception {
        WSFedResponseBuilder builder = new WSFedResponseBuilder();

        String value = builder.escapeAttribute(new String(new char[] { (char)128 }));
        assertEquals("&#" + 128 + ";", value);

        value = builder.escapeAttribute(new String("\""));
        assertEquals("&#" + (int)'"' + ";", value);

        value = builder.escapeAttribute(new String("<"));
        assertEquals("&#" + (int)'<' + ";", value);

        value = builder.escapeAttribute(new String(">"));
        assertEquals("&#" + (int)'>' + ";", value);

        value = builder.escapeAttribute(new String("D"));
        assertEquals("D", value);
    }

    @Test
    public void testBuildResponse() throws Exception {
        WSFedResponseBuilder builder = new WSFedResponseBuilder()
                .setAction("ACTION")
                .setDestination("DESTINATION")
                .setRealm("REALM")
                .setContext("CONTEXT")
                .setReplyTo("REPLYTO")
                .setMethod("METHOD");

        Response response = builder.buildResponse("RESULT");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("no-cache", response.getMetadata().getFirst("Pragma"));
        assertEquals("no-cache, no-store", response.getMetadata().getFirst("Cache-Control"));

        Document doc = responseToDocument(response);

        assertFormAction(doc, "METHOD", "DESTINATION");
        assertInputNode(doc, WSFedConstants.WSFED_ACTION, "ACTION");
        assertInputNode(doc, WSFedConstants.WSFED_REALM, "REALM");
        assertInputNode(doc, WSFedConstants.WSFED_RESULT, "RESULT");
        assertInputNode(doc, WSFedConstants.WSFED_REPLY, "REPLYTO");
        assertInputNode(doc, WSFedConstants.WSFED_CONTEXT, "CONTEXT");
    }

    @Test
    public void testBuildResponseEmpty() throws Exception {
        WSFedResponseBuilder builder = new WSFedResponseBuilder();

        Response response = builder.buildResponse(null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("no-cache", response.getMetadata().getFirst("Pragma"));
        assertEquals("no-cache, no-store", response.getMetadata().getFirst("Cache-Control"));

        Document doc = responseToDocument(response);

        assertFormAction(doc, "GET", "null");
        assertInputNodeMissing(doc, WSFedConstants.WSFED_ACTION);
        assertInputNodeMissing(doc, WSFedConstants.WSFED_REALM);
        assertInputNodeMissing(doc, WSFedConstants.WSFED_RESULT);
        assertInputNodeMissing(doc, WSFedConstants.WSFED_REPLY);
        assertInputNodeMissing(doc, WSFedConstants.WSFED_CONTEXT);
    }
}