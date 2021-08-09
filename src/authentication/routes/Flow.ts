import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { FlowDetails } from "../FlowDetails";

export type FlowParams = {
  realm: string;
  id: string;
  usedBy: string;
  builtIn?: string;
};

export const FlowRoute: RouteDef = {
  path: "/:realm/authentication/:id/:usedBy/:builtIn?",
  component: FlowDetails,
  breadcrumb: (t) => t("authentication:flowDetails"),
  access: "manage-authorization",
};

export const toFlow = (params: FlowParams): LocationDescriptorObject => ({
  pathname: generatePath(FlowRoute.path, params),
});
