import type RoleRepresentation from "./roleRepresentation.js";

export enum OrganizationInvitationStatus {
  PENDING = "PENDING",
  EXPIRED = "EXPIRED",
}

export default interface OrganizationInvitationRepresentation {
  id?: string;
  email?: string;
  organizationId?: string;
  firstName?: string;
  lastName?: string;
  sentDate?: number;
  expiresAt?: number;
  status?: OrganizationInvitationStatus;
  roles?: RoleRepresentation[];
  /** @deprecated The invite link is no longer exposed in API responses. */
  inviteLink?: string;
}
