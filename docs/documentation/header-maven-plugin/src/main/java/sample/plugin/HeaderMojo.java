package sample.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "header", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class HeaderMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "index", property = "masterFile", required = true)
    private String masterFileName;

    @Parameter(defaultValue = "${project.build.directory}/sources", property = "outputDir", required = true)
    private File outputDir;

    private File baseDir;

    private File topicsDir;

    public void execute() throws MojoExecutionException {
        try {
            baseDir = mavenProject.getBasedir();

            copy(new File(baseDir, masterFileName + ".adoc"));
            copy(new File(baseDir, "topics.adoc"));

            File docInfo = new File(baseDir, "docinfo.html");
            if (docInfo.isFile()) {
                copy(docInfo);
                copy(new File(baseDir, "docinfo-footer.html"));
            }

            topicsDir = new File(baseDir, "topics");

            processTopics(topicsDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void copy(File file) throws IOException {
        File out = new File(outputDir, file.getName());
        out.mkdirs();
        Files.copy(file.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void processTopics(File f) throws IOException {
        String topicsAbsolutePath = f.getAbsolutePath();
        String topicsParentDirPath = topicsDir.getParentFile().getAbsolutePath();
        if (isWindows()) {
            topicsAbsolutePath = topicsAbsolutePath.replace("\\", "/");
            topicsParentDirPath = topicsParentDirPath.replace("\\", "/");
        }
        File out = new File(outputDir, topicsAbsolutePath.replaceFirst(topicsParentDirPath, ""));

        if (f.isFile() && topicsAbsolutePath.contains("/templates/")) {
            out.getParentFile().mkdirs();
            Files.copy(f.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else if (f.isDirectory()) {
            File[] files = f.listFiles();
            if(files != null){
                for (File c : files) {
                    processTopics(c);
                }
            }
        } else if (f.getName().endsWith(".adoc")) {
            out.getParentFile().mkdirs();

            String filePath = f.getAbsolutePath().replace(baseDir.getParent(), "").substring(1);
            if (isWindows()) {
                filePath = filePath.replace("\\", "/");
            }
            String includeHeaderPath = filePath.substring(baseDir.getName().length() + 7);
            includeHeaderPath = includeHeaderPath.substring(0, includeHeaderPath.lastIndexOf('/'));
            includeHeaderPath = includeHeaderPath.replaceAll("/[^/]+", "../");
            includeHeaderPath = includeHeaderPath + "templates/header.adoc";

            String header = "\n\n:include_filename: " + filePath + "\ninclude::" + includeHeaderPath + "[]\n\n";
            try(PrintStream ps = new PrintStream(new FileOutputStream(out));BufferedReader br = new BufferedReader(new FileReader(f));){
                for (String l = br.readLine(); l != null; l = br.readLine()) {
                    ps.println(l);
                    if (l.startsWith("=")) {
                        break;
                    }
                }
                ps.print(header);

                for (String l = br.readLine(); l != null; l = br.readLine()) {
                    ps.println(l);
                }
            }
        }
    }
    private boolean isWindows(){
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().startsWith("windows");
    }
}
