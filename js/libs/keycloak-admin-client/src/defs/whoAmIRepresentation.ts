export type AccessType =
  | "view-realm"
  | "view-identity-providers"
  | "manage-identity-providers"
  | "impersonation"
  | "create-client"
  | "manage-users"
  | "query-realms"
  | "view-authorization"
  | "query-clients"
  | "query-users"
  | "manage-events"
  | "manage-realm"
  | "view-events"
  | "view-users"
  | "view-clients"
  | "manage-authorization"
  | "manage-clients"
  | "query-groups"
  | "anyone";

export default interface WhoAmIRepresentation {
  userId: string;
  realm: string;
  displayName: string;
  locale: string;
  createRealm: boolean;
  realm_access: { [key: string]: AccessType[] };
}
