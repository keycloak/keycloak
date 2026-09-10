import UserRepresentation from "./userRepresentation.js";

export default interface OrganizationMemberRepresentation
  extends UserRepresentation {
  membershipType: string;
}
