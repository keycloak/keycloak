export default interface OrganizationRoleRepresentation {
  id?: string;
  name?: string;
  description?: string;
  composite?: boolean;
  attributes?: Record<string, string[]>;
}
