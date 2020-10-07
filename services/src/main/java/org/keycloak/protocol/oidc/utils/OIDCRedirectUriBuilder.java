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

package org.keycloak.protocol.oidc.utils;

import org.keycloak.common.util.Encode;
import org.keycloak.common.util.HtmlUtils;
import org.keycloak.common.util.KeycloakUriBuilder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class OIDCRedirectUriBuilder {

    protected final KeycloakUriBuilder uriBuilder;

    protected OIDCRedirectUriBuilder(KeycloakUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    public abstract OIDCRedirectUriBuilder addParam(String paramName, String paramValue);

    public abstract Response build();


    public static OIDCRedirectUriBuilder fromUri(String baseUri, OIDCResponseMode responseMode) {
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(baseUri);

        switch (responseMode) {
            case QUERY: return new QueryRedirectUriBuilder(uriBuilder);
            case FRAGMENT: return new FragmentRedirectUriBuilder(uriBuilder);
            case FORM_POST: return new FormPostRedirectUriBuilder(uriBuilder);
        }

        throw new IllegalStateException("Not possible to end here");
    }


    // Impl subclasses


    // http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#ResponseModes
    private static class QueryRedirectUriBuilder extends OIDCRedirectUriBuilder {

        protected QueryRedirectUriBuilder(KeycloakUriBuilder uriBuilder) {
            super(uriBuilder);
        }

        @Override
        public OIDCRedirectUriBuilder addParam(String paramName, String paramValue) {
            uriBuilder.queryParam(paramName, paramValue);
            return this;
        }

        @Override
        public Response build() {
            URI redirectUri = uriBuilder.build();
            Response.ResponseBuilder location = Response.status(302).location(redirectUri);
            return location.build();
        }
    }


    // http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#ResponseModes
    private static class FragmentRedirectUriBuilder extends OIDCRedirectUriBuilder {

        private StringBuilder fragment;

        protected FragmentRedirectUriBuilder(KeycloakUriBuilder uriBuilder) {
            super(uriBuilder);

            String fragment = uriBuilder.getFragment();
            if (fragment != null) {
                this.fragment = new StringBuilder(fragment);
            }
        }

        @Override
        public OIDCRedirectUriBuilder addParam(String paramName, String paramValue) {
            String param = paramName + "=" + Encode.encodeQueryParamAsIs(paramValue);
            if (fragment == null) {
                fragment = new StringBuilder(param);
            } else {
                fragment.append("&").append(param);
            }
            return this;
        }

        @Override
        public Response build() {
            if (fragment != null) {
                uriBuilder.encodedFragment(fragment.toString());
            }
            URI redirectUri = uriBuilder.build();

            Response.ResponseBuilder location = Response.status(302).location(redirectUri);
            return location.build();
        }

    }


    // http://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html
    private static class FormPostRedirectUriBuilder extends OIDCRedirectUriBuilder {

        private Map<String, String> params = new HashMap<>();

        protected FormPostRedirectUriBuilder(KeycloakUriBuilder uriBuilder) {
            super(uriBuilder);
        }

        @Override
        public OIDCRedirectUriBuilder addParam(String paramName, String paramValue) {
            params.put(paramName, paramValue);
            return this;
        }

        @Override
        public Response build() {
            StringBuilder builder = new StringBuilder();
            URI redirectUri = uriBuilder.build();

            builder.append("<HTML>");
            builder.append("  <HEAD>");
            builder.append("    <TITLE>OIDC Form_Post Response</TITLE>");
            builder.append("  </HEAD>");
            builder.append("  <BODY Onload=\"document.forms[0].submit()\">");

            builder.append("    <FORM METHOD=\"POST\" ACTION=\"" + redirectUri.toString() + "\">");

            for (Map.Entry<String, String> param : params.entrySet()) {
                builder.append("  <INPUT TYPE=\"HIDDEN\" NAME=\"")
                        .append(param.getKey())
                        .append("\" VALUE=\"")
                        .append(HtmlUtils.escapeAttribute(param.getValue()))
                        .append("\" />");
            }

            builder.append("      <NOSCRIPT>");
            builder.append("        <P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue .</P>");
            builder.append("        <INPUT name=\"continue\" TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />");
            builder.append("      </NOSCRIPT>");
            builder.append("    </FORM>");
            builder.append("  </BODY>");
            builder.append("</HTML>");

            return Response.status(Response.Status.OK)
                    .type(MediaType.TEXT_HTML_TYPE)
                    .entity(builder.toString()).build();
        }

    }


}
