import type FederatedIdentityRepresentation from "./federatedIdentityRepresentation.js";

/**
 * Represents an identity provider either already linked to a user, or available for the user to link to.
 */
export default interface FederatedIdentityLinkRepresentation
  extends FederatedIdentityRepresentation {
  providerId?: string;
  displayName?: string;
  social?: boolean;
}
