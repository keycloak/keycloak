import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type FlowParams = {
  realm: string;
  id: string;
  usedBy: string;
  builtIn?: string;
};

const FlowDetails = lazy(() => import("../FlowDetails"));

export const FlowRoute: AppRouteObject = {
  path: "/:realm/authentication/:id/:usedBy",
  element: <FlowDetails />,
  breadcrumb: (t) => t("authentication:flowDetails"),
  handle: {
    access: "view-authorization",
  },
};

export const FlowWithBuiltInRoute: AppRouteObject = {
  ...FlowRoute,
  path: "/:realm/authentication/:id/:usedBy/:builtIn",
};

export const toFlow = (params: FlowParams): Partial<Path> => {
  const path = params.builtIn ? FlowWithBuiltInRoute.path : FlowRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
