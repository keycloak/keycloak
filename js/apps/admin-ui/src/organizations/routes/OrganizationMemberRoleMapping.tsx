import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OrganizationMemberRoleMappingParams = {
  realm: string;
  orgId: string;
  userId: string;
};

const OrganizationMemberRoleMappingDetails = lazy(() => import("../OrganizationMemberRoleMappingDetails"));

export const OrganizationMemberRoleMappingRoute: AppRouteObject = {
  path: "/:realm/organizations/:orgId/members/:userId/role-mapping",
  element: <OrganizationMemberRoleMappingDetails />,
  breadcrumb: (t) => t("organizationMemberRoleMapping"),
  handle: {
    access: "manage-users",
  },
};

export const toOrganizationMemberRoleMapping = (
  params: OrganizationMemberRoleMappingParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(OrganizationMemberRoleMappingRoute.path, params),
});
