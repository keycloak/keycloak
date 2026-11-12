import type OrganizationDomainRepresentation from "./organizationDomainRepresentation.js";
import type IdentityProviderRepresentation from "./identityProviderRepresentation.js";
import type MemberRepresentation from "./memberRepresentation.js";
import type GroupRepresentation from "./groupRepresentation.js";

export default interface OrganizationRepresentation {
  id?: string;
  name?: string;
  alias?: string;
  description?: string;
  redirectUrl?: string;
  enabled?: boolean;
  attributes?: Record<string, string[]>;
  domains?: OrganizationDomainRepresentation[];
  members?: MemberRepresentation[];
  identityProviders?: IdentityProviderRepresentation[];
  groups?: GroupRepresentation[];
}
