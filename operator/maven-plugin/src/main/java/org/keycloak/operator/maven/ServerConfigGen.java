package org.keycloak.operator.maven;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.utils.StringEscapeUtils;
import org.apache.maven.plugin.logging.Log;
import org.keycloak.config.AllOptions;
import org.keycloak.config.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ServerConfigGen {

    public static final String STRING_TYPE = "java.lang.String";
    public static final String FILE_TYPE = "java.io.File";
    public static final String LIST_TYPE = "java.util.List";
    public static final String VALUE_OR_SECRET = "org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret";
    public static final String SECRET_KEY_SELECTOR = "io.fabric8.kubernetes.api.model.SecretKeySelector";

    public static final String ANNOTATION_JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
    public static final String ANNOTATION_JSON_PROPERTY_DESCRIPTION = "com.fasterxml.jackson.annotation.JsonPropertyDescription";

    public void generate(Log log, File destination) {
        String className = "ServerConfig";
        String[] packageName = new String[]{"org", "keycloak", "operator", "codegen", "configuration"};
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration(String.join(".", packageName));

        ClassOrInterfaceDeclaration serverConfig = cu.addClass(className);

        Path dest = Paths.get(destination.getAbsolutePath(), packageName);
        dest.toFile().mkdirs();

        AllOptions.ALL_OPTIONS.forEach(o -> {
            if (o.getSupportedRuntimes().contains(Option.Runtime.OPERATOR)) {
                String fieldName = toCamelCase(o.getKey());
                String fieldType = typeConversion(o.getType().getCanonicalName());

                FieldDeclaration field = serverConfig.addField(fieldType, fieldName, Modifier.Keyword.PRIVATE);
                field.addSingleMemberAnnotation(
                        ANNOTATION_JSON_PROPERTY,
                        new StringLiteralExpr(o.getKey()));
                if (o.getDescription() != null) {
                    field.addSingleMemberAnnotation(
                            ANNOTATION_JSON_PROPERTY_DESCRIPTION,
                            new StringLiteralExpr(StringEscapeUtils.escapeJava(o.getDescription())));
                }
                field.createGetter();
                field.createSetter();
            }
        });

        writeToFile(dest.resolve(className + ".java").toFile(), cu.toString());
    }

    private String typeConversion(String originalType) {
        if (originalType.equals(STRING_TYPE)) {
            return VALUE_OR_SECRET;
        } else if (originalType.equals(FILE_TYPE)) {
            return SECRET_KEY_SELECTOR;
        } else if (originalType.equals(LIST_TYPE)) {
            return STRING_TYPE; // FIXME
        } else {
            return originalType;
        }
    }

    private void writeToFile(File file, String str) {
        try (FileWriter fileWriter = new FileWriter(file);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.println(str);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toCamelCase(String original) {
        boolean convertNext = false;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            char currentChar = original.charAt(i);
            if (currentChar == '-') {
                convertNext = true;
            } else if (convertNext) {
                builder.append(Character.toUpperCase(currentChar));
                convertNext = false;
            } else {
                builder.append(Character.toLowerCase(currentChar));
            }
        }
        return builder.toString();
    }

}
