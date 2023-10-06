import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateUnencodedPath } from "../../util";
import type { AppRouteObject } from "../../routes";

export type ClientProfileParams = {
  realm: string;
  profileName: string;
};

const ClientProfileForm = lazy(() => import("../ClientProfileForm"));

export const ClientProfileRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/:profileName/edit-profile",
  element: <ClientProfileForm />,
  breadcrumb: (t) => t("clientProfile"),
  handle: {
    access: ["view-realm", "view-users"],
  },
};

export const toClientProfile = (
  params: ClientProfileParams,
): Partial<Path> => ({
  pathname: generateUnencodedPath(ClientProfileRoute.path, params),
});
