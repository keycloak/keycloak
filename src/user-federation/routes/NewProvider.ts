import { lazy } from "react";
import { generatePath } from "react-router-dom-v5-compat";

import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ProviderRouteParams = {
  realm: string;
  providerId: string;
  id?: string;
};

export const CustomProviderRoute: RouteDef = {
  path: "/:realm/user-federation/:providerId/new",
  component: lazy(() => import("../custom/CustomProviderSettings")),
  breadcrumb: (t) => t("user-federation:addCustomProvider"),
  access: "view-realm",
};

export const CustomEditProviderRoute: RouteDef = {
  path: "/:realm/user-federation/:providerId/:id",
  component: lazy(() => import("../custom/CustomProviderSettings")),
  breadcrumb: (t) => t("user-federation:providerDetails"),
  access: "view-realm",
};

export const toProvider = (params: ProviderRouteParams): Partial<Path> => ({
  pathname: generatePath(CustomProviderRoute.path, params),
});
