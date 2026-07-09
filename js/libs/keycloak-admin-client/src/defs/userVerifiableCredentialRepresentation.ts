/**
 * Represents a verifiable credential associated with a user.
 * */
export default interface UserVerifiableCredentialRepresentation {
  credentialScopeName?: string;
  credentialConfigurationId?: string;
  revision?: string;
  createdDate?: number;
  updatedDate?: number;
  userAttributes?: Record<string, string[]>;
}
