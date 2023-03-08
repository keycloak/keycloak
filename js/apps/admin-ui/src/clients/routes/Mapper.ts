import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type MapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

export const MapperRoute: RouteDef = {
  path: "/:realm/clients/:id/clientScopes/dedicated/mappers/:mapperId",
  component: lazy(() => import("../../client-scopes/details/MappingDetails")),
  breadcrumb: (t) => t("common:mappingDetails"),
  access: "view-clients",
};

export const toMapper = (params: MapperParams): Partial<Path> => ({
  pathname: generatePath(MapperRoute.path, params),
});
