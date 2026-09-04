/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.email.aws.credentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.keycloak.email.aws.AwsHttpRequest;
import org.keycloak.email.aws.AwsHttpResponse;
import org.keycloak.email.aws.AwsHttpTransport;
import org.keycloak.email.aws.AwsRegion;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Credentials for a Kubernetes service account — EKS IRSA and anything else that projects an OIDC
 * token into the pod: the token on disk is exchanged at STS, through {@code AssumeRoleWithWebIdentity},
 * for the temporary credentials of {@code AWS_ROLE_ARN}.
 * <p>
 * The exchange is deliberately <em>unsigned</em>, and that is the whole point of the flow rather than
 * an omission: the signed OIDC token is itself the proof of identity, and this is the source that
 * exists precisely because the process holds no AWS key to sign with.
 * <p>
 * STS speaks the AWS query protocol, which has no JSON rendering, so this is the only place in the
 * provider that parses XML. The parser is hardened before it touches a byte: an answer that arrives
 * here is only as trustworthy as the DNS and TLS path to {@code sts.amazonaws.com}, and a
 * {@link DocumentBuilderFactory} at its defaults turns a spoofed answer into arbitrary file reads and
 * outbound requests from the Keycloak process.
 */
public final class WebIdentityTokenCredentialsProvider implements AwsCredentialsProvider {

    private static final Logger logger = Logger.getLogger(WebIdentityTokenCredentialsProvider.class);

    private static final String TOKEN_FILE_VARIABLE = "AWS_WEB_IDENTITY_TOKEN_FILE";
    private static final String ROLE_ARN_VARIABLE = "AWS_ROLE_ARN";
    private static final String SESSION_NAME_VARIABLE = "AWS_ROLE_SESSION_NAME";

    /** The same three settings as JVM system properties, which the AWS SDKs read first. */
    private static final String TOKEN_FILE_PROPERTY = "aws.webIdentityTokenFile";
    private static final String ROLE_ARN_PROPERTY = "aws.roleArn";
    private static final String SESSION_NAME_PROPERTY = "aws.roleSessionName";
    private static final String REGION_VARIABLE = "AWS_REGION";
    private static final String DEFAULT_REGION_VARIABLE = "AWS_DEFAULT_REGION";

    /**
     * The session name ends up in CloudTrail and in the assumed-role ARN, so it is a fixed, telling
     * string rather than something random: every Keycloak-sent email is then attributable to this
     * provider without correlating anything.
     */
    private static final String DEFAULT_SESSION_NAME = "keycloak-email-aws-ses";

    private static final String GLOBAL_STS_ENDPOINT = "https://sts.amazonaws.com/";
    private static final String STS_API_VERSION = "2011-06-15";
    private static final String DISALLOW_DOCTYPE_DECLARATION = "http://apache.org/xml/features/disallow-doctype-decl";

