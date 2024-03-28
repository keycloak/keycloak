import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { FunctionComponent } from "react";

import { BooleanComponent } from "./BooleanComponent";
import { ClientSelectComponent } from "./ClientSelectComponent";
import { FileComponent } from "./FileComponent";
import { GroupComponent } from "./GroupComponent";
import { ListComponent } from "./ListComponent";
import { MapComponent } from "./MapComponent";
import { MultiValuedListComponent } from "./MultivaluedListComponent";
import { MultiValuedStringComponent } from "./MultivaluedStringComponent";
import { PasswordComponent } from "./PasswordComponent";
import { RoleComponent } from "./RoleComponent";
import { ScriptComponent } from "./ScriptComponent";
import { StringComponent } from "./StringComponent";
import { TextComponent } from "./TextComponent";
import { UrlComponent } from "./UrlComponent";
import { UserProfileAttributeListComponent } from "./UserProfileAttributeListComponent";

export type ComponentProps = Omit<ConfigPropertyRepresentation, "type"> & {
  isDisabled?: boolean;
  isNew?: boolean;
  stringify?: boolean;
};

const ComponentTypes = [
  "String",
  "Text",
  "boolean",
  "List",
  "Role",
  "Script",
  "Map",
  "Group",
  "MultivaluedList",
  "ClientList",
  "UserProfileAttributeList",
  "MultivaluedString",
  "File",
  "Password",
  "Url",
] as const;

export type Components = (typeof ComponentTypes)[number];

export const COMPONENTS: {
  [index in Components]: FunctionComponent<ComponentProps>;
} = {
  String: StringComponent,
  Text: TextComponent,
  boolean: BooleanComponent,
  List: ListComponent,
  Role: RoleComponent,
  Script: ScriptComponent,
  Map: MapComponent,
  Group: GroupComponent,
  ClientList: ClientSelectComponent,
  UserProfileAttributeList: UserProfileAttributeListComponent,
  MultivaluedList: MultiValuedListComponent,
  MultivaluedString: MultiValuedStringComponent,
  File: FileComponent,
  Password: PasswordComponent,
  Url: UrlComponent,
} as const;

export const isValidComponentType = (value: string): value is Components =>
  value in COMPONENTS;
