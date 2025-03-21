import type OrganizationRepresentation from "./organizationRepresentation.js";

export default interface MembershipRepresentation
  extends OrganizationRepresentation {
  membershipType?: string;
}
