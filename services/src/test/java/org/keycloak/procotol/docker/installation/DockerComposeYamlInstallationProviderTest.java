package org.keycloak.procotol.docker.installation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyType;
import org.keycloak.protocol.docker.installation.DockerComposeYamlInstallationProvider;
import org.keycloak.rule.CryptoInitRule;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.keycloak.protocol.docker.installation.DockerComposeYamlInstallationProvider.ROOT_DIR;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DockerComposeYamlInstallationProviderTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    DockerComposeYamlInstallationProvider installationProvider;
    static Certificate certificate;
    
    @Before
    public void setUp() throws Exception {
        final KeyPairGenerator keyGen = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        keyGen.initialize(2048, new SecureRandom());

        final KeyPair keypair = keyGen.generateKeyPair();
        certificate = CertificateUtils.generateV1SelfSignedCertificate(keypair, "test-realm");
        installationProvider = new DockerComposeYamlInstallationProvider();
    }

    private Response fireInstallationProvider() throws IOException {
        ByteArrayOutputStream byteStream = null;
        ZipOutputStream zipOutput = null;
        byteStream = new ByteArrayOutputStream();
        zipOutput = new ZipOutputStream(byteStream);

        return installationProvider.generateInstallation(zipOutput, byteStream, certificate, new URL("http://localhost:8080/auth/"), "docker-test", "docker-registry");
    }

    @Test
    @Ignore // Used only for smoke testing
    public void writeToRealZip() throws IOException {
        final Response response = fireInstallationProvider();
        final byte[] responseBytes = (byte[]) response.getEntity();
        FileUtils.writeByteArrayToFile(new File("target/keycloak-docker-compose-yaml.zip"), responseBytes);
    }

    @Test
    public void testAllTheZipThings() throws Exception {
        final Response response = fireInstallationProvider();
        assertThat("compose YAML returned non-ok response", response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        shouldIncludeDockerComposeYamlInZip(getZipResponseFromInstallProvider(response));
        shouldIncludeReadmeInZip(getZipResponseFromInstallProvider(response));
        shouldWriteBlankDataDirectoryInZip(getZipResponseFromInstallProvider(response));
        shouldWriteCertDirectoryInZip(getZipResponseFromInstallProvider(response));
        shouldWriteSslCertificateInZip(getZipResponseFromInstallProvider(response));
        shouldWritePrivateKeyInZip(getZipResponseFromInstallProvider(response));
    }

    public void shouldIncludeDockerComposeYamlInZip(ZipInputStream zipInput) throws Exception {
        final Optional<String> actualDockerComposeFileContents = getFileContents(zipInput, ROOT_DIR + "docker-compose.yaml");

        assertThat("Could not find docker-compose.yaml file in zip archive response", actualDockerComposeFileContents.isPresent(), equalTo(true));

        List<String> expectedDockerComposeAsStringLines = FileUtils.readLines(new File("src/test/resources/docker-compose-expected.yaml"), Charset.defaultCharset());
        String[] actualDockerComposeAsStringLines = actualDockerComposeFileContents.get().split("\n");

        String messageIfTestFails = "Invalid docker-compose file contents: \n" + actualDockerComposeFileContents.get();
        for (int i = 0; i < expectedDockerComposeAsStringLines.size(); i++) {
            assertEquals(messageIfTestFails, expectedDockerComposeAsStringLines.get(i), actualDockerComposeAsStringLines[i]);
        }
    }

    public void shouldIncludeReadmeInZip(ZipInputStream zipInput) throws Exception {
        final Optional<String> dockerComposeFileContents = getFileContents(zipInput, ROOT_DIR + "README.md");

        assertThat("Could not find README.md file in zip archive response", dockerComposeFileContents.isPresent(), equalTo(true));
    }

    public void shouldWriteBlankDataDirectoryInZip(ZipInputStream zipInput) throws Exception {
        ZipEntry zipEntry;
        boolean dataDirFound = false;
        while ((zipEntry = zipInput.getNextEntry()) != null) {
            try {
                if (zipEntry.getName().equals(ROOT_DIR + "data/")) {
                    dataDirFound = true;
                    assertThat("Zip entry for data directory is not the correct type", zipEntry.isDirectory(), equalTo(true));
                }
            } finally {
                zipInput.closeEntry();
            }
        }

        assertThat("Could not find data directory", dataDirFound, equalTo(true));
    }

    public void shouldWriteCertDirectoryInZip(ZipInputStream zipInput) throws Exception {
        ZipEntry zipEntry;
        boolean certsDirFound = false;
        while ((zipEntry = zipInput.getNextEntry()) != null) {
            try {
                if (zipEntry.getName().equals(ROOT_DIR + "certs/")) {
                    certsDirFound = true;
                    assertThat("Zip entry for cert directory is not the correct type", zipEntry.isDirectory(), equalTo(true));
                }
            } finally {
                zipInput.closeEntry();
            }
        }

        assertThat("Could not find cert directory", certsDirFound, equalTo(true));
    }

    public void shouldWriteSslCertificateInZip(ZipInputStream zipInput) throws Exception {
        final Optional<String> localhostCertificateFileContents = getFileContents(zipInput, ROOT_DIR + "certs/localhost.crt");

        assertThat("Could not find localhost certificate", localhostCertificateFileContents.isPresent(), equalTo(true));
        final X509Certificate x509Certificate = PemUtils.decodeCertificate(localhostCertificateFileContents.get());
        assertThat("Invalid x509 given by docker-compose YAML", x509Certificate, notNullValue());
    }

    public void shouldWritePrivateKeyInZip(ZipInputStream zipInput) throws Exception {
        final Optional<String> localhostPrivateKeyFileContents = getFileContents(zipInput, ROOT_DIR + "certs/localhost.key");

        assertThat("Could not find localhost private key", localhostPrivateKeyFileContents.isPresent(), equalTo(true));
        final PrivateKey privateKey = PemUtils.decodePrivateKey(localhostPrivateKeyFileContents.get());
        assertThat("Invalid private Key given by docker-compose YAML", privateKey, notNullValue());
    }

    private ZipInputStream getZipResponseFromInstallProvider(Response response) throws IOException {
        final Object responseEntity = response.getEntity();
        if (!(responseEntity instanceof byte[])) {
            fail("Recieved non-byte[] entity for docker-compose YAML installation response");
        }

        return new ZipInputStream(new ByteArrayInputStream((byte[]) responseEntity));
    }

    private static Optional<String> getFileContents(final ZipInputStream zipInputStream, final String fileName) throws IOException {
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            try {
                if (zipEntry.getName().equals(fileName)) {
                    return Optional.of(readBytesToString(zipInputStream));
                }
            } finally {
                zipInputStream.closeEntry();
            }
        }

        // fall-through case if file name not found:
        return Optional.empty();
    }

    private static String readBytesToString(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } finally {
            output.close();
        }

        return new String(output.toByteArray());
    }
}
