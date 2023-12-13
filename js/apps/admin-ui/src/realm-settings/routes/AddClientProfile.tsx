import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddClientProfileParams = {
  realm: string;
  tab: string;
};

const ClientProfileForm = lazy(() => import("../ClientProfileForm"));

export const AddClientProfileRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/:tab/add-profile",
  element: <ClientProfileForm />,
  breadcrumb: (t) => t("newClientProfile"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddClientProfile = (
  params: AddClientProfileParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(AddClientProfileRoute.path, params),
});
