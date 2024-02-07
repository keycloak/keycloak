import { lazy } from "react";
import { generatePath, Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type OrgTab =
  | "settings"
  | "attributes"
  | "members"
  | "clients"
  | "invitations"
  | "roles"
  | "identityproviders";

export type OrgParams = {
  realm: string;
  orgId: string;
  tab: OrgTab;
};

const OrgDetails = lazy(() => import("../OrgDetails"));

export const OrgRoute: AppRouteObject = {
  path: "/:realm/organizations/:orgId/:tab",
  element: <OrgDetails />,
  breadcrumb: (t) => t("orgDetails"),
  handle: {
    access: "view-clients",
  },
};

export const toOrg = (params: OrgParams): Partial<Path> => ({
  pathname: generatePath(OrgRoute.path, params),
});
