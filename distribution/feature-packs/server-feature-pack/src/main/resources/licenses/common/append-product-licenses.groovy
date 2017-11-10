import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

import groovy.xml.XmlUtil

// Insert nodes into the xml file for the filtered dependencies in the active
// maven project. Also create individual license files for the new xml nodes,
// by copying one full copy, and symlinks to that copy for the rest.

FileSystem fs = FileSystems.default

Path licensesXmlFile = fs.getPath(properties['xmlFile'])
log.info("File ${properties['licenseFile']} will be copied for use as common individual license file")
Node root = new XmlParser().parse(licensesXmlFile.toFile())
Node dependencies = root.dependencies[0]

Path licenseFileRoot = licensesXmlFile.parent
Path licenseFile = null

def matched = false
session.currentProject.dependencyArtifacts.toSorted().each { artifact ->
    if (artifact.groupId == properties['groupId']) {
        matched = true

        def dependency = dependencies.appendNode('dependency')
        dependency.appendNode('groupId', artifact.groupId)
        dependency.appendNode('artifactId', artifact.artifactId)
        dependency.appendNode('version', artifact.version)
        def licenses = dependency.appendNode('licenses')
        def license = licenses.appendNode('license')
        license.appendNode('name', properties['licenseName'])
        license.appendNode('url', properties['licenseUrl'])

        def newFilename = "${artifact.groupId},${artifact.artifactId},${artifact.version},${properties['licenseName']}.txt"
        Path newFile = licenseFileRoot.resolve(newFilename)
        if (licenseFile == null) {
            log.info("==> ${newFilename}")
            Path original = fs.getPath(properties['licenseFile'])
            Files.copy(original, newFile)
            // Drop any weird mode setting the original file might have
            Files.setPosixFilePermissions(newFile, PosixFilePermissions.fromString("rw-rw-r--"))
            licenseFile = newFile
        } else {
            log.info("  -> ${newFilename}")
            Files.createSymbolicLink(newFile, licenseFile.fileName);
        }
    }
}

if (!matched) {
    fail("No project direct dependencies matched groupId ${properties['groupId']}")
}

log.info("Updating XML ${properties['xmlFile']}")
licensesXmlFile.withWriter("utf-8") { writer ->
    XmlUtil.serialize(root, writer)
}
