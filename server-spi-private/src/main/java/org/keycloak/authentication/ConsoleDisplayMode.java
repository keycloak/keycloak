package org.keycloak.authentication;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class encapsulates a proprietary HTTP challenge protocol designed by keycloak team which is used by text-based console
 * clients to dynamically render and prompt for information in a textual manner.  The class is a builder which can
 * build the challenge response (the header and response body).
 *
 * When doing code to token flow in OAuth, server could respond with
 *
 * 401
 * WWW-Authenticate: X-Text-Form-Challenge callback="http://localhost/..."
 *                                         param="username" label="Username: " mask=false
 *                                         param="password" label="Password: " mask=true
 * Content-Type: text/plain
 *
 * Please login with your username and password
 *
 *
 * The client receives this challenge.  It first outputs whatever the text body of the message contains.  It will
 * then prompt for username and password using the label values as prompt messages for each parameter.
 *
 * After the input has been entered by the user, the client does a form POST to the callback url with the values of the
 * input parameters entered.
 *
 * The server can challenge with 401 as many times as it wants.  The client will look for 302 responses.  It will will
 * follow all redirects unless the Location url has an OAuth "code" parameter.  If there is a code parameter, then the
 * client will stop and finish the OAuth flow to obtain a token.  Any other response code other than 401 or 302 the client
 * should abort with an error message.
 *
 */
public class ConsoleDisplayMode {

