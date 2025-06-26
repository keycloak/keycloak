import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
/** TIDECLOAK IMPLEMENTATION */

export type ChangeRequestsTab = "users" | "roles" | "clients";

export type ChangeRequestsParams = { realm: string; tab?: ChangeRequestsTab };

const ChangeRequestsSection = lazy(() => import("../ChangeRequestsSection"));

export const ChangeRequestsRoute: AppRouteObject = {
  path: "/:realm/change-requests",
  element: <ChangeRequestsSection />,
  breadcrumb: (t) => t("changeRequestsList"),
  handle: {
    access: "query-users", // update this to some appropriate
  },
};

export const ChangeRequestsRouteWithTab: AppRouteObject = {
  ...ChangeRequestsRoute,
  path: "/:realm/change-requests/:tab",
};

export const toChangeRequests = (params: ChangeRequestsParams): Partial<Path> => {
  const path = params.tab ? ChangeRequestsRouteWithTab.path : ChangeRequestsRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
