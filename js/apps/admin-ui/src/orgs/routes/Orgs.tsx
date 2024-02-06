import { lazy } from "react";
import { generatePath, Path } from "react-router-dom";
import { AppRouteObject } from "../../routes";

export type OrgsParams = {
  realm: string;
};

const OrgsSection = lazy(() => import("../OrgsSection"));

export const OrgsRoute: AppRouteObject = {
  path: "/:realm/organizations",
  element: <OrgsSection />,
  breadcrumb: (t) => t("orgList"),
  handle: {
    access: "query-clients",
  },
};

export const toOrgs = (params: OrgsParams): Partial<Path> => ({
  pathname: generatePath(OrgsRoute.path, params),
});
