package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "verify-compatibility")
public class VerifyCompatibilityMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        try {
           Log log = getLog();
           File root = project.getBasedir();
           File sFile = new File(root, supportedFile);
           File uFile = new File(root, unsupportedFile);
           if (!sFile.exists() && !uFile.exists()) {
              log.info("No JSON ChangeSet files exist to verify");
              return;
           }

           // Parse JSON files to determine all committed ChangeSets
           ObjectMapper mapper = new ObjectMapper();
           List<ChangeSet> sChanges = mapper.readValue(sFile, new TypeReference<>() {});
           List<ChangeSet> uChanges = mapper.readValue(uFile, new TypeReference<>() {});
           Set<ChangeSet> recordedChanges = Stream.of(sChanges, uChanges)
                 .flatMap(List::stream)
                 .collect(Collectors.toSet());

           if (recordedChanges.isEmpty()) {
              log.info("No supported or unsupported ChangeSet exist in specified files");
              return;
           }

            Set<ChangeSet> intersection = new HashSet<>(sChanges);
            intersection.retainAll(uChanges);
            if (!intersection.isEmpty()) {
                log.error("The following ChangeSets should be defined in either the supported or unsupported file, they cannot appear in both:");
                intersection.forEach(change -> log.error("\t\t" + change.toString()));
                log.error("The offending ChangeSets should be removed from one of the files");
                throw new MojoExecutionException("One or more ChangeSet definitions exist in both the supported and unsupported file");
            }

           // Parse all ChangeSets currently defined in the jpa-changelog files
           ChangeLogXMLParser xmlParser = new ChangeLogXMLParser(classLoader());
           Set<ChangeSet> currentChanges = xmlParser.discoverAllChangeSets();
           if (recordedChanges.equals(currentChanges)) {
              log.info("All ChangeSets in the module recorded as expected in the supported and unsupported files");
           } else {
              log.error("The recorded ChangeSet JSON files differ from the current repository state");
              log.error("The following ChangeSets should be defined in either the supported '%s' or unsupported '%s' file:".formatted(sFile.toString(), uFile.toString()));
              currentChanges.removeAll(recordedChanges);
              currentChanges.forEach(change -> log.error("\t\t" + change.toString()));
              log.error("You must determine whether the ChangeSet(s) is compatible with rolling upgrades or not");
              log.error("A ChangeSet that requires locking preventing other cluster members accessing the database is NOT compatible with rolling upgrades");
              log.error("Rolling upgrade compatibility must be verified against all supported database vendors before the supported file is updated");
              log.error("If the change IS compatible, then it should be committed to the repository in the supported file: '%s'".formatted(sFile.toString()));
              log.error("If the change IS NOT compatible, then it should be committed to the repository in the unsupported file: '%s'".formatted(sFile.toString()));
              log.error("Adding a ChangeSet to the unsupported file ensures that a rolling upgrade is not attempted when upgrading to the first patch version containing the change");
              throw new MojoExecutionException("One or more ChangeSet definitions are missing from the supported or unsupported files");
           }
        } catch (Exception e) {
            throw new MojoExecutionException("Error loading project resources", e);
        }
    }
}
