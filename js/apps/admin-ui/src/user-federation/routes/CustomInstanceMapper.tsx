import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { AppRouteObject } from "../../routes";

export type CustomUserFederationMapperRouteParams = {
  realm: string;
  providerId: string;
  parentId: string;
  id?: string;
};

const CustomInstanceMapperSettings = lazy(
  () => import("../custom/CustomInstanceMapperSettings"),
);

export const CreateCustomUserFederationMapperRoute: AppRouteObject = {
  path: "/:realm/user-federation/:providerId/:parentId/mappers/create",
  element: <CustomInstanceMapperSettings />,
  breadcrumb: (t) => t("userFederation.custom.mapper.createBreadcrumb"),
  handle: {
    access: "view-realm",
  },
};

export const UpdateCustomUserFederationMapperRoute: AppRouteObject = {
  path: "/:realm/user-federation/:providerId/:parentId/mappers/:id",
  element: <CustomInstanceMapperSettings />,
  breadcrumb: (t) => t("userFederation.custom.mapper.updateBreadcrumb"),
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationMapper = (
  params: CustomUserFederationMapperRouteParams,
): Partial<Path> => {
  return params.id
    ? {
        pathname: generatePath(
          UpdateCustomUserFederationMapperRoute.path,
          params,
        ),
      }
    : {
        pathname: generatePath(
          CreateCustomUserFederationMapperRoute.path,
          params,
        ),
      };
};
