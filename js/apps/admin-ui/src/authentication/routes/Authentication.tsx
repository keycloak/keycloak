import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  AuthenticationRoutePath,
  AuthenticationRouteWithTabPath,
  type AuthenticationParams,
  type AuthenticationTab,
  toAuthentication,
} from "./Authentication.routes";

export type { AuthenticationParams, AuthenticationTab };
export { toAuthentication };

const AuthenticationSection = lazy(() => import("../AuthenticationSection"));

export const AuthenticationRoute: AppRouteObject = {
  path: AuthenticationRoutePath,
  element: <AuthenticationSection />,
  handle: {
    access: ["view-realm", "view-identity-providers", "view-clients"],
    breadcrumb: (t) => t("authentication"),
  },
};

export const AuthenticationRouteWithTab: AppRouteObject = {
  ...AuthenticationRoute,
  path: AuthenticationRouteWithTabPath,
};
