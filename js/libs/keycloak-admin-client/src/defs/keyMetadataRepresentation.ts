/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_keysmetadatarepresentation-keymetadatarepresentation
 */
export default interface KeysMetadataRepresentation {
  active?: { [index: string]: string };
  keys?: KeyMetadataRepresentation[];
}

export interface KeyMetadataRepresentation {
  providerId?: string;
  providerPriority?: number;
  kid?: string;
  status?: string;
  type?: string;
  algorithm?: string;
  publicKey?: string;
  certificate?: string;
  validTo?: string;
}
