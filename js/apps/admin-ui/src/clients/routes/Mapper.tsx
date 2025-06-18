import { lazy } from "react";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type MapperParams = {
  realm: string;
  id: string;
  mapperId: string;
  viewMode: "edit" | "new";
};

const MappingDetails = lazy(
  () => import("../../client-scopes/details/MappingDetails"),
);

export const MapperRoute: AppRouteObject = {
  path: "/:realm/clients/:id/clientScopes/dedicated/mappers/:mapperId/:viewMode",
  element: <MappingDetails />,
  breadcrumb: (t) => t("mappingDetails"),
  handle: {
    access: "view-clients",
  },
};

export const toMapper = (params: MapperParams): Partial<Path> => ({
  pathname: generateEncodedPath(MapperRoute.path, params),
});
