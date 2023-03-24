import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { RouteDef } from "../../route-config";

export type CustomUserFederationRouteParams = {
  realm: string;
  providerId: string;
  id: string;
};

const CustomProviderSettings = lazy(
  () => import("../custom/CustomProviderSettings")
);

export const CustomUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/:providerId/:id",
  element: <CustomProviderSettings />,
  breadcrumb: (t) => t("user-federation:providerDetails"),
  handle: {
    access: "view-realm",
  },
};

export const toCustomUserFederation = (
  params: CustomUserFederationRouteParams
): Partial<Path> => ({
  pathname: generatePath(CustomUserFederationRoute.path, params),
});
