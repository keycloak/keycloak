package org.keycloak.storage.ldap.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.ldap.LDAPStorageProvider;

public class CertificateLDAPStorageMapperFactory extends UserAttributeLDAPStorageMapperFactory {

  public static final String PROVIDER_ID = "certificate-ldap-mapper";

  private static final List<ProviderConfigProperty> certificateConfigProperties;

  static {
    certificateConfigProperties = getCertificateConfigProperties(null);
  }

  private static List<ProviderConfigProperty> getCertificateConfigProperties(ComponentModel p) {
    List<ProviderConfigProperty> configProps = new ArrayList<>(getConfigProps(null));

    ProviderConfigurationBuilder config = ProviderConfigurationBuilder.create()
        .property()
        .name(CertificateLDAPStorageMapper.IS_DER_FORMATTED)
        .label("DER Formatted")
        .helpText("Activate this if the certificate is DER formatted in LDAP and not PEM formatted.")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .add();
    configProps.addAll(config.build());
    return configProps;
  }

  @Override
  public String getHelpText() {
    return "Used to map single attribute which contains a certificate from LDAP user to attribute of UserModel in Keycloak DB";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return certificateConfigProperties;
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
    super.validateConfiguration(session, realm, config);

    boolean isBinaryAttribute = config.get(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, false);
    boolean isDerFormatted = config.get(CertificateLDAPStorageMapper.IS_DER_FORMATTED, false);
    if (isDerFormatted && !isBinaryAttribute) {
      throw new ComponentValidationException("With DER formatted certificate enabled, the ''Is Binary Attribute'' option must be enabled too");
    }

  }

  @Override
  protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
    return new CertificateLDAPStorageMapper(mapperModel, federationProvider);
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
    return getCertificateConfigProperties(parent);
  }

}
