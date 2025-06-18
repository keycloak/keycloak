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
package org.keycloak.common.util;

import java.util.Map;


/**
 * Helper class to do a browser redirect via a POST.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 * @deprecated Class is deprecated and may be removed in the future. Use org.keycloak.saml.BaseSAML2BindingBuilder#buildHtml instead
 */
@Deprecated
public class HttpPostRedirect {

    /**
     * Generate an HTML page that does a browser redirect via a POST.  The HTML document uses Javascript to automatically
     * submit a FORM post when loaded.
     *
     * This is similar to what the SAML Post Binding does.
     *
     * Here's an example
     *
     * <pre>
     * {@code
     * <HTML>
     *   <HEAD>
     *       <TITLE>title</TITLE>
     *   </HEAD>
     *   <BODY Onload="document.forms[0].submit()">
     *       <FORM METHOD="POST" ACTION="actionUrl">
     *           <INPUT TYPE="HIDDEN" NAME="param" VALUE="value"/>
     *           <NOSCRIPT>
     *               <P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>
     *               <INPUT TYPE="SUBMIT" VALUE="CONTINUE"/>
     *           </NOSCRIPT>
     *       </FORM>
     *   </BODY>
     * </HTML>
     * }
     * </pre>

     *
     * @param title may be null.  Just the title of the HTML document
     * @param actionUrl URL to redirect to
     * @param params must be encoded so that they can be placed in an HTML form hidden INPUT field value
     * @return
     */
    public String buildHtml(String title, String actionUrl, Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        builder.append("<HTML>")
                .append("<HEAD>");
        if (title != null) {
            builder.append("<TITLE>SAML HTTP Post Binding</TITLE>");
        }
        builder.append("</HEAD>")
                .append("<BODY Onload=\"document.forms[0].submit()\">")

                .append("<FORM METHOD=\"POST\" ACTION=\"")
                .append(HtmlUtils.escapeAttribute(actionUrl))
                .append("\">");
        for (Map.Entry<String, String> param : params.entrySet()) {
            builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"").append(param.getKey()).append("\"").append(" VALUE=\"")
                    .append(HtmlUtils.escapeAttribute(param.getValue())).append("\"/>");
        }


        builder.append("<NOSCRIPT>")
                .append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>")
                .append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />")
                .append("</NOSCRIPT>")

                .append("</FORM></BODY></HTML>");

        return builder.toString();
    }

}
