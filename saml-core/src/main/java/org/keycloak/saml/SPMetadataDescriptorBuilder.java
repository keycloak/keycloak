package org.keycloak.saml;

import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class SPMetadataDescriptorBuilder {
    protected static Logger logger = Logger.getLogger(SPMetadataDescriptorBuilder.class);

    private URI loginBinding;
    private URI logoutBinding;
    private URI artifactResolutionBinding;
    private List<URI> assertionEndpoints = new ArrayList<>();
    private URI artifactResolutionEndpoint;
    private List<URI> logoutEndpoints = new ArrayList<>();
    private boolean wantAuthnRequestsSigned;
    private boolean wantAssertionsSigned;
    private boolean wantAssertionsEncrypted;
    private String entityId;
    private String nameIDPolicyFormat;
    private List<KeyDescriptorType> signingCerts;
    private List<KeyDescriptorType> encryptionCerts;
    private Integer metadataValidUntilUnit;
    private Integer metadataValidUntilPeriod;
    private Integer defaultAssertionEndpoint;

    public SPMetadataDescriptorBuilder loginBinding(URI loginBinding) {
        this.loginBinding = loginBinding;
        return this;
    }

    public SPMetadataDescriptorBuilder logoutBinding(URI logoutBinding) {
        this.logoutBinding = logoutBinding;
        return this;
    }

    public SPMetadataDescriptorBuilder artifactResolutionBinding(URI artifactResolutionBinding) {
        this.artifactResolutionBinding = artifactResolutionBinding;
        return this;
    }

    public SPMetadataDescriptorBuilder assertionEndpoints(List<URI> assertionEndpoints) {
        this.assertionEndpoints = assertionEndpoints;
        return this;
    }

    public SPMetadataDescriptorBuilder artifactResolutionEndpoint(URI artifactResolutionEndpoint) {
        this.artifactResolutionEndpoint = artifactResolutionEndpoint;
        return this;
    }

    public SPMetadataDescriptorBuilder logoutEndpoints(List<URI> logoutEndpoint) {
        this.logoutEndpoints = logoutEndpoint;
        return this;
    }

    public SPMetadataDescriptorBuilder wantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        this.wantAuthnRequestsSigned = wantAuthnRequestsSigned;
        return this;
    }

    public SPMetadataDescriptorBuilder wantAssertionsSigned(boolean wantAssertionsSigned) {
        this.wantAssertionsSigned = wantAssertionsSigned;
        return this;
    }

    public SPMetadataDescriptorBuilder wantAssertionsEncrypted(boolean wantAssertionsEncrypted) {
        this.wantAssertionsEncrypted = wantAssertionsEncrypted;
        return this;
    }

    public SPMetadataDescriptorBuilder entityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public SPMetadataDescriptorBuilder nameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
        return this;
    }

    public SPMetadataDescriptorBuilder signingCerts(List<KeyDescriptorType> signingCerts) {
        this.signingCerts = signingCerts;
        return this;
    }

    public SPMetadataDescriptorBuilder encryptionCerts(List<KeyDescriptorType> encryptionCerts) {
        this.encryptionCerts = encryptionCerts;
        return this;
    }

    public SPMetadataDescriptorBuilder metadataValidUntilUnit(Integer metadataValidUntilUnit) {
        this.metadataValidUntilUnit = metadataValidUntilUnit;
        return this;
    }

    public SPMetadataDescriptorBuilder metadataValidUntilPeriod(Integer metadataValidUntilPeriod) {
        this.metadataValidUntilPeriod = metadataValidUntilPeriod;
        return this;
    }

    public SPMetadataDescriptorBuilder defaultAssertionEndpoint(Integer defaultAssertionEndpoint) {
        this.defaultAssertionEndpoint = defaultAssertionEndpoint;
        return this;
    }

    public EntityDescriptorType build() {
        logger.info("Building SP Entity Descriptor");

        EntityDescriptorType entityDescriptorType = new EntityDescriptorType(entityId);
        entityDescriptorType.setID(IDGenerator.create("ID_"));
        if (metadataValidUntilUnit != null && metadataValidUntilPeriod != null) {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(metadataValidUntilPeriod, metadataValidUntilUnit);
                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                gregorianCalendar.setTime(calendar.getTime());
                entityDescriptorType.setValidUntil(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
            } catch (DatatypeConfigurationException e) {
                logger.warnf("Cannot convert configured valid until (%s %s) to a valid date time format.", metadataValidUntilUnit, metadataValidUntilPeriod);
            }
        }
        SPSSODescriptorType spSSODescriptor = new SPSSODescriptorType(Arrays.asList(JBossSAMLURIConstants.PROTOCOL_NSURI.get()));
        spSSODescriptor.setAuthnRequestsSigned(wantAuthnRequestsSigned);
        spSSODescriptor.setWantAssertionsSigned(wantAssertionsSigned);
        spSSODescriptor.addNameIDFormat(nameIDPolicyFormat);
        for (URI logoutEndpoint : logoutEndpoints) {
            spSSODescriptor.addSingleLogoutService(new EndpointType(logoutBinding, logoutEndpoint));
        }

        if (wantAuthnRequestsSigned && signingCerts != null) {
            for (KeyDescriptorType key: signingCerts) {
                spSSODescriptor.addKeyDescriptor(key);
            }
        }

        if (wantAssertionsEncrypted && encryptionCerts != null) {
            for (KeyDescriptorType key: encryptionCerts) {
                spSSODescriptor.addKeyDescriptor(key);
            }
        }

        if (artifactResolutionEndpoint != null && artifactResolutionBinding != null) {
            IndexedEndpointType artifactEndpoint = new IndexedEndpointType(artifactResolutionBinding, artifactResolutionEndpoint);
            artifactEndpoint.setIndex(0);
            spSSODescriptor.addArtifactResolutionService(artifactEndpoint);
        }

        int assertionIndex = 1;
        if (this.assertionEndpoints != null && !this.assertionEndpoints.isEmpty()) {
            for (URI assertionEndpoint : this.assertionEndpoints) {
                IndexedEndpointType assertionConsumerEndpoint = new IndexedEndpointType(loginBinding, assertionEndpoint);
                if (defaultAssertionEndpoint.equals(assertionIndex)) {
                    assertionConsumerEndpoint.setIsDefault(true);
                } else {
                    assertionConsumerEndpoint.setIsDefault(false);
                }
                assertionConsumerEndpoint.setIndex(assertionIndex);
                spSSODescriptor.addAssertionConsumerService(assertionConsumerEndpoint);
                assertionIndex++;

                IndexedEndpointType assertionConsumerEndpoint2 = new IndexedEndpointType(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri(), assertionEndpoint);
                if (defaultAssertionEndpoint.equals(assertionIndex)) {
                    assertionConsumerEndpoint2.setIsDefault(true);
                } else {
                    assertionConsumerEndpoint2.setIsDefault(false);
                }
                assertionConsumerEndpoint2.setIndex(assertionIndex);
                spSSODescriptor.addAssertionConsumerService(assertionConsumerEndpoint2);
                assertionIndex++;
            }
        } else {
            logger.warn("No assertion endpoints found!");
        }
        entityDescriptorType.addChoiceType(new EntityDescriptorType.EDTChoiceType(Arrays.asList(new EntityDescriptorType.EDTDescriptorChoiceType(spSSODescriptor))));
        return entityDescriptorType;
    }
}


