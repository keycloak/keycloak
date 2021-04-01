package org.keycloak.testsuite.util;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.w3c.dom.Document;

import java.io.InputStream;

public class SamlUtils {
    public static SamlDeployment getSamlDeploymentForClient(String client) throws ParsingException {
        InputStream is = SamlUtils.class.getResourceAsStream("/adapter-test/keycloak-saml/" + client + "/WEB-INF/keycloak-saml.xml");

        // InputStream -> Document
        Document doc = IOUtil.loadXML(is);

        // Modify saml deployment the same way as before deploying to real app server
        DeploymentArchiveProcessorUtils.modifySAMLDocument(doc);

        // Document -> InputStream
        InputStream isProcessed = IOUtil.documentToInputStream(doc);

        // InputStream -> SamlDeployment
        ResourceLoader loader = new ResourceLoader() {
            @Override
            public InputStream getResourceAsStream(String resource) {
                return getClass().getResourceAsStream("/adapter-test/keycloak-saml/" + client + resource);
            }
        };
        return new DeploymentBuilder().build(isProcessed, loader);
    }
}
