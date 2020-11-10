export interface UserFederationRepresentation {
  id: string;
  name: string;
  providerId: string;
  providerType: string;
  parentId: string;
  config: { [index: string]: any };
}
