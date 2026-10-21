import type UserRepresentation from "./userRepresentation.js";

export default interface MemberRepresentation extends UserRepresentation {
  membershipType?: string;
}
