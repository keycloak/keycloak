package org.keycloak.connections.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A test to ensure that database changes are not added to the changelog of a Keycloak patch release without considering
 * the impact on zero-downtime rolling upgrades.
 * <p>
 * Before a database modification is added to a patch release, it is crucial that the author of the changes verifies that
 * the update will not lock the entire database. Updates that lock the entire database are not compatible with zero-downtime
 * rollouts as when the lock is acquired it will prevent the existing Keycloak nodes from functioning.
 * <p>
 * If a database update is possible in a patch release without causing problematic locking, then the id of the liquibase
 * changeset should be added to the {@link DatabasePatchUpdateTest#ALLOWED_PATCHES} {@link Set}.
 * <p>
 * If a database update is essential, i.e. to fix a CVE or significant performance regression, and database wide locking
 * cannot be avoided then three steps are required:
 * <ol>
 * <li>Inform the wider Keycloak team that the database change is unavoidable</li>
 * <li>Update the logic in {@link org.keycloak.compatibility.KeycloakCompatibilityMetadataProvider} so that a rolling upgrade is avoided when upgrading to the patch version that introduces the database update</li>
 * <li>Add the liquibase changeset id to the {@link DatabasePatchUpdateTest#ALLOWED_PATCHES} {@link Set}.</li>
 * </ol>
 */
public class DatabasePatchUpdateTest {

    static final String RESOURCE_DIR = "META-INF";

    // Add the id of a Liquibase changeSet here in order to allow the patch
    static final Set<String> ALLOWED_PATCHES = Set.of(
          "99.9.1-test-change"
    );

    @ParameterizedTest
    @MethodSource("patchReleases")
    public void testAllowedChangeSetsOnly(String release) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Security: Disable DTDs to prevent XML External Entity (XXE) attacks
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (InputStream is = getClass().getResourceAsStream("/%s/%s".formatted(RESOURCE_DIR, release))) {
            XMLStreamReader reader = factory.createXMLStreamReader(is);

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String tagName = reader.getLocalName();

                    // 1. Handle Root Element
                    if (tagName.equals("databaseChangeLog")) {
                        continue;
                    }

                    // 2. Process changeSet
                    if (tagName.equals("changeSet")) {
                        String id = reader.getAttributeValue(null, "id");
                        Assertions.assertTrue(ALLOWED_PATCHES.contains(id), "Unexpected change '%s' in release '%s'".formatted(id, release));

                        // Skip all child elements until we find the closing </changeSet>
                        int depth = 1;
                        while (depth > 0 && reader.hasNext()) {
                            event = reader.next();
                            if (event == XMLStreamConstants.START_ELEMENT) {
                                depth++;
                            } else if (event == XMLStreamConstants.END_ELEMENT) {
                                depth--;
                            }
                        }
                        continue;
                    }
                    // 3. Strict Requirement: Throw exception for any other element
                    fail("Unexpected element <%s> found in '%s'. Only <changeSet> is permitted within <databaseChangeLog>".formatted(tagName, release));
                }
            }
        }
    }

    // Detect all jpa-changelog*x-y.z.xml files for patch releases on the classpath that are for a KC version greater than 26.6.0
    static Collection<String> patchReleases() throws IOException, URISyntaxException {
        List<String> fileNames = new ArrayList<>();
        Enumeration<URL> en = DatabasePatchUpdateTest.class.getClassLoader().getResources(RESOURCE_DIR);

        while (en.hasMoreElements()) {
            URI uri = en.nextElement().toURI();

            if (uri.getScheme().equals("jar")) {
                // Handle JAR resources
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    Path path = fs.getPath(RESOURCE_DIR);
                    fileNames.addAll(listFromPath(path));
                }
            } else {
                // Handle local file system (IDE)
                fileNames.addAll(listFromPath(Paths.get(uri)));
            }
        }

        Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        return fileNames.stream()
              .filter(string -> string.startsWith("jpa-changelog"))
              .filter(string -> {
                  Matcher matcher = pattern.matcher(string);
                  if (matcher.find()) {
                      int major = Integer.parseInt(matcher.group(1));
                      int minor = Integer.parseInt(matcher.group(2));
                      int patch = Integer.parseInt(matcher.group(3));
                      if (patch == 0) return false;
                      if (major > 26) return true;
                      return major == 26 && minor >= 6;
                  }
                  return false;
              })
              .toList();
    }

    private static List<String> listFromPath(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path, 1)) {
            return walk.filter(Files::isRegularFile)
                  .map(p -> p.getFileName().toString())
                  .toList();
        }
    }
}
