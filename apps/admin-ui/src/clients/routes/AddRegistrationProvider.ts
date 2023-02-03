import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ClientRegistrationTab } from "./ClientRegistration";

export type RegistrationProviderParams = {
  realm: string;
  subTab: ClientRegistrationTab;
  id?: string;
  providerId: string;
};

export const AddRegistrationProviderRoute: RouteDef = {
  path: "/:realm/clients/client-registration/:subTab/:providerId",
  component: lazy(() => import("../registration/DetailProvider")),
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "manage-clients",
};

export const EditRegistrationProviderRoute: RouteDef = {
  ...AddRegistrationProviderRoute,
  path: "/:realm/clients/client-registration/:subTab/:providerId/:id",
};

export const toRegistrationProvider = (
  params: RegistrationProviderParams
): Partial<Path> => {
  const path = params.id
    ? EditRegistrationProviderRoute.path
    : AddRegistrationProviderRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
