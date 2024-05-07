import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

import type { AppRouteObject } from "../../routes";

export type CustomUserFederationInstanceTab = "settings" | "mappers";

export type CustomUserFederationRouteParams = {
  realm: string;
  providerId: string;
  id?: string;
  tab?: CustomUserFederationInstanceTab;
};

const CustomProviderSettings = lazy(() => import("../custom/CustomInstance"));

export const NewCustomUserFederationInstanceRoute: AppRouteObject = {
  path: "/:realm/user-federation/:providerId/new",
  element: <CustomProviderSettings />,
  breadcrumb: (t) => t("addCustomProvider"),
  handle: {
    access: "view-realm",
  },
};

export const UpdateCustomUserFederationInstanceRoute: AppRouteObject = {
  path: "/:realm/user-federation/:providerId/:id",
  element: <CustomProviderSettings />,
  breadcrumb: (t) => t("editProvider"),
  handle: {
    access: "view-realm",
  },
};

export const UpdateCustomUserFederationInstanceRouteWithTab: AppRouteObject = {
  ...UpdateCustomUserFederationInstanceRoute,
  path: "/:realm/user-federation/:providerId/:id/:tab",
};

export const toCustomUserFederation = (
  params: CustomUserFederationRouteParams,
): Partial<Path> => {
  const path =
    params.tab && params.id
      ? UpdateCustomUserFederationInstanceRouteWithTab.path
      : params.id
        ? UpdateCustomUserFederationInstanceRoute.path
        : NewCustomUserFederationInstanceRoute.path;
  return {
    pathname: generateEncodedPath(path, params),
  };
};
