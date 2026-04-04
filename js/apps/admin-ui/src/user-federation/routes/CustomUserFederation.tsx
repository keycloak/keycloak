import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

import type { AppRouteObject } from "../../routes";

export type CustomUserFederationRouteParams = {
  realm: string;
  providerId: string;
  id: string;
};

const CustomProviderSettings = lazy(
  () => import("../custom/CustomProviderSettings"),
);

export const CustomUserFederationRoute: AppRouteObject = {
  path: "/:realm/user-federation/:providerId/:id",
  element: <CustomProviderSettings />,
  breadcrumb: (t) => t("providerDetails"),
  handle: {
    access: "view-realm",
  },
};

export const toCustomUserFederation = (
  params: CustomUserFederationRouteParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(CustomUserFederationRoute.path, params),
});
