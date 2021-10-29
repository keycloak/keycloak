import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type MapperParams = {
  realm: string;
  id: string;
  type: string;
  mapperId: string;
};

export const MapperRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/:type/mappers/:mapperId",
  component: lazy(() => import("../details/MappingDetails")),
  breadcrumb: (t) => t("common:mappingDetails"),
  access: "view-clients",
};

export const toMapper = (params: MapperParams): LocationDescriptorObject => ({
  pathname: generatePath(MapperRoute.path, params),
});
