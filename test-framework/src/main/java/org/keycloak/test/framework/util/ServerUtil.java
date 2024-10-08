package org.keycloak.test.framework.util;

import org.keycloak.it.TestProvider;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerUtil {
    public static Path getProviderPackagePath(TestProvider provider) {
        String fullPathUrl = provider.getClasses()[0].getResource(".").toString();
        String packagePath = provider.getClasses()[0].getPackageName().replace('.', '/').concat("/");
        URL pathUrl;
        try {
            pathUrl = new URL(fullPathUrl.replace(packagePath, ""));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }

        File fileUri;
        try {
            fileUri = new File(pathUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid package provider path", e);
        }
        return Paths.get(fileUri.getPath());
    }
}
