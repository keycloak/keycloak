export default interface WhoAmIRepresentation {
  userId: string;
  realm: string;
  displayName: string;
  locale: string;
  createRealm: boolean;
  realm_access: { [key: string]: string[] };
}
