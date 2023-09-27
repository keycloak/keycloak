import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type MapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

const MappingDetails = lazy(() => import("../details/MappingDetails"));

export const MapperRoute: AppRouteObject = {
  path: "/:realm/client-scopes/:id/mappers/:mapperId",
  element: <MappingDetails />,
  breadcrumb: (t) => t("mappingDetails"),
  handle: {
    access: "view-clients",
  },
};

export const toMapper = (params: MapperParams): Partial<Path> => ({
  pathname: generatePath(MapperRoute.path, params),
});
