import type OrganizationDomainRepresentation from "./organizationDomainRepresentation.js";

export default interface OrganizationRepresentation {
  id?: string;
  name?: string;
  description?: string;
  redirectUrl?: string;
  enabled?: boolean;
  attributes?: Record<string, string[]>;
  domains?: OrganizationDomainRepresentation[];
}
