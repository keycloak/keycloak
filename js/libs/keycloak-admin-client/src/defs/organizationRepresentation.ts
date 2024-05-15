import type OrganizationDomainRepresentation from "./organizationDomainRepresentation.js";

export default interface OrganizationRepresentation {
  id?: string;
  name?: string;
  description?: string;
  enabled?: boolean;
  attributes?: { [index: string]: string[] };
  domains?: OrganizationDomainRepresentation[];
}
