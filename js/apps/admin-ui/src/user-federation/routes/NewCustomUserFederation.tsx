import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateUnencodedPath } from "../../util";

import type { AppRouteObject } from "../../routes";

export type NewCustomUserFederationRouteParams = {
  realm: string;
  providerId: string;
};

const CustomProviderSettings = lazy(
  () => import("../custom/CustomProviderSettings"),
);

export const NewCustomUserFederationRoute: AppRouteObject = {
  path: "/:realm/user-federation/:providerId/new",
  element: <CustomProviderSettings />,
  breadcrumb: (t) => t("addCustomProvider"),
  handle: {
    access: "view-realm",
  },
};

export const toNewCustomUserFederation = (
  params: NewCustomUserFederationRouteParams,
): Partial<Path> => ({
  pathname: generateUnencodedPath(NewCustomUserFederationRoute.path, params),
});
