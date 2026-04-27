/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_rolerepresentation
 */

export default interface RoleRepresentation {
  id?: string;
  name?: string;
  description?: string;
  scopeParamRequired?: boolean;
  composite?: boolean;
  composites?: Composites;
  clientRole?: boolean;
  containerId?: string;
  attributes?: { [index: string]: string[] };
}

export interface Composites {
  realm?: string[];
  client?: { [index: string]: string[] };
  application?: { [index: string]: string[] };
}

// when requesting to role-mapping api (create, delete), id and name are required
export interface RoleMappingPayload extends RoleRepresentation {
  id: string;
  name: string;
}
