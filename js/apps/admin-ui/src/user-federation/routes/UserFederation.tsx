import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type UserFederationParams = { realm: string };

const UserFederationSection = lazy(() => import("../UserFederationSection"));

export const UserFederationRoute: AppRouteObject = {
  path: "/:realm/user-federation",
  element: <UserFederationSection />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("userFederation"),
  },
};

export const toUserFederation = (
  params: UserFederationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(UserFederationRoute.path, params),
});
