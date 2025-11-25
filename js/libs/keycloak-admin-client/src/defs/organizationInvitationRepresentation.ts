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
  inviteLink?: string;
}
