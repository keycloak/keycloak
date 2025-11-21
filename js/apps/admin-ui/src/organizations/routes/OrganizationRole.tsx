import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OrganizationRoleTab =
  | "details"
  | "associated-roles"
  | "attributes"
  | "members"
  | "permissions"
  | "events";

export type OrganizationRoleParams = {
  realm: string;
  orgId: string;
  roleId: string;
  tab: OrganizationRoleTab;
};

const OrganizationRoleTabs = lazy(() => import("../OrganizationRoleTabs"));

export const OrganizationRoleRoute: AppRouteObject = {
  path: "/:realm/organizations/:orgId/roles/:roleId/:tab",
  element: <OrganizationRoleTabs />,
  breadcrumb: (t) => t("organizationRoleDetails"),
  handle: {
    access: "manage-users",
  },
};

export const toOrganizationRole = (params: OrganizationRoleParams): Partial<Path> => ({
  pathname: generateEncodedPath(OrganizationRoleRoute.path, params),
});
