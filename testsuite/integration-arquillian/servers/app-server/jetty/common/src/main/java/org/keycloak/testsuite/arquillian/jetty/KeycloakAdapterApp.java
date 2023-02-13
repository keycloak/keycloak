package org.keycloak.testsuite.arquillian.jetty;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.deploy.App;
import org.jboss.shrinkwrap.api.Archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeycloakAdapterApp extends App {

    public static final String WEB_XML_PATH = "/WEB-INF/web.xml";

    private static final Pattern modulePattern = Pattern.compile("<module-name>(.*)</module-name>");
    private static final Pattern authMethodPattern = Pattern.compile("<auth-method>(.*)</auth-method>");

    private final boolean usesOIDCAuthenticator;
    private final boolean usesSAMLAuthenticator;
    private final boolean usesJaxrs;
    private final String applicationName;

    public KeycloakAdapterApp(App app, Archive<?> archive) {
        super(app.getDeploymentManager(), app.getAppProvider(), app.getOriginId());
        boolean usesJaxRS = false;
        boolean usesOIDCAuthenticator = false;
        boolean usesSAMLAuthenticator = false;
        String applicationName = archive.getName();
        if (archive.contains(WEB_XML_PATH)) {
            try {
                try (InputStream is = archive.get(WEB_XML_PATH).getAsset().openStream()) {
                    String webXml = IOUtils.toString(is, StandardCharsets.UTF_8);

                    usesJaxRS = webXml.contains("javax.ws.rs.core.Application");

                    for(String line : webXml.split("\n")) {
                        line = line.trim();
                        if (!usesOIDCAuthenticator && !usesSAMLAuthenticator) {
                            Matcher m = authMethodPattern.matcher(line);
                            if (m.find()) {
                                String authMethod = m.group(1);
                                switch (authMethod) {
                                    case "KEYCLOAK": {
                                        usesOIDCAuthenticator = true;
                                        break;
                                    }
                                    case "KEYCLOAK-SAML": {
                                        usesSAMLAuthenticator = true;
                                        break;
                                    }
                                    default: {
                                        throw new IllegalArgumentException("Unknown auth-method" + authMethod);
                                    }


                                }
                            }
                        }

                        Matcher m = modulePattern.matcher(line.trim());
                        if (m.find()) {
                            applicationName = m.group(1);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
        this.applicationName = applicationName;
        this.usesJaxrs = usesJaxRS;
        this.usesOIDCAuthenticator = usesOIDCAuthenticator;
        this.usesSAMLAuthenticator = usesSAMLAuthenticator;
    }

    public boolean usesOIDCAuthenticator() {
       return usesOIDCAuthenticator;
    }

    public boolean usesJaxrs() {
        return usesJaxrs;
    }

    public boolean usesSAMLAuthenticator() {
        return usesSAMLAuthenticator;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
