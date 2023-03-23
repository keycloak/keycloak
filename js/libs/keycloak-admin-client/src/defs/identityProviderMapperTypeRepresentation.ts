import type { ConfigPropertyRepresentation } from "./configPropertyRepresentation.js";

export interface IdentityProviderMapperTypeRepresentation {
  id?: string;
  name?: string;
  category?: string;
  helpText?: string;
  properties?: ConfigPropertyRepresentation[];
}
