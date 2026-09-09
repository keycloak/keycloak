package org.keycloak.testframework.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.it.utils.Maven;
import org.keycloak.testframework.FatalTestClassException;
import org.keycloak.testframework.util.FileUtils;
import org.keycloak.testframework.util.MavenProjectUtil;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import org.jboss.logging.Logger;

final class ProviderDeployer {

    private final Logger log;
    private final File providersDir;
    private final boolean hotDeployEnabled;
    private final Set<KeycloakDependency> requestedDependencies;
    private final Set<SingleTestProvider> providerFactories;
    private final String REL_SERVICES_PATH = "META-INF" + File.separator + "services" + File.separator;

    ProviderDeployer(Logger log, File keycloakHomeDir, KeycloakServerConfigBuilder serverConfig, boolean hotDeployEnabled) {
        this.log = log;
        this.providersDir = new File(keycloakHomeDir, "providers");
        this.requestedDependencies = serverConfig.toDependencies();
        this.hotDeployEnabled = hotDeployEnabled;
        this.providerFactories = serverConfig.toProviderFactories();
    }

    boolean updateDependencies() throws IOException {
        boolean anyDependenciesModified = deleteNotRequestedDependencies();

        for (KeycloakDependency d : requestedDependencies) {
            boolean shouldPackageClasses = hotDeployEnabled && d.isHotDeployable();

            String jarName = getDependencyJarName(d);

            Path dependencyPath = getDependencyPath(d);
            Path targetPath = providersDir.toPath().resolve(jarName);

            File targetFile = targetPath.toFile();

            long dependencyLastModified = getMostRecentModification(dependencyPath);
            File targetLastModifiedFile = new File(targetFile.getAbsolutePath() + ".lastModified");
            long targetLastModified = targetLastModifiedFile.isFile() ? FileUtils.readLongFromFile(targetLastModifiedFile) : -1;

            if (dependencyLastModified != targetLastModified || !targetFile.isFile()) {
                log.trace("Adding or overwriting existing provider: " + targetPath.toFile().getAbsolutePath());

                if (shouldPackageClasses || d.dependencyCurrentProject()) {
                    MavenProjectUtil.buildJar(jarName, dependencyPath, targetPath);
                } else {
                    Files.copy(dependencyPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.writeString(targetLastModifiedFile.toPath(), Long.toString(dependencyLastModified));
                anyDependenciesModified = true;
            }
        }
        return anyDependenciesModified;
    }

    private String getDependencyJarName(KeycloakDependency dependency) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();

        if (dependency.dependencyCurrentProject()) {
            LocalProject project = MavenProjectUtil.getCurrentModule();

            groupId = project.getGroupId();
            artifactId = project.getArtifactId();
        }

        return groupId + "__" + artifactId + ".jar";
    }

    private boolean deleteNotRequestedDependencies() {
        Set<String> requestedJarNames = requestedDependencies.stream()
                .map(this::getDependencyJarName)
                .collect(Collectors.toSet());

        List<File> toDelete = listExistingDependencies().stream()
                .filter(f -> !requestedJarNames.contains(f.getName()))
                .toList();

        for (File f : toDelete) {
            String path = f.getAbsolutePath();
            log.trace("Deleted non-requested provider: " + path);
            FileUtils.delete(f);
            FileUtils.delete(new File(path + ".lastModified"));
        }

        return !toDelete.isEmpty();
    }

    private List<File> listExistingDependencies() {
        if (providersDir.isDirectory()) {
            File[] files = providersDir.listFiles(n -> n.getName().endsWith(".jar"));
            if (files != null) {
                return Arrays.stream(files).toList();
            }
        }
        return List.of();
    }

    private Path getDependencyPath(KeycloakDependency d) {
        if (d.dependencyCurrentProject()) {
            return MavenProjectUtil.getCurrentModule().getClassesDir();
        }

        if (d.isHotDeployable() && hotDeployEnabled) {
            return MavenProjectUtil.findLocalModule(d.getGroupId(), d.getArtifactId()).getClassesDir();
        }

        return Maven.resolveArtifact(d.getGroupId(), d.getArtifactId());
    }

    private long getMostRecentModification(Path path) throws IOException {
        File file = path.toFile();
        if (!file.exists()) {
            return 0;
        }

        if (file.isFile()) {
            return file.lastModified();
        }

        try (Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().lastModified())
                    .max()
                    .orElse(0);
        }
    }

    public void deployProviderFactories() {
        for (SingleTestProvider testProvider : providerFactories) {
            try {
                Set<Class<?>> classesToDeploy = new HashSet<>();
                List<String> resourceFiles = Arrays.stream(testProvider.resourceFiles()).toList();

                Class<?> providerFactory = testProvider.providerFactory();
                classesToDeploy.add(providerFactory);

                List<Class<?>> providerFactoryIFaces = getAllIFaces(providerFactory);
                classesToDeploy.addAll(providerFactoryIFaces);
                Set<String> potentialServiceFiles = getPotentialServiceFiles(providerFactoryIFaces);

                Class<?> provider = getProviderFromFactory(providerFactory);
                try {
                    classesToDeploy.add(provider.getDeclaredMethod("getResource").getReturnType());
                } catch (NoSuchMethodException ignored) {}
                classesToDeploy.add(provider);
                classesToDeploy.addAll(getAllIFaces(provider));

                if (testProvider.spi() != null) {
                    classesToDeploy.add(testProvider.spi());
                    potentialServiceFiles.add(REL_SERVICES_PATH + "org.keycloak.provider.Spi");
                }

                String jarName = provider.getSimpleName() + "-single-deploy.jar";
                Path targetPath = providersDir.toPath().resolve(jarName);
                Path classesDir = Path.of(providerFactory.getProtectionDomain().getCodeSource().getLocation().toURI());

                MavenProjectUtil.buildJar(jarName, classesDir, classesToDeploy, potentialServiceFiles, resourceFiles, targetPath);
            } catch (URISyntaxException e) {
                throw new FatalTestClassException("Failed to deploy: " + testProvider.providerFactory(), e);
            }
        }
    }

    private List<Class<?>> getAllIFaces(Class<?> providerFactory) {
        List<Class<?>> providerFactoryIFaces = List.of(providerFactory.getInterfaces());
        List<Class<?>> classesToDeploy = new ArrayList<>(providerFactoryIFaces);
        for (Class<?> iFace : providerFactoryIFaces) {
            classesToDeploy.addAll(Arrays.asList(iFace.getInterfaces()));
        }
        return classesToDeploy;
    }

    private Set<String> getPotentialServiceFiles(List<Class<?>> providerFactoryIFaces) {
        Set<String> potentialServiceFiles = new HashSet<>();
        for (Class<?> iFace : providerFactoryIFaces) {
            potentialServiceFiles.add(REL_SERVICES_PATH + iFace.getName());
        }
        return potentialServiceFiles;
    }

    private Class<?> getProviderFromFactory(Class<?> providerFactory) {
        for (Method method : providerFactory.getDeclaredMethods()) {
            if (method.getName().equals("create") && !method.isBridge()) {
                return method.getReturnType();
            }
        }
        throw new FatalTestClassException("Could not find provider of: " + providerFactory.getName() + ". ProviderFactory does not declare a create method"); // TODO prolly need a new exception
    }

}
