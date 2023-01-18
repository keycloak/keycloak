import { lazy } from "react";
import type { Path } from "react-router-dom";
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
): Partial<Path> => ({
  pathname: generatePath(CreateInitialAccessTokenRoute.path, params),
});
