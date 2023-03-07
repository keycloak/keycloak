import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { RouteDef } from "../../route-config";

export type NewCustomUserFederationRouteParams = {
  realm: string;
  providerId: string;
};

export const NewCustomUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/:providerId/new",
  component: lazy(() => import("../custom/CustomProviderSettings")),
  breadcrumb: (t) => t("user-federation:addCustomProvider"),
  access: "view-realm",
};

export const toNewCustomUserFederation = (
  params: NewCustomUserFederationRouteParams
): Partial<Path> => ({
  pathname: generatePath(NewCustomUserFederationRoute.path, params),
});
