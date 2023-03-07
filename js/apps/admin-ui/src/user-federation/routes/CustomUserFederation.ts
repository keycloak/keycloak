import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { RouteDef } from "../../route-config";

export type CustomUserFederationRouteParams = {
  realm: string;
  providerId: string;
  id: string;
};

export const CustomUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/:providerId/:id",
  component: lazy(() => import("../custom/CustomProviderSettings")),
  breadcrumb: (t) => t("user-federation:providerDetails"),
  access: "view-realm",
};

export const toCustomUserFederation = (
  params: CustomUserFederationRouteParams
): Partial<Path> => ({
  pathname: generatePath(CustomUserFederationRoute.path, params),
});
