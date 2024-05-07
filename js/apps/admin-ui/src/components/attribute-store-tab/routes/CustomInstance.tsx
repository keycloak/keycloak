import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { AppRouteObject } from "../../../routes";

export type CustomAttributeStoreInstanceTab = "settings";

export type CustomAttributeStoreInstanceRouteParams = {
  realm: string;
  providerId: string;
  id?: string;
  tab?: CustomAttributeStoreInstanceTab;
};

const CustomInstanceSettings = lazy(() => import("../pages/CustomInstance"));

export const CreateCustomAttributeStoreInstanceRoute: AppRouteObject = {
  path: "/:realm/attributeStore/:providerId/create",
  element: <CustomInstanceSettings />,
  breadcrumb: (t) => t("attributeStore.providers.custom.createBreadcrumb"),
  handle: {
    access: "view-realm",
  },
};

export const UpdateCustomAttributeStoreInstanceRoute: AppRouteObject = {
  path: "/:realm/attributeStore/:providerId/:id",
  element: <CustomInstanceSettings />,
  breadcrumb: (t) => t("attributeStore.providers.custom.updateBreadcrumb"),
  handle: {
    access: "view-realm",
  },
};

export const UpdateCustomAttributeStoreInstanceRouteWithTab: AppRouteObject = {
  ...UpdateCustomAttributeStoreInstanceRoute,
  path: "/:realm/attributeStore/:providerId/:id/:tab",
};

export const toCustomAttributeStoreInstance = (
  params: CustomAttributeStoreInstanceRouteParams,
): Partial<Path> => {
  const path =
    params.tab && params.id
      ? UpdateCustomAttributeStoreInstanceRouteWithTab.path
      : params.id
        ? UpdateCustomAttributeStoreInstanceRoute.path
        : CreateCustomAttributeStoreInstanceRoute.path;
  return {
    pathname: generatePath(path, params),
  };
};
