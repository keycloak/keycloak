import { lazy } from "react";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type OrganizationRoleTab =
  | "details"
  | "associated-roles"
  | "attributes"
  | "users-in-role"
  | "events";

export type OrganizationRoleParams = {
  realm: string;
  orgId: string;
  roleId: string;
  tab: OrganizationRoleTab;
};

const OrganizationRoleDetails = lazy(
  () => import("../OrganizationRoleDetails"),
);

export const OrganizationRoleRoute: AppRouteObject = {
  path: "/:realm/organizations/:orgId/roles/:roleId/:tab/*",
  element: <OrganizationRoleDetails />,
  handle: {
    access: "view-organizations",
    breadcrumb: (t) => t("organizationRoleDetails"),
  },
};

export const toOrganizationRole = (
  params: OrganizationRoleParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(OrganizationRoleRoute.path, params),
});
