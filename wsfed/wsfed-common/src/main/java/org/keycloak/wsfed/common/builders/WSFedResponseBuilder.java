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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.keycloak.saml.common.util.StringUtil.isNotNull;

public class WSFedResponseBuilder {
    protected String destination;
    protected String action;
    protected String realm;
    protected String context;
    protected String replyTo;
    protected String method = HttpMethod.GET;

    public String getDestination() {
        return destination;
    }

    public WSFedResponseBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public String getAction() {
        return action;
    }

    public WSFedResponseBuilder setAction(String action) {
        this.action = action;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public WSFedResponseBuilder setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public String getContext() {
        return context;
    }

    public WSFedResponseBuilder setContext(String context) {
        this.context = context;
        return this;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public WSFedResponseBuilder setReplyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public WSFedResponseBuilder setMethod(String method) {
        this.method = method;
        return this;
    }

    public Response buildResponse(String result) {
        String str = buildHtml(destination, action, result, realm, context);

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        return Response.ok(str, MediaType.TEXT_HTML_TYPE)
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache, no-store").build();
    }

    protected String buildHtml(String destination, String action, String result, String realm, String context) {
        StringBuilder builder = new StringBuilder();

        builder.append("<HTML>");
        builder.append("<HEAD>");

        builder.append("<TITLE>HTTP Binding Response (Response)</TITLE>");
        builder.append("</HEAD>");
        builder.append("<BODY Onload=\"document.forms[0].submit()\">");

        builder.append(String.format("<FORM METHOD=\"%s\" ACTION=\"%s\">", method, destination));

        if (isNotNull(action)) {
            builder.append(String.format("<INPUT TYPE=\"HIDDEN\" NAME=\"%s\" VALUE=\"%s\" />", WSFedConstants.WSFED_ACTION, action));
        }

        if (isNotNull(realm)) {
            builder.append(String.format("<INPUT TYPE=\"HIDDEN\" NAME=\"%s\" VALUE=\"%s\" />", WSFedConstants.WSFED_REALM, realm));
        }

        if (isNotNull(result)) {
            builder.append(String.format("<INPUT TYPE=\"HIDDEN\" NAME=\"%s\" VALUE=\"%s\" />", WSFedConstants.WSFED_RESULT, escapeAttribute(result)));
        }

        if (isNotNull(replyTo)) {
            builder.append(String.format("<INPUT TYPE=\"HIDDEN\" NAME=\"%s\" VALUE=\"%s\" />", WSFedConstants.WSFED_REPLY, replyTo));
        }

        if (isNotNull(context)) {
            builder.append(String.format("<INPUT TYPE=\"HIDDEN\" NAME=\"%s\" VALUE=\"%s\" />", WSFedConstants.WSFED_CONTEXT, context));
        }

        builder.append("<NOSCRIPT>");
        builder.append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>");
        builder.append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />");
        builder.append("</NOSCRIPT>");

        builder.append("</FORM></BODY></HTML>");

        return builder.toString();
    }

    protected static String escapeAttribute(String s) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>') {
                out.append("&#" + (int) c + ";");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
