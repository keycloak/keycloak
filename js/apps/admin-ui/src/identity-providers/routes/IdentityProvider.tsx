import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderTab =
  | "settings"
  | "mappers"
  | "permissions"
  | "events";

export type IdentityProviderParams = {
  realm: string;
  providerId: string;
  alias: string;
  tab: IdentityProviderTab;
};

const DetailSettings = lazy(() => import("../add/DetailSettings"));

export const IdentityProviderRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/:alias/:tab",
  element: <DetailSettings />,
  handle: {
    access: "view-identity-providers",
    breadcrumb: (t) => t("providerDetails"),
  },
};

export const toIdentityProvider = (
  params: IdentityProviderParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderRoute.path, params),
});
