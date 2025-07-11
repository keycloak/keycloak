/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/#_keystoreconfig
 */
export default interface KeyStoreConfig {
  realmCertificate?: boolean;
  storePassword?: string;
  keyPassword?: string;
  keyAlias?: string;
  realmAlias?: string;
  format?: string;
  keySize?: number;
  validity?: number;
}
