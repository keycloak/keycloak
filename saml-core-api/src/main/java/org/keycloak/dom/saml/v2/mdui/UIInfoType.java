package org.keycloak.dom.saml.v2.mdui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;

/**
 * *
 * <p>
 * Java class for UIInfoType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 *   &lt;element name="UIInfo" type="mdui:UIInfoType"/>
 *   &lt;complexType name="UIInfoType">
 *       &lt;choice minOccurs="0" maxOccurs="unbounded">
 *           &lt;element ref="mdui:DisplayName"/>
 *           &lt;element ref="mdui:Description"/>
 *           &lt;element ref="mdui:Keywords"/>
 *           &lt;element ref="mdui:Logo"/>
 *           &lt;element ref="mdui:InformationURL"/>
 *           &lt;element ref="mdui:PrivacyStatementURL"/>
 *           &lt;any namespace="##other" processContents="lax"/>
 *       &lt;/choice>
 * &lt;/complexType>
 *
 * </pre>
 */

public class UIInfoType implements Serializable {

    protected List<LocalizedNameType> displayName = new ArrayList<>();
    protected List<LocalizedNameType> description = new ArrayList<>();
    protected List<KeywordsType> keywords = new ArrayList<>();
    protected List<LocalizedURIType> informationURL = new ArrayList<>();
    protected List<LocalizedURIType> privacyStatementURL = new ArrayList<>();
    protected List<LogoType> logo = new ArrayList<>();

    public void addDisplayName(LocalizedNameType displayName) {
        this.displayName.add(displayName);
    }

    public void addDescription(LocalizedNameType description) {
        this.description.add(description);
    }

    public void addKeywords(KeywordsType keywords) {
        this.keywords.add(keywords);
    }

    public void addInformationURL(LocalizedURIType informationURL) {
        this.informationURL.add(informationURL);
    }

    public void addPrivacyStatementURL(LocalizedURIType privacyStatementURL) {
        this.privacyStatementURL.add(privacyStatementURL);
    }

    public void addLogo(LogoType logo) {
        this.logo.add(logo);
    }

    public List<LocalizedNameType> getDisplayName() {
        return displayName;
    }

    public List<LocalizedNameType> getDescription() {
        return description;
    }

    public List<KeywordsType> getKeywords() {
        return keywords;
    }

    public List<LocalizedURIType> getInformationURL() {
        return informationURL;
    }

    public List<LocalizedURIType> getPrivacyStatementURL() {
        return privacyStatementURL;
    }

    public List<LogoType> getLogo() {
        return logo;
    }

}