package org.keycloak.guides;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.keycloak.guides.maven.GuideBuilder;
import org.keycloak.guides.maven.GuideMojo;

import freemarker.template.TemplateException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DocsBuildDebugUtil {

    public static void main(String[] args) throws IOException, TemplateException, ParserConfigurationException, SAXException {
        Properties properties = readPropertiesFromPomXml();

        Path usrDir = Paths.get(System.getProperty("user.dir"));
        Path guidesRoot = usrDir.resolve("docs/guides");
        for (Path srcDir : GuideMojo.getSourceDirs(guidesRoot)) {
            Path targetDir = usrDir.resolve("target").resolve("generated-guides").resolve(srcDir.getFileName());
            Files.createDirectories(targetDir);

            GuideBuilder builder = new GuideBuilder(srcDir, targetDir, null, properties);
            builder.build();
            System.out.println("Guides generated to: " + targetDir);
        }
    }

    private static Properties readPropertiesFromPomXml() throws ParserConfigurationException, SAXException, IOException {
        Properties properties = new Properties();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // parse pom.xml file - avoid adding Maven as a dependency here
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File("pom.xml"));
        NodeList propertiesXml = doc.getDocumentElement().getElementsByTagName("properties");
        if (propertiesXml.getLength() == 0)
            return properties;

        propertiesXml = propertiesXml.item(0).getChildNodes();
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
