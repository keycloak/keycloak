package org.keycloak.db.compatibility.verifier;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

record ChangeLogXMLParser(ClassLoader classLoader) {

   static final String RESOURCE_DIR = "META-INF";

   Set<ChangeSet> discoverAllChangeSets() throws IOException {
      var changeSets = changeSetXmlFiles()
            .map(this::extractChangeSets)
            .flatMap(List::stream)
            .toList();

      Set<ChangeSet> uniqueSets = new HashSet<>(changeSets.size());
      ChangeSet duplicate = changeSets.stream()
            .filter(item -> !uniqueSets.add(item))
            .findFirst()
            .orElse(null);
      if (duplicate != null) {
         throw new IllegalStateException("Duplicate ChangeSet detected: " + duplicate);
      }
      return uniqueSets;
   }

   List<ChangeSet> extractChangeSets(String filename) {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      // Security: Disable DTDs to prevent XML External Entity (XXE) attacks
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      List<ChangeSet> ids = new ArrayList<>();

      try (InputStream is = classLoader.getResourceAsStream(filename)) {
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
                  String author = reader.getAttributeValue(null, "author");
                  ids.add(new ChangeSet(id, author, filename));

                  // Skip all child elements until we find the closing </changeSet>
                  skipUnknownElement(reader);
                  continue;
               }
               // 3. Skip all other elements
               skipUnknownElement(reader);
            }
         }
         return ids;
      } catch (IOException | XMLStreamException e) {
         throw new IllegalStateException(e);
      }
   }

   // Detect all jpa-changelog*.xml files on the classpath
   private Stream<String> changeSetXmlFiles() throws IOException {
      List<String> fileNames = new ArrayList<>();
      Enumeration<URL> en = classLoader.getResources(RESOURCE_DIR);

      while (en.hasMoreElements()) {
         URI uri;
         try {
            uri = en.nextElement().toURI();
         } catch (URISyntaxException e) {
            // Should never happen
            throw new IllegalStateException(e);
         }

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
      return fileNames.stream()
            .filter(s -> s.startsWith("jpa-changelog") && s.endsWith(".xml"))
            .map(s -> "%s/%s".formatted(RESOURCE_DIR, s));
   }

   private List<String> listFromPath(Path path) throws IOException {
      try (Stream<Path> walk = Files.walk(path, 1)) {
         return walk.filter(Files::isRegularFile)
               .map(p -> p.getFileName().toString())
               .toList();
      }
   }

   private static void skipUnknownElement(XMLStreamReader reader) throws XMLStreamException {
      int level = 1;
      while (level > 0 && reader.hasNext()) {
         int event = reader.next();
         if (event == XMLStreamConstants.START_ELEMENT) {
            level++;
         } else if (event == XMLStreamConstants.END_ELEMENT) {
            level--;
         }
      }
   }
}
