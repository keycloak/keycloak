import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientRegistrationTab = "anonymous" | "authenticated";

export type ClientRegistrationParams = {
  realm: string;
  subTab: ClientRegistrationTab;
};

export const ClientRegistrationRoute: RouteDef = {
  path: "/:realm/clients/client-registration/:subTab",
  component: lazy(() => import("../ClientsSection")),
  breadcrumb: (t) => t("clients:clientRegistration"),
  access: "view-clients",
};

export const toClientRegistration = (
  params: ClientRegistrationParams
): Partial<Path> => ({
  pathname: generatePath(ClientRegistrationRoute.path, params),
});
