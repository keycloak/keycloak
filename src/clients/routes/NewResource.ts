import type { LocationDescriptorObject } from "history";
import type { RouteDef } from "../../route-config";
import { generatePath } from "react-router-dom";
import { lazy } from "react";

export type NewResourceParams = { realm: string; id: string };

export const NewResourceRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/resource/new",
  component: lazy(() => import("../authorization/ResourceDetails")),
  breadcrumb: (t) => t("clients:createResource"),
  access: "manage-clients",
};

export const toCreateResource = (
  params: NewResourceParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewResourceRoute.path, params),
});
