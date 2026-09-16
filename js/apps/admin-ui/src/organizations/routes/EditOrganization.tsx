import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OrganizationTab =
  | "settings"
  | "attributes"
  | "members"
  | "roles"
  | "groups"
  | "identityProviders"
  | "events";

export type EditOrganizationParams = {
  realm: string;
  id: string;
  tab: OrganizationTab;
};

const DetailOrganization = lazy(() => import("../DetailOrganization"));

export const EditOrganizationRoute: AppRouteObject = {
  path: "/:realm/organizations/:id/:tab/*",
  element: <DetailOrganization />,
  handle: {
    access: "query-organizations",
    breadcrumb: (t) => t("organizationDetails"),
  },
};

export const toEditOrganization = (
  params: EditOrganizationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(EditOrganizationRoute.path, params),
});
