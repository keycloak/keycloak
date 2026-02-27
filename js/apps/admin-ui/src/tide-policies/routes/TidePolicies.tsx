/** TIDECLOAK IMPLEMENTATION */

import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type TidePoliciesTab = "overview" | "forseti-contracts";

export type TidePoliciesParams = { realm: string; tab?: TidePoliciesTab };

const TidePoliciesSection = lazy(() => import("../TidePoliciesSection"));

export const TidePoliciesRoute: AppRouteObject = {
  path: "/:realm/tide-policies",
  element: <TidePoliciesSection />,
  breadcrumb: (t) => t("Policies"),
  handle: {
    access: "query-users",
  },
};

export const TidePoliciesRouteWithTab: AppRouteObject = {
  ...TidePoliciesRoute,
  path: "/:realm/tide-policies/:tab",
};

export const toTidePolicies = (
  params: TidePoliciesParams,
): Partial<Path> => {
  const path = params.tab
    ? TidePoliciesRouteWithTab.path
    : TidePoliciesRoute.path;
  return {
    pathname: generateEncodedPath(path, params),
  };
};