    /**
     * Browser is required to login.  This will abort client from doing a console login.
     *
     * @param session
     * @return
     */
    public static Response browserRequired(KeycloakSession session) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "X-Text-Form-Challenge browserRequired")
                .type(MediaType.TEXT_PLAIN)
                .entity("\n" + session.getProvider(LoginFormsProvider.class).getMessage("browserRequired") + "\n").build();
    }

    /**
     * Browser is required to continue login.  This will prompt client on whether to continue with a browser or abort.
     *
     * @param session
     * @param callback
     * @return
     */
    public static Response browserContinue(KeycloakSession session, String callback) {
        String browserContinueMsg = session.getProvider(LoginFormsProvider.class).getMessage("browserContinue");
        String browserPrompt = session.getProvider(LoginFormsProvider.class).getMessage("browserContinuePrompt");
        String answer = session.getProvider(LoginFormsProvider.class).getMessage("browserContinueAnswer");

        String header = "X-Text-Form-Challenge callback=\"" + callback + "\"";
        header += " browserContinue=\"" + browserPrompt + "\" answer=\"" + answer + "\"";
        return Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", header)
                .type(MediaType.TEXT_PLAIN)
                .entity("\n" + browserContinueMsg + "\n").build();
    }



    /**
     * Build challenge response for required actions
     *
     * @param context
     * @return
     */
    public static ConsoleDisplayMode challenge(RequiredActionContext context) {
        return new ConsoleDisplayMode(context);

    }

    /**
     * Build challenge response for authentication flows
     *
     * @param context
     * @return
     */
    public static ConsoleDisplayMode challenge(AuthenticationFlowContext context) {
        return new ConsoleDisplayMode(context);

    }
    /**
     * Build challenge response header only for required actions
     *
     * @param context
     * @return
     */
    public static HeaderBuilder header(RequiredActionContext context) {
        return new ConsoleDisplayMode(context).header();

    }

    /**
     * Build challenge response header only for authentication flows
     *
     * @param context
     * @return
     */
    public static HeaderBuilder header(AuthenticationFlowContext context) {
        return new ConsoleDisplayMode(context).header();

    }
    ConsoleDisplayMode(RequiredActionContext requiredActionContext) {
        this.requiredActionContext = requiredActionContext;
    }

    ConsoleDisplayMode(AuthenticationFlowContext flowContext) {
        this.flowContext = flowContext;
    }


    protected RequiredActionContext requiredActionContext;
    protected AuthenticationFlowContext flowContext;
    protected HeaderBuilder header;

    /**
     * Create a theme form pre-populated with challenge
     *
     * @return
     */
    public LoginFormsProvider form() {
        if (header == null) throw new RuntimeException("Header Not Set");
        return formInternal()
                .setStatus(Response.Status.UNAUTHORIZED)
                .setMediaType(MediaType.TEXT_PLAIN_TYPE)
                .setResponseHeader(HttpHeaders.WWW_AUTHENTICATE, header.build());
    }

    /**
     * Create challenge response with a  body generated from localized
     * message.properties of your theme
     *
     * @param msg message id
     * @param params parameters to use to format the message
     *
     * @return
     */
    public Response message(String msg, String... params) {
        if (header == null) throw new RuntimeException("Header Not Set");
        Response response = Response.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, header.build())
                .type(MediaType.TEXT_PLAIN)
                .entity("\n" + formInternal().getMessage(msg, params) + "\n").build();
        return response;
    }

    /**
     * Create challenge response with a text message body
     *
     * @param text plain text of http response body
     *
     * @return
     */
    public Response text(String text) {
        if (header == null) throw new RuntimeException("Header Not Set");
        Response response = Response.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, header.build())
                .type(MediaType.TEXT_PLAIN)
                .entity("\n" + text + "\n").build();
        return response;

    }


    /**
     * Generate response with empty http response body
     *
     * @return
     */
    public Response response() {
        if (header == null) throw new RuntimeException("Header Not Set");
        Response response = Response.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, header.build()).build();
        return response;

    }



    protected LoginFormsProvider formInternal() {
        if (requiredActionContext != null) {
            return requiredActionContext.form();
        } else {
            return flowContext.form();

        }
    }

    /**
     * Start building the header
     *
     * @return
     */
    public HeaderBuilder header() {
        String callback;
        if (requiredActionContext != null) {
            callback = requiredActionContext.getActionUrl(true).toString();
        } else {
            callback = flowContext.getActionUrl(flowContext.generateAccessCode(), true).toString();

        }
        header = new HeaderBuilder(callback);
        return header;
    }

    public class HeaderBuilder {
        protected StringBuilder builder = new StringBuilder();

        protected HeaderBuilder(String callback) {
            builder.append("X-Text-Form-Challenge callback=\"").append(callback).append("\" ");
        }

        protected ParamBuilder param;

        protected void checkParam() {
            if (param != null) {
                param.buildInternal();
                param = null;
            }
        }

        /**
         * Build header string
         *
         * @return
         */
        public String build() {
            checkParam();
            return builder.toString();
        }

        /**
         * Define a param
         *
         * @param name
         * @return
         */
        public ParamBuilder param(String name) {
            checkParam();
            builder.append("param=\"").append(name).append("\" ");
            param = new ParamBuilder(name);
            return param;
        }

        public class ParamBuilder {
            protected boolean mask;
            protected String label;

            protected ParamBuilder(String name) {
                this.label = name;
            }

            public ParamBuilder label(String msg) {
                this.label = formInternal().getMessage(msg);
                return this;
            }

            public ParamBuilder labelText(String txt) {
                this.label = txt;
                return this;
            }

            /**
             * Should input be masked by the client.  For example, when entering password, you don't want to show password on console.
             *
             * @param mask
             * @return
             */
            public ParamBuilder mask(boolean mask) {
                this.mask = mask;
                return this;
            }

            public void buildInternal() {
                builder.append("label=\"").append(label).append(" \" ");
                builder.append("mask=").append(mask).append(" ");
            }

            /**
             * Build header string
             *
             * @return
             */
            public String build() {
                return HeaderBuilder.this.build();
            }

            public ConsoleDisplayMode challenge() {
                return ConsoleDisplayMode.this;
            }

            public LoginFormsProvider form() {
                return ConsoleDisplayMode.this.form();
            }

            public Response message(String msg, String... params) {
                return ConsoleDisplayMode.this.message(msg, params);
            }

            public Response text(String text) {
                return ConsoleDisplayMode.this.text(text);

            }

            public ParamBuilder param(String name) {
                return HeaderBuilder.this.param(name);
            }
        }
    }
}
