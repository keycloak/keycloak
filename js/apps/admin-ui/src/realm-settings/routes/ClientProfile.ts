import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ClientProfileParams = {
  realm: string;
  profileName: string;
};

export const ClientProfileRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:profileName/edit-profile",
  component: lazy(() => import("../ClientProfileForm")),
  breadcrumb: (t) => t("realm-settings:clientProfile"),
  access: ["view-realm", "view-users"],
};

export const toClientProfile = (
  params: ClientProfileParams
): Partial<Path> => ({
  pathname: generatePath(ClientProfileRoute.path, params),
});
