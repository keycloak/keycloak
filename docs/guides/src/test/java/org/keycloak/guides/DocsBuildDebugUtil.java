package org.keycloak.guides;

import freemarker.template.TemplateException;
import org.keycloak.guides.maven.GuideBuilder;

import java.io.File;
import java.io.IOException;

public class DocsBuildDebugUtil {

    public static void main(String[] args) throws IOException, TemplateException {
        File usrDir = new File(System.getProperty("user.dir"));

        for (File srcDir: usrDir.toPath().resolve("docs/guides").toFile().listFiles(d -> d.isDirectory() && !d.getName().equals("templates"))) {
            if (srcDir.getName().equals("target") || srcDir.getName().equals("src")) {
                // those are standard maven folders, ignore them
                continue;
            }
            File targetDir = usrDir.toPath().resolve("target/generated-guides/" + srcDir.getName()).toFile();
            targetDir.mkdirs();
            GuideBuilder builder = new GuideBuilder(srcDir, targetDir, null);
            builder.build();
            System.out.println("Guides generated to: " + targetDir.getAbsolutePath().toString());
        }
    }

}
