import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateUnencodedPath } from "../../util";
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
  pathname: generateUnencodedPath(MapperRoute.path, params),
});
