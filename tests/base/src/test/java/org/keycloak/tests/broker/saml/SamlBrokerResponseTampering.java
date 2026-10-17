package org.keycloak.tests.broker.saml;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.utils.SecureContextResolver;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Drives a broker login over HTTP and edits the IdP's auto-submitted SAML response before it reaches the ACS. */
final class SamlBrokerResponseTampering {

    private static final String ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String BEARER_METHOD = "urn:oasis:names:tc:SAML:2.0:cm:bearer";

    enum Mutation {
        REMOVE_SUBJECT,
        REMOVE_BEARER_CONFIRMATION,
        REMOVE_SUBJECT_CONFIRMATION_DATA,
        REMOVE_NOT_ON_OR_AFTER,
        REMOVE_RECIPIENT
    }

    record Result(int statusCode, String body) {
    }

    private SamlBrokerResponseTampering() {
    }

    static Result loginWithMutation(OAuthClient oauth, ManagedCertificates certificates, String idpAlias, String username, String password,
            Mutation mutation) throws IOException {
        // Like browsers, allow secure cookies on trusted loopback HTTP origins.
        CookieSpecProvider cookieSpec = context -> new DefaultCookieSpec() {
            @Override
            public boolean match(Cookie cookie, CookieOrigin origin) {
                if (SecureContextResolver.isLocal(origin.getHost())) {
                    origin = new CookieOrigin(origin.getHost(), origin.getPort(), origin.getPath(), true);
                }
                return super.match(cookie, origin);
            }
        };
        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultCookieSpecRegistry(RegistryBuilder.<CookieSpecProvider>create().register("browser", cookieSpec).build())
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec("browser").build());
        if (certificates.isTlsEnabled()) {
            builder.setSSLContext(certificates.getClientSSLContext());
        }
        try (CloseableHttpClient client = builder.build()) {
            CookieStore cookieStore = new BasicCookieStore();
            String loginUrl = oauth.loginForm().param("kc_idp_hint", idpAlias).build();
            Page page = execute(client, cookieStore, new HttpGet(loginUrl));

            for (int step = 0; step < 10; step++) {
                List<HtmlForm> forms = parseForms(page.body());
                if (forms.isEmpty()) {
                    throw new IllegalStateException("Expected a form during SAML broker login, got HTTP " + page.statusCode()
                            + " at " + page.uri() + ": " + page.body());
                }
                HtmlForm form = forms.get(0);

                List<FormField> samlResponses = form.fields().stream()
                        .filter(field -> "SAMLResponse".equals(field.name()))
                        .toList();
                if (!samlResponses.isEmpty()) {
                    if (samlResponses.size() != 1) {
                        throw new IllegalStateException("Expected one SAMLResponse field");
                    }
                    FormField samlResponse = samlResponses.get(0);
                    samlResponse.value(mutate(samlResponse.value(), mutation));
                    return execute(client, cookieStore, submitForm(form, page.uri())).result();
                }

                FormField usernameField = form.fields().stream().filter(field -> "username".equals(field.id())).findFirst().orElse(null);
                FormField passwordField = form.fields().stream().filter(field -> "password".equals(field.id())).findFirst().orElse(null);
                if (usernameField != null && passwordField != null) {
                    usernameField.value(username);
                    passwordField.value(password);
                } else if (form.fields().stream().noneMatch(field -> "SAMLRequest".equals(field.name()))) {
                    throw new IllegalStateException("Unexpected form during SAML broker login: " + page.body());
                }

                page = execute(client, cookieStore, submitForm(form, page.uri()));
            }
            throw new IllegalStateException("SAML broker login did not produce a SAML response");
        }
    }

    private static Page execute(CloseableHttpClient client, CookieStore cookieStore,
            HttpUriRequest request) throws IOException {
        HttpUriRequest currentRequest = request;
        for (int redirect = 0; redirect < 10; redirect++) {
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(cookieStore);
            try (CloseableHttpResponse response = client.execute(currentRequest, context)) {
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                List<URI> redirects = context.getRedirectLocations();
                URI responseUri = redirects == null || redirects.isEmpty()
                        ? currentRequest.getURI()
                        : redirects.get(redirects.size() - 1);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 300 && statusCode < 400 && response.getFirstHeader("Location") != null) {
                    currentRequest = new HttpGet(responseUri.resolve(response.getFirstHeader("Location").getValue()));
                    continue;
                }
                return new Page(statusCode, responseUri, body);
            }
        }
        throw new IllegalStateException("Too many redirects while driving SAML broker login");
    }

    private static HttpUriRequest submitForm(HtmlForm form, URI currentUri) {
        URI action = currentUri.resolve(form.action());
        List<NameValuePair> parameters = new ArrayList<>();
        for (FormField input : form.fields()) {
            if (input.name() == null) {
                continue;
            }
            if (("checkbox".equalsIgnoreCase(input.type()) || "radio".equalsIgnoreCase(input.type())) && !input.checked()) {
                continue;
            }
            parameters.add(new BasicNameValuePair(input.name(), input.value()));
        }
        if ("get".equalsIgnoreCase(form.method())) {
            URIBuilder builder = new URIBuilder(action);
            parameters.forEach(parameter -> builder.addParameter(parameter.getName(), parameter.getValue()));
            try {
                return new HttpGet(builder.build());
            } catch (java.net.URISyntaxException e) {
                throw new IllegalArgumentException("Invalid form action " + action, e);
            }
        }
        HttpPost post = new HttpPost(action);
        post.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
        return post;
    }

    private static List<HtmlForm> parseForms(String html) {
        List<HtmlForm> forms = new ArrayList<>();
        try {
            new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
                private HtmlForm currentForm;

                @Override
                public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                    if (HTML.Tag.FORM.equals(tag)) {
                        currentForm = new HtmlForm(attribute(attributes, HTML.Attribute.ACTION, ""),
                                attribute(attributes, HTML.Attribute.METHOD, "get"));
                        forms.add(currentForm);
                    }
                }

                @Override
                public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                    if (HTML.Tag.INPUT.equals(tag) && currentForm != null) {
                        currentForm.fields().add(new FormField(
                                attribute(attributes, HTML.Attribute.NAME), attribute(attributes, HTML.Attribute.VALUE, ""),
                                attribute(attributes, HTML.Attribute.ID), attribute(attributes, HTML.Attribute.TYPE, "text"),
                                attributes.isDefined(HTML.Attribute.CHECKED)));
                    }
                }

                @Override
                public void handleEndTag(HTML.Tag tag, int position) {
                    if (HTML.Tag.FORM.equals(tag)) {
                        currentForm = null;
                    }
                }
            }, true);
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse SAML login form", e);
        }
        return forms;
    }

    private static String attribute(MutableAttributeSet attributes, HTML.Attribute attribute) {
        Object value = attributes.getAttribute(attribute);
        return value == null ? null : value.toString();
    }

    private static String attribute(MutableAttributeSet attributes, HTML.Attribute attribute, String defaultValue) {
        String value = attribute(attributes, attribute);
        return value == null ? defaultValue : value;
    }

    private static String mutate(String response, Mutation mutation) {
        try {
            byte[] xml = Base64.getDecoder().decode(response);
            org.w3c.dom.Document document = DocumentUtil.getDocument(new String(xml, StandardCharsets.UTF_8));
            Node assertion = document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
            Node subject = firstChild(assertion, "Subject");
            if (mutation == Mutation.REMOVE_SUBJECT) {
                assertion.removeChild(subject);
            } else {
                Node confirmation = firstChild(subject, "SubjectConfirmation");
                while (confirmation != null && (confirmation.getAttributes().getNamedItem("Method") == null
                        || !BEARER_METHOD.equals(confirmation.getAttributes().getNamedItem("Method").getNodeValue()))) {
                    confirmation = nextSibling(confirmation, "SubjectConfirmation");
                }
                if (mutation == Mutation.REMOVE_BEARER_CONFIRMATION) {
                    subject.removeChild(confirmation);
                } else {
                    Node data = firstChild(confirmation, "SubjectConfirmationData");
                    switch (mutation) {
                        case REMOVE_SUBJECT_CONFIRMATION_DATA -> confirmation.removeChild(data);
                        case REMOVE_NOT_ON_OR_AFTER -> data.getAttributes().removeNamedItem("NotOnOrAfter");
                        case REMOVE_RECIPIENT -> data.getAttributes().removeNamedItem("Recipient");
                        default -> throw new IllegalArgumentException("Unsupported mutation: " + mutation);
                    }
                }
            }
            String modified = DocumentUtil.getDocumentAsString(document);
            return Base64.getEncoder().encodeToString(modified.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Could not mutate SAML response", e);
        }
    }

    private static Node firstChild(Node parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (localName.equals(child.getLocalName())) {
                return child;
            }
        }
        throw new IllegalStateException("Missing SAML element " + localName);
    }

    private static Node nextSibling(Node node, String localName) {
        for (Node sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (localName.equals(sibling.getLocalName())) {
                return sibling;
            }
        }
        return null;
    }

    private record Page(int statusCode, URI uri, String body) {
        private Result result() {
            return new Result(statusCode, body);
        }
    }

    private record HtmlForm(String action, String method, List<FormField> fields) {
        private HtmlForm(String action, String method) {
            this(action, method, new ArrayList<>());
        }
    }

    private static final class FormField {
        private final String name;
        private String value;
        private final String id;
        private final String type;
        private final boolean checked;

        private FormField(String name, String value, String id, String type, boolean checked) {
            this.name = name;
            this.value = value;
            this.id = id;
            this.type = type;
            this.checked = checked;
        }

        private String value() {
            return value;
        }

        private void value(String value) {
            this.value = value;
        }

        private String name() {
            return name;
        }

        private String id() {
            return id;
        }

        private String type() {
            return type;
        }

        private boolean checked() {
            return checked;
        }
    }
}
