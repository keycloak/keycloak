package org.keycloak.guides;

import freemarker.template.TemplateException;
import org.keycloak.guides.maven.GuideBuilder;

import java.io.File;
import java.io.IOException;

public class DocsBuildDebugUtil {

    public static void main(String[] args) throws IOException, TemplateException {
        String userDir = System.getProperty("user.dir");
        File usrDir = new File(System.getProperty("user.dir"));
        File srcDir = usrDir.toPath().resolve("docs/guides/src/main").toFile();
        File targetDir = usrDir.toPath().resolve("target/generated-guides-tests").toFile();
        targetDir.mkdirs();
        GuideBuilder builder = new GuideBuilder(srcDir, targetDir, null);
        builder.build();
        System.out.println("Guides generated to: " + targetDir.getAbsolutePath().toString());
    }

}
