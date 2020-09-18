// Generated using typescript-generator version 2.0.400 on 2020-09-11 12:02:07.

export interface RoleRepresentation {
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
