import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RealmRoleTabs } from "../RealmRoleTabs";

export type ClientRoleTab = "details" | "attributes" | "users-in-role";

export type ClientRoleParams = {
  realm: string;
  clientId: string;
  id: string;
  tab?: ClientRoleTab;
};

export const ClientRoleRoute: RouteDef = {
  path: "/:realm/clients/:clientId/roles/:id/:tab?",
  component: RealmRoleTabs,
  breadcrumb: (t) => t("roles:roleDetails"),
  access: "view-realm",
};

export const toClientRole = (
  params: ClientRoleParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientRoleRoute.path, params),
});
