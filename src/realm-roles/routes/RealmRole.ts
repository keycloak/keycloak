import type { LocationDescriptorObject } from "history";
import { useRouteMatch } from "react-router-dom";
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
): LocationDescriptorObject => {
  const { url } = useRouteMatch();
  return {
    pathname: `${url}/${params.id}/details`,
  };
};
