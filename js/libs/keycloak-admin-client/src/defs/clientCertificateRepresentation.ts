export default interface ClientCertificateRepresentation {
  providerId?: string;
  providerName?: string;
  kid?: string;
  status?: string;
  type?: string;
  algorithm?: string;
  use?: string;
  subjectDn?: string;
  issuerDn?: string;
  serialNumber?: string;
  validFrom?: number;
  validTo?: number;
  fingerprintSha256?: string;
}

export interface ClientCertificateTestRepresentation {
  keyProviderId?: string;
  clientId?: string;
  tokenUrl?: string;
  mtlsTokenUrl?: string;
}
