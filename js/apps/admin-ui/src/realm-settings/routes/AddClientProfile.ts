import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddClientProfileParams = {
  realm: string;
  tab: string;
};

export const AddClientProfileRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:tab/add-profile",
  component: lazy(() => import("../ClientProfileForm")),
  breadcrumb: (t) => t("realm-settings:newClientProfile"),
  access: "manage-realm",
};

export const toAddClientProfile = (
  params: AddClientProfileParams
): Partial<Path> => ({
  pathname: generatePath(AddClientProfileRoute.path, params),
});
