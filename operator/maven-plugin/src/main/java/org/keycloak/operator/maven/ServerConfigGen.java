package org.keycloak.operator.maven;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.StringEscapeUtils;
import org.apache.maven.plugin.logging.Log;
import org.keycloak.config.AllOptions;
import org.keycloak.config.MultiOption;
import org.keycloak.config.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ServerConfigGen {

    private static final String VALUE = "value";

    public static final String STRING_TYPE = "java.lang.String";
    public static final String FILE_TYPE = "java.io.File";
    public static final String LIST_TYPE = "java.util.List";
    public static final String ENV_VAR_TYPE = "io.fabric8.kubernetes.api.model.EnvVar";
    public static final String VALUE_OR_SECRET = "org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret";
    public static final String SECRET_KEY_SELECTOR = "io.fabric8.kubernetes.api.model.SecretKeySelector";

    public static final String ANNOTATION_JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
    public static final String ANNOTATION_JSON_PROPERTY_DESCRIPTION = "com.fasterxml.jackson.annotation.JsonPropertyDescription";

    public void generate(Log log, File destination) {
        String className = "ServerConfig";
        String[] packageName = new String[]{"org", "keycloak", "operator", "codegen", "configuration"};
        CompilationUnit cu = new CompilationUnit();
        CompilationUnit enumCu = new CompilationUnit();
        cu.setPackageDeclaration(String.join(".", packageName));

        ClassOrInterfaceDeclaration serverConfig = cu.addClass(className);

        Path dest = Paths.get(destination.getAbsolutePath(), packageName);
        dest.toFile().mkdirs();

        StringBuilder envVarsBody = new StringBuilder();
        envVarsBody.append("{" + LIST_TYPE + "<" + ENV_VAR_TYPE + "> all = new java.util.ArrayList<>();");

        StringBuilder allWatchedSecrets = new StringBuilder();
        allWatchedSecrets.append("{" + LIST_TYPE + "<" + STRING_TYPE + "> all = new java.util.ArrayList<>();");

        StringBuilder allMountSecrets = new StringBuilder();
        allMountSecrets.append("{" + LIST_TYPE + "<" + SECRET_KEY_SELECTOR + "> all = new java.util.ArrayList<>();");

        AllOptions.ALL_OPTIONS.forEach(o -> {
            if (o.getSupportedRuntimes().contains(Option.Runtime.OPERATOR)) {
                String fieldName = toCamelCase(o.getKey());
                Class auxiliaryType = (o instanceof MultiOption) ? ((MultiOption)o).getAuxiliaryType() : null;
                String fieldType = typeConversion(o.getType(), auxiliaryType, o.getExpectedValues(), serverConfig, enumCu);

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

                // Create an "EnvVar" extractor
                MethodDeclaration getEnvVar = serverConfig.addMethod(fieldName + "EnvVar", Modifier.Keyword.PRIVATE);
                getEnvVar.setType(ENV_VAR_TYPE);
                getEnvVar.setBody(new BlockStmt().addStatement(
                        "return org.keycloak.operator.controllers.KeycloakDeployment.getEnvVar(\"" + o.getKey() + "\", " + fieldName + ");"
                ));

                envVarsBody.append("all.add(" + fieldName + "EnvVar());\n");

                // Create a "Watched Secrets" extractor
                MethodDeclaration getWatchedSecret = serverConfig.addMethod(fieldName + "WatchedSecret", Modifier.Keyword.PRIVATE);
                getWatchedSecret.setType(STRING_TYPE);
                getWatchedSecret.setBody(new BlockStmt().addStatement(
                        "return org.keycloak.operator.controllers.KeycloakDeployment.getWatchedSecret(" + fieldName + ");"
                ));

                allWatchedSecrets.append("all.add(" + fieldName + "WatchedSecret());\n");

                // Create a "Secrets to be mounted" extractor
                MethodDeclaration getMountSecret = serverConfig.addMethod(fieldName + "MountSecret", Modifier.Keyword.PRIVATE);
                getMountSecret.setType(SECRET_KEY_SELECTOR);
                getMountSecret.setBody(new BlockStmt().addStatement(
                        "return org.keycloak.operator.controllers.KeycloakDeployment.getMountSecret(" + fieldName + ");"
                ));

                allMountSecrets.append("all.add(" + fieldName + "MountSecret());\n");
            }
        });

        MethodDeclaration getAllEnvVars = serverConfig.addMethod("getAllEnvVars", Modifier.Keyword.PUBLIC);
        getAllEnvVars.setType(LIST_TYPE + "<" + ENV_VAR_TYPE + ">");
        envVarsBody.append("all.removeAll(java.util.Collections.singleton(null));return all;}");
        getAllEnvVars.setBody(new BlockStmt().addStatement(envVarsBody.toString()));

        MethodDeclaration getAllWatchedSecrets = serverConfig.addMethod("getAllWatchedSecrets", Modifier.Keyword.PUBLIC);
        getAllWatchedSecrets.setType(LIST_TYPE + "<" + STRING_TYPE + ">");
        allWatchedSecrets.append("all.removeAll(java.util.Collections.singleton(null));return all;}");
        getAllWatchedSecrets.setBody(new BlockStmt().addStatement(allWatchedSecrets.toString()));

        MethodDeclaration getAllMountSecrets = serverConfig.addMethod("getAllMountSecrets", Modifier.Keyword.PUBLIC);
        getAllMountSecrets.setType(LIST_TYPE + "<" + SECRET_KEY_SELECTOR + ">");
        allMountSecrets.append("all.removeAll(java.util.Collections.singleton(null));return all;}");
        getAllMountSecrets.setBody(new BlockStmt().addStatement(allMountSecrets.toString()));

        writeToFile(dest.resolve(className + ".java").toFile(), cu.toString());
    }

    private String typeConversion(Class<?> t, Class<?> auxiliaryType, List<String> expectedValues, ClassOrInterfaceDeclaration serverConfig, CompilationUnit enumCu) {
        if (t.isEnum() && expectedValues != null && expectedValues.size() > 0) {
            String fieldType = t.getSimpleName();
            if (enumCu.getEnumByName(fieldType).isPresent()) {
                return fieldType;
            }
            EnumDeclaration en = enumCu.addEnum(fieldType);

            expectedValues.forEach(ev -> {
                EnumConstantDeclaration ed = en.addEnumConstant(ev.replace("-", "_"));
                // TODO: check that this works as expected with Features or DB
                if (ev.contains("-")) {
                    ed.addAnnotation(
                        new SingleMemberAnnotationExpr(
                            new Name(ANNOTATION_JSON_PROPERTY),
                            new StringLiteralExpr(ev)));
                }
            });
            serverConfig.addMember(en);
            return fieldType;
        } else if (t.getCanonicalName().equals(STRING_TYPE)) {
            return VALUE_OR_SECRET;
        } else if (t.getCanonicalName().equals(FILE_TYPE)) {
            return SECRET_KEY_SELECTOR;
        } else if (t.getCanonicalName().equals(LIST_TYPE)) {
            return LIST_TYPE + "<" + typeConversion(auxiliaryType, null, expectedValues, serverConfig, enumCu) + ">";
        } else {
            return t.getCanonicalName();
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
