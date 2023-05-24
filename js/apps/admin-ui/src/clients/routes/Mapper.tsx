import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type MapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

const MappingDetails = lazy(
  () => import("../../client-scopes/details/MappingDetails")
);

export const MapperRoute: AppRouteObject = {
  path: "/:realm/clients/:id/clientScopes/dedicated/mappers/:mapperId",
  element: <MappingDetails />,
  breadcrumb: (t) => t("common:mappingDetails"),
  handle: {
    access: "view-clients",
  },
};

export const toMapper = (params: MapperParams): Partial<Path> => ({
  pathname: generatePath(MapperRoute.path, params),
});
