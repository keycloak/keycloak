import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { MappingDetails } from "../details/MappingDetails";

export type MapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

export const MapperRoute: RouteDef = {
  path: "/:realm/client-scopes/:id/mappers/:mapperId",
  component: MappingDetails,
  breadcrumb: (t) => t("common:mappingDetails"),
  access: "view-clients",
};

export const toMapper = (params: MapperParams): LocationDescriptorObject => ({
  pathname: generatePath(MapperRoute.path, params),
});
