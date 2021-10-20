import type { FunctionComponent } from "react";

import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { StringComponent } from "./StringComponent";
import { BooleanComponent } from "./BooleanComponent";
import { ListComponent } from "./ListComponent";
import { RoleComponent } from "./RoleComponent";
import { ScriptComponent } from "./ScriptComponent";
import { ClientSelectComponent } from "./ClientSelectComponent";

export type ComponentProps = Omit<ConfigPropertyRepresentation, "type">;
const ComponentTypes = [
  "String",
  "boolean",
  "List",
  "Role",
  "Script",
  "ClientList",
] as const;

export type Components = typeof ComponentTypes[number];

export const COMPONENTS: {
  [index in Components]: FunctionComponent<ComponentProps>;
} = {
  String: StringComponent,
  boolean: BooleanComponent,
  List: ListComponent,
  Role: RoleComponent,
  Script: ScriptComponent,
  ClientList: ClientSelectComponent,
} as const;