    /**
     * STS is a public-internet service rather than a link-local endpoint, so it gets a wider budget
     * than the metadata sources — but a credential lookup runs inside a Keycloak transaction and may
     * never hang: five seconds each way, then fail.
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 5000;

    /**
     * Rethrows rather than letting the parser's default handler print to {@code System.err}: that
     * bypasses the server log, and prints in the JVM's locale. The failure is not lost — it becomes
     * the cause of the {@link AwsCredentialsException} thrown below.
     */
    private static final ErrorHandler STRICT_ERROR_HANDLER = new ErrorHandler() {

        @Override
        public void warning(SAXParseException exception) {
            logger.debugf("Ignoring a warning while parsing the STS response: %s", exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    };

    private final AwsEnvironment environment;

    public WebIdentityTokenCredentialsProvider(AwsEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
        String tokenFile = environment.setting(TOKEN_FILE_PROPERTY, TOKEN_FILE_VARIABLE);
        String roleArn = environment.setting(ROLE_ARN_PROPERTY, ROLE_ARN_VARIABLE);
        if (tokenFile == null || roleArn == null) {
            return null;
        }

        // Read on every refresh rather than once: the kubelet rotates the projected token well
        // before it expires, and STS stops accepting the content this process read at boot.
        String token = readToken(tokenFile);
        String sessionName = environment.setting(SESSION_NAME_PROPERTY, SESSION_NAME_VARIABLE);
        String body = "Action=AssumeRoleWithWebIdentity"
                + "&Version=" + STS_API_VERSION
                + "&RoleArn=" + formEncode(roleArn)
                + "&RoleSessionName=" + formEncode(sessionName == null ? DEFAULT_SESSION_NAME : sessionName)
                + "&WebIdentityToken=" + formEncode(token);

        URI endpoint = stsEndpoint();
        AwsHttpRequest request = new AwsHttpRequest("POST", endpoint,
                Map.of("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"),
                body.getBytes(StandardCharsets.UTF_8), CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);

        logger.debugf("Assuming %s at %s with the web identity token from %s", roleArn, endpoint, tokenFile);
        AwsHttpResponse response;
        try {
            response = transport.exchange(request);
        } catch (IOException e) {
            throw new AwsCredentialsException("STS AssumeRoleWithWebIdentity call to " + endpoint + " failed", e);
        }
        if (!response.isSuccessful()) {
            throw new AwsCredentialsException(failureMessage(roleArn, response));
        }
        return credentials(parse(response.body()));
    }

    @Override
    public String name() {
        return "web identity token (" + TOKEN_FILE_VARIABLE + ")";
    }

    /**
     * Reads and trims the projected token. Every failure here is fatal rather than a fall-through:
     * the variables say this pod was given a service account identity, so an unreadable or empty
     * token is a broken deployment, not an absent source.
     */
    private static String readToken(String tokenFile) throws AwsCredentialsException {
        String token;
        try {
            // Trimmed because the projected file ends with a newline, and STS rejects the token
            // with an opaque InvalidIdentityToken if that newline is sent along.
            token = Files.readString(Paths.get(tokenFile)).trim();
        } catch (IOException | InvalidPathException e) {
            throw new AwsCredentialsException(TOKEN_FILE_VARIABLE + " points at " + tokenFile
                    + ", which cannot be read", e);
        }
        if (token.isEmpty()) {
            throw new AwsCredentialsException(TOKEN_FILE_VARIABLE + " points at " + tokenFile + ", which is empty");
        }
        return token;
    }

    private URI stsEndpoint() throws AwsCredentialsException {
        String region = environment.value(REGION_VARIABLE);
        if (region == null) {
            region = environment.value(DEFAULT_REGION_VARIABLE);
        }
        if (region != null && !AwsRegion.isValid(region)) {
            // This value is interpolated into the hostname of a request whose body carries the
            // service-account token. A region of "attacker.example/collect" would otherwise build
            // https://sts.attacker.example/collect.amazonaws.com/ and post the token there.
            throw new AwsCredentialsException(REGION_VARIABLE + " is not a valid AWS region name: " + region);
        }
        // The regional endpoint is preferred over the global one for the reason AWS gives: it keeps
        // the call inside the region, so an STS outage elsewhere cannot stop this server sending mail.
        String endpoint = region == null ? GLOBAL_STS_ENDPOINT
                : "https://sts." + region + "." + AwsRegion.dnsSuffix(region) + "/";
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new AwsCredentialsException("Cannot build an STS endpoint for region " + region
                    + ", configured in " + REGION_VARIABLE + " or " + DEFAULT_REGION_VARIABLE, e);
        }
    }

    /**
     * Encodes one form parameter value. {@link URLEncoder} is correct here and
     * {@code AwsV4Signer.uriEncode} is not — the two solve mirror-image problems. The STS body is
     * {@code application/x-www-form-urlencoded}, where a space is {@code +} and {@code ~} is escaped;
     * those exact two rules are what makes {@code URLEncoder} wrong for a SigV4 canonical request,
     * which is RFC 3986. Do not "unify" them.
     */
    private static String formEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * The HTTP status and the STS error code, and deliberately nothing more: an AWS error body can
     * quote the request that produced it back at you, and that request carries the web identity token.
     */
    private static String failureMessage(String roleArn, AwsHttpResponse response) {
        String code = errorCode(response.body());
        return "STS refused AssumeRoleWithWebIdentity for " + roleArn + " with HTTP " + response.status()
                + (code == null ? "" : " (" + code + ")");
    }

    /** Best effort: an error body that does not parse must not mask the status being reported. */
    private static String errorCode(byte[] body) {
        try {
            Element code = firstElement(parse(body), "Code");
            return code == null ? null : code.getTextContent().trim();
        } catch (AwsCredentialsException e) {
            return null;
        }
    }

    private static AwsCredentials credentials(Document response) throws AwsCredentialsException {
        Element credentials = firstElement(response, "Credentials");
        if (credentials == null) {
            throw new AwsCredentialsException("STS returned no <Credentials> in its"
                    + " AssumeRoleWithWebIdentity response");
        }
        return new AwsCredentials(
                required(credentials, "AccessKeyId"),
                required(credentials, "SecretAccessKey"),
                required(credentials, "SessionToken"),
                expiration(required(credentials, "Expiration")));
    }

    /**
     * An unparseable expiry is an error, not a credential that never expires: treating it as
     * non-expiring would cache these credentials forever and start failing every email an hour later,
     * far away from the cause.
     */
    private static Instant expiration(String value) throws AwsCredentialsException {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new AwsCredentialsException("STS returned an Expiration that is not an ISO-8601 instant", e);
        }
    }

    private static String required(Element parent, String tagName) throws AwsCredentialsException {
        Element element = (Element) parent.getElementsByTagName(tagName).item(0);
        String value = element == null ? "" : element.getTextContent().trim();
        if (value.isEmpty()) {
            throw new AwsCredentialsException("STS returned no <" + tagName + "> in its"
                    + " AssumeRoleWithWebIdentity response");
        }
        return value;
    }

    private static Element firstElement(Document document, String tagName) {
        return (Element) document.getElementsByTagName(tagName).item(0);
    }

    /**
     * Parses an STS response with external entities, DTDs and XInclude all switched off. Every one of
     * these settings is load-bearing, and removing any of them is what {@code refusesAnXxePayload…}
     * in the test exists to catch: whoever can answer for the STS endpoint would otherwise read files
     * off this host, or make it issue requests of their choosing, through a declared entity.
     */
    private static Document parse(byte[] xml) throws AwsCredentialsException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(DISALLOW_DOCTYPE_DECLARATION, true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(STRICT_ERROR_HANDLER);
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException e) {
            // IllegalArgumentException because setAttribute is allowed to refuse an attribute it does
            // not recognise: on a JAXP implementation other than the JDK's it would otherwise escape
            // unchecked, out of a method whose callers are promised an AwsCredentialsException.
            // The message stays out of the exception: it is the untrusted document talking.
            throw new AwsCredentialsException("STS returned a response that is not parseable XML", e);
        }
    }
}
