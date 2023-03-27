import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddClientProfileParams = {
  realm: string;
  tab: string;
};

const ClientProfileForm = lazy(() => import("../ClientProfileForm"));

export const AddClientProfileRoute: RouteDef = {
  path: "/:realm/realm-settings/client-policies/:tab/add-profile",
  element: <ClientProfileForm />,
  breadcrumb: (t) => t("realm-settings:newClientProfile"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddClientProfile = (
  params: AddClientProfileParams
): Partial<Path> => ({
  pathname: generatePath(AddClientProfileRoute.path, params),
});
