package org.keycloak.guides;

import freemarker.template.TemplateException;
import org.keycloak.guides.maven.GuideBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DocsBuildDebugUtil {

    public static void main(String[] args) throws IOException, TemplateException, ParserConfigurationException, SAXException {
        File usrDir = new File(System.getProperty("user.dir"));
        Properties properties = readPropertiesFromPomXml();

        for (File srcDir: usrDir.toPath().resolve("docs/guides").toFile().listFiles(d -> d.isDirectory() && !d.getName().equals("templates"))) {
            if (srcDir.getName().equals("target") || srcDir.getName().equals("src")) {
                // those are standard maven folders, ignore them
                continue;
            }
            File targetDir = usrDir.toPath().resolve("target/generated-guides/" + srcDir.getName()).toFile();
            targetDir.mkdirs();

            // put here all the entries needed from the parent pom.xml
            GuideBuilder builder = new GuideBuilder(srcDir, targetDir, null, properties);
            builder.build();
            System.out.println("Guides generated to: " + targetDir.getAbsolutePath());
        }
    }

    private static Properties readPropertiesFromPomXml() throws ParserConfigurationException, SAXException, IOException {
        Properties properties = new Properties();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // parse pom.xml file - avoid adding Maven as a dependency here
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File("pom.xml"));
        NodeList propertiesXml = doc.getDocumentElement().getElementsByTagName("properties").item(0).getChildNodes();
        for(int i = 0; i < propertiesXml.getLength(); ++i) {
            Node item = propertiesXml.item(i);
            if (!(item instanceof Element)) {
                continue;
            }
            properties.put(item.getNodeName(), item.getTextContent());
        }
        return properties;
    }

}
