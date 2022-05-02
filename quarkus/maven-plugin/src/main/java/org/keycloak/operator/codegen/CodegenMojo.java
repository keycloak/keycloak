package org.keycloak.operator.codegen;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.utils.StringEscapeUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Map;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;

@Mojo(name = "keycloak-operator-codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter(property = "operator.codegen.target", defaultValue = "${basedir}/target/generated-sources/java")
    private File target;

    private Map<String, Set<Property>> configurations = new HashMap<>();

    private void addKeyValue(String k, Property v) {
        if (configurations.containsKey(k)) {
            Set<Property> tmpValue = configurations.get(k);
            tmpValue.add(v);
            configurations.put(k, tmpValue);
        } else {
            Set<Property> configs = new HashSet<>();
            configs.add(v);
            configurations.put(k, configs);
        }
    }

    private void writeToFile(File file, String str) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.println(str);
        }
    }

    @Override
    public void execute() throws MojoFailureException {
        try {
            Log log = getLog();

            // TODO: if custom image should we remove build time configurations?
            PropertyMappers.getMappers().forEach(c -> {
                addKeyValue(
                        c.getCategory().getHeading(),
                        new Property(
                                c.getCliFormat().replaceFirst("--", ""),
                                c.getType().getCanonicalName(),
                                c.getDescription())
                        );
            });

            Map<String, CompilationUnit> cus = new HashMap<>();

            CompilationUnit cu = new CompilationUnit();
            cu.setPackageDeclaration("org.keycloak.codegen.configuration");

            ClassOrInterfaceDeclaration serverConfig = cu.addClass("ServerConfig");
            cus.put("ServerConfig", cu);

            Path destination = Paths.get(target.getAbsolutePath(), "org", "keycloak", "codegen", "configuration");
            destination.toFile().mkdirs();

            configurations.forEach((k, v) -> {
                String className = k.replace("/", "_");
                serverConfig.addField(className, className.toLowerCase(Locale.ROOT));

                CompilationUnit innerCu = new CompilationUnit();
                innerCu.setPackageDeclaration("org.keycloak.codegen.configuration");
                ClassOrInterfaceDeclaration clazz = innerCu.addClass(className);
                cus.put(className, innerCu);

                v.forEach(p -> {
                    String fieldName = replaceNonAlphanumericByUnderscores(p.getName());

                    if (!fieldName.equals("null")) { // edge case in Proxy configuration
                        FieldDeclaration field = clazz.addField(p.getType(), fieldName, Modifier.Keyword.PUBLIC);
                        field.addSingleMemberAnnotation(
                                JsonProperty.class,
                                new StringLiteralExpr(p.getName()));
                        if (p.getDescription() != null) {
                            field.addSingleMemberAnnotation(
                                    JsonPropertyDescription.class,
                                    new StringLiteralExpr(StringEscapeUtils.escapeJava(p.getDescription())));
                        }
                    }
                });
            });

            cus.forEach((k, v) -> {
                try {
                    writeToFile(destination.resolve(k + ".java").toFile(), v.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to generate code", e);
        }
    }

}
