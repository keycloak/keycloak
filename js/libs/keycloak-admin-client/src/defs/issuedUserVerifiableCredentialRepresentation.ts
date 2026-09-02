export default interface IssuedUserVerifiableCredentialRepresentation {
  id?: string;
  userId?: string;
  credentialType?: string;
  issuedAt?: number;
  expiresAt?: number;
  clientId?: string;
  revision?: string;
  clientName?: string;
  clientBaseUrl?: string;
}
