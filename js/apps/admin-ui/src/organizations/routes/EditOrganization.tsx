import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OrganizationTab =
  | "settings"
  | "attributes"
  | "members"
  | "identityProviders"
  | "events";

export type EditOrganizationParams = {
  realm: string;
  id: string;
  tab: OrganizationTab;
};

const DetailOrganization = lazy(() => import("../DetailOrganization"));

export const EditOrganizationRoute: AppRouteObject = {
  path: "/:realm/organizations/:id/:tab",
  element: <DetailOrganization />,
  breadcrumb: (t) => t("organizationDetails"),
  handle: {
    access: "manage-users",
  },
};

export const toEditOrganization = (
  params: EditOrganizationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(EditOrganizationRoute.path, params),
});
