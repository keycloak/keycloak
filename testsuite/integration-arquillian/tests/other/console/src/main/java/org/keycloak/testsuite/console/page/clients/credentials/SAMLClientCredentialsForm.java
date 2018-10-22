package org.keycloak.testsuite.console.page.clients.credentials;

import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.net.URL;

import static org.keycloak.services.resources.admin.ClientAttributeCertificateResource.CERTIFICATE_PEM;
import static org.keycloak.common.util.KeystoreUtil.KeystoreFormat.JKS;
import static org.keycloak.common.util.KeystoreUtil.KeystoreFormat.PKCS12;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.sendKeysToInvisibleElement;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class SAMLClientCredentialsForm extends Form {

    private static final String PATH_PREFIX = "saml-keys/";

    @FindBy(linkText = "SAML Keys")
    private WebElement samlKeysLink;

    @FindBy(xpath = "//button[@data-ng-click='importSigningKey()']")
    private WebElement importButton;

    @FindBy(id = "uploadKeyFormat")
    private Select uploadKeyFormat;

    @FindBy(id = "import-file")
    private WebElement selectFileButton;

    @FindBy(xpath = "//button[@data-ng-click='uploadFile()']")
    private WebElement uploadButton;

    @FindBy(xpath = "//div[contains(@class, 'alert-success')]")
    private WebElement success;

    @FindBy(id = "uploadKeyAlias")
    private WebElement uploadKeyAlias;

    @FindBy(id = "uploadStorePas")
    private WebElement uploadStorePassword;

    public void importPemCertificateKey() {
        navigateToImport();
        uploadKeyFormat.selectByVisibleText(CERTIFICATE_PEM);
        uploadFile(PATH_PREFIX + "client.pem");
    }

    public void importJKSKey() {
        navigateToImport();
        uploadKeyFormat.selectByVisibleText(JKS.toString());
        fillCredentials();
        uploadFile(PATH_PREFIX + "client.jks");
    }

    public void importPKCS12Key() {
        navigateToImport();
        uploadKeyFormat.selectByVisibleText(PKCS12.toString());
        fillCredentials();
        uploadFile(PATH_PREFIX + "client.p12");
    }

    public String getSuccessMessage() {
        return getTextFromElement(success);
    }

    private void uploadFile(String file) {
        URL fileUrl = getClass().getClassLoader().getResource(file);
        String absolutePath = new File(fileUrl.getFile()).getAbsolutePath(); // For Windows, we need to use File.getAbsolutePath()
        sendKeysToInvisibleElement(selectFileButton, absolutePath);
        clickLink(uploadButton);
    }

    private void fillCredentials() {
        UIUtils.setTextInputValue(uploadKeyAlias, "samlKey");
        UIUtils.setTextInputValue(uploadStorePassword, "secret");
    }

    private void navigateToImport() {
        clickLink(samlKeysLink);
        clickLink(importButton);
    }
}
