/**
 * Representation for organization membership used in partial imports.
 */
export default interface OrganizationMembershipRepresentation {
  organizationId?: string;
  username?: string;
  membershipType?: "MANAGED" | "UNMANAGED";
}
