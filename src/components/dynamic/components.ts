import type { FunctionComponent } from "react";

import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { StringComponent } from "./StringComponent";
import { BooleanComponent } from "./BooleanComponent";
import { ListComponent } from "./ListComponent";
import { RoleComponent } from "./RoleComponent";
import { ScriptComponent } from "./ScriptComponent";
import { ClientSelectComponent } from "./ClientSelectComponent";
import { MultiValuedStringComponent } from "./MultivaluedStringComponent";
import { MultiValuedListComponent } from "./MultivaluedListComponent";

export type ComponentProps = Omit<ConfigPropertyRepresentation, "type"> & {
  selectedValues?: string[];
  parentCallback?: (data: any) => void;
};
const ComponentTypes = [
  "String",
  "boolean",
  "List",
  "Role",
  "Script",
  "MultivaluedList",
  "ClientList",
  "MultivaluedString",
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
  MultivaluedList: MultiValuedListComponent,
  MultivaluedString: MultiValuedStringComponent,
} as const;

export const isValidComponentType = (value: string): value is Components =>
  value in COMPONENTS;
