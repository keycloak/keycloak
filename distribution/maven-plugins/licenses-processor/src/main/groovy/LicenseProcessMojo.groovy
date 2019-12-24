import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

import groovy.xml.XmlUtil

@Mojo(name="process", defaultPhase=LifecyclePhase.PROCESS_RESOURCES)
class LicenseProcessMojo extends AbstractMojo {

    @Parameter(defaultValue='${project}', readonly=true)
    private MavenProject project

    void execute() {
        FileSystem fs = FileSystems.default

        // Property configuration with defaults
        def outputDirectoryRaw = project.properties['outputDirectory'] ?: "${project.build.directory}/licenses"
        def xmlFileSource = project.properties['xmlFileSource'] ?: "${project.basedir}/src/main/resources/licenses/${project.properties['product.slot']}/licenses.xml"
        def licenseName = project.properties['licenseName'] ?: "Apache Software License 2.0"
        def licenseUrl = project.properties['licenseUrl'] ?: "https://raw.githubusercontent.com/keycloak/keycloak/${project.version}/LICENSE.txt"
        def groupId = project.properties['groupId'] ?: "org.keycloak"

        Path outputDirectory = fs.getPath(outputDirectoryRaw)
        Files.createDirectories(outputDirectory)

        // Load license data XML for modification
        Path licensesXmlFile = fs.getPath(xmlFileSource)
        Node root = new XmlParser().parse(licensesXmlFile.toFile())
        Node dependencies = root.dependencies[0]

        // For each direct dependency, append those matching the groupId filter
        log.info("Appending first party dependency license data")
        Path licenseFileRoot = outputDirectory
        def matched = false
        project.dependencyArtifacts.toSorted().each { artifact ->
            if (artifact.groupId == groupId) {
                matched = true

                def dependency = dependencies.appendNode('dependency')
                dependency.appendNode('groupId', artifact.groupId)
                dependency.appendNode('artifactId', artifact.artifactId)
                dependency.appendNode('version', artifact.version)
                def licenses = dependency.appendNode('licenses')
                def license = licenses.appendNode('license')
                license.appendNode('name', licenseName)
                license.appendNode('url', licenseUrl)

                def newFilename = "${artifact.groupId},${artifact.artifactId},${artifact.version},${licenseName}.txt"
                Path newFile = licenseFileRoot.resolve(newFilename)
                InputStream originalLicense = this.class.getResourceAsStream("keycloak-licenses-common/LICENSE.txt")
                log.info("==> ${newFilename}")
                Files.copy(originalLicense, newFile, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        if (!matched) {
            fail("No project direct dependencies matched groupId ${groupId}")
        }

        // Write out XML with appended data to new path
        Path outputLicensesXmlFile = outputDirectory.resolve("licenses.xml")
        log.info("Writing XML to ${outputLicensesXmlFile}")
        outputLicensesXmlFile.withWriter("utf-8") { writer ->
            XmlUtil.serialize(root, writer)
        }

        // Copy misc. files from the resource jar
        ["licenses.xsl", "licenses.css"].each { filename ->
            InputStream input = this.class.getResourceAsStream("keycloak-licenses-common/${filename}")
            Path out = outputDirectory.resolve(filename)
            Files.copy(input, out, StandardCopyOption.REPLACE_EXISTING)
        }

        // Generate HTML by applying XSLT to XML. Have to switch to the Java
        // libraries for XSLT support.
        Path outputHtmlFile = outputDirectory.resolve("licenses.html")
        log.info("Writing transformed HTML to ${outputHtmlFile}")
        Transformer transformer
        L:{
            InputStream input = this.class.getResourceAsStream("keycloak-licenses-common/licenses.xsl")
            transformer = TransformerFactory.newInstance().newTemplates(new StreamSource(input)).newTransformer()
        }
        transformer.setParameter("productname", project.properties['product.name.full'])
        transformer.setParameter("version", project.version)
        outputLicensesXmlFile.withInputStream() { inStream ->
            def input = new StreamSource(inStream)
            outputHtmlFile.withOutputStream() { outStream ->
                def out = new StreamResult(outStream)
                transformer.transform(input, out)
            }
        }
    }

}
