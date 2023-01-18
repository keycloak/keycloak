import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type FlowParams = {
  realm: string;
  id: string;
  usedBy: string;
  builtIn?: string;
};

export const FlowRoute: RouteDef = {
  path: "/:realm/authentication/:id/:usedBy",
  component: lazy(() => import("../FlowDetails")),
  breadcrumb: (t) => t("authentication:flowDetails"),
  access: "view-authorization",
};

export const FlowWithBuiltInRoute: RouteDef = {
  ...FlowRoute,
  path: "/:realm/authentication/:id/:usedBy/:builtIn",
};

export const toFlow = (params: FlowParams): Partial<Path> => {
  const path = params.builtIn ? FlowWithBuiltInRoute.path : FlowRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
