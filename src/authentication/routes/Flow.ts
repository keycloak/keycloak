import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type FlowParams = {
  realm: string;
  id: string;
  usedBy: string;
  builtIn?: string;
};

export const FlowRoute: RouteDef = {
  path: "/:realm/authentication/:id/:usedBy/:builtIn?",
  component: lazy(() => import("../FlowDetails")),
  breadcrumb: (t) => t("authentication:flowDetails"),
  access: "view-authorization",
  legacy: true,
};

export const toFlow = (params: FlowParams): Partial<Path> => ({
  pathname: generatePath(FlowRoute.path, params),
});
