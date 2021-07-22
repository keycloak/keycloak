import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RealmRolesSection } from "../RealmRolesSection";

export type RealmRolesParams = { realm: string };

export const RealmRolesRoute: RouteDef = {
  path: "/:realm/roles",
  component: RealmRolesSection,
  breadcrumb: (t) => t("roles:roleList"),
  access: "view-realm",
};

export const toRealmRoles = (
  params: RealmRolesParams
): LocationDescriptorObject => ({
  pathname: generatePath(RealmRolesRoute.path, params),
});
