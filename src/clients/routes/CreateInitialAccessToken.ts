import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type CreateInitialAccessTokenParams = { realm: string };

export const CreateInitialAccessTokenRoute: RouteDef = {
  path: "/:realm/clients/initialAccessToken/create",
  component: lazy(() => import("../initial-access/CreateInitialAccessToken")),
  breadcrumb: (t) => t("clients:createToken"),
  access: "manage-clients",
};

export const toCreateInitialAccessToken = (
  params: CreateInitialAccessTokenParams
): LocationDescriptorObject => ({
  pathname: generatePath(CreateInitialAccessTokenRoute.path, params),
});
