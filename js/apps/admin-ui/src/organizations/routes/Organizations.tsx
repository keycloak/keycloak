import { lazy } from "react";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

const OrganizationsSection = lazy(() => import("../OrganizationsSection"));

export const OrganizationsRoute: AppRouteObject = {
  path: "/:realm/organizations",
  element: <OrganizationsSection />,
  breadcrumb: (t) => t("organizationsList"),
  handle: {
    access: "query-groups",
  },
};

export const toClients = (): Partial<Path> => {
  const path = OrganizationsRoute.path;

  return {
    pathname: path,
  };
};
