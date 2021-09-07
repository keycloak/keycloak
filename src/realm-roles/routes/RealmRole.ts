import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router";
import type { RouteDef } from "../../route-config";
import { RealmRoleTabs } from "../RealmRoleTabs";

export type RealmRoleTab =
  | "details"
  | "AssociatedRoles"
  | "attributes"
  | "users-in-role";

export type RealmRoleParams = {
  realm: string;
  id: string;
  tab?: RealmRoleTab;
};

export const RealmRoleRoute: RouteDef = {
  path: "/:realm/roles/:id/:tab?",
  component: RealmRoleTabs,
  breadcrumb: (t) => t("roles:roleDetails"),
  access: ["view-realm", "view-users"],
};

export const toRealmRole = (
  params: RealmRoleParams
): LocationDescriptorObject => ({
  pathname: generatePath(RealmRoleRoute.path, params),
});
