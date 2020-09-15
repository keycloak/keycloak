export interface GroupRepresentation {
  id?: string;
  name?: string;
  path?: string;
  attributes?: { [index: string]: string[] };
  realmRoles?: string[];
  clientRoles?: { [index: string]: string[] };
  subGroups?: GroupRepresentation[];
  access?: { [index: string]: boolean };
}
