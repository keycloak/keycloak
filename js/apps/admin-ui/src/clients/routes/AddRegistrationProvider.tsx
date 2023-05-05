import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";
import { ClientRegistrationTab } from "./ClientRegistration";

export type RegistrationProviderParams = {
  realm: string;
  subTab: ClientRegistrationTab;
  id?: string;
  providerId: string;
};

const DetailProvider = lazy(() => import("../registration/DetailProvider"));

export const AddRegistrationProviderRoute: AppRouteObject = {
  path: "/:realm/clients/client-registration/:subTab/:providerId",
  element: <DetailProvider />,
  breadcrumb: (t) => t("clients:clientSettings"),
  handle: {
    access: "manage-clients",
  },
};

export const EditRegistrationProviderRoute: AppRouteObject = {
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
