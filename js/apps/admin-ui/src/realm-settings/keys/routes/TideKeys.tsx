import { lazy } from "react";
import type { AppRouteObject } from "../../../routes";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../../utils/generateEncodedPath";
import { KeyProviderForm } from "../key-providers/KeyProviderForm";
import { ProviderType } from "../../routes/KeyProvider";

/** TIDECLOAK IMPLEMENTATION */
export type TideKeyTab = "settings" | "license";

export type TideKeyParams = 
{ 
    realm: string; 
    id: string; 
    providerType: ProviderType;
    tab?: TideKeyTab;
};

const TideKeyForm = lazy(() => import("../key-providers/KeyProviderForm"));

export const TideKeyRoute: AppRouteObject = {
  path: "/:realm/realm-settings/keys/providers/:id/:providerType/settings",
  element: <TideKeyForm />,
  breadcrumb: (t) => t("tideKeyForm"),
  handle: {
    access: "view-realm",
  },
};

export const TideKeyRouteWithTab: AppRouteObject = {
  ...TideKeyRoute,
  path: "/:realm/realm-settings/keys/providers/:id/:providerType/settings/:tab",
};

export const toTideKey = (params: TideKeyParams): Partial<Path> => {
  const path = params.tab ? TideKeyRouteWithTab.path : TideKeyRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
