import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RealmRolesParams = { realm: string };

export const RealmRolesRoute: RouteDef = {
  path: "/:realm/roles",
  component: lazy(() => import("../RealmRolesSection")),
  breadcrumb: (t) => t("roles:roleList"),
  access: "view-realm",
};

export const toRealmRoles = (
  params: RealmRolesParams
): LocationDescriptorObject => ({
  pathname: generatePath(RealmRolesRoute.path, params),
});
