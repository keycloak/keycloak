import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { CreateInitialAccessToken } from "../initial-access/CreateInitialAccessToken";

export type CreateInitialAccessTokenParams = { realm: string };

export const CreateInitialAccessTokenRoute: RouteDef = {
  path: "/:realm/clients/initialAccessToken/create",
  component: CreateInitialAccessToken,
  breadcrumb: (t) => t("clients:createToken"),
  access: "manage-clients",
};

export const toCreateInitialAccessToken = (
  params: CreateInitialAccessTokenParams
): LocationDescriptorObject => ({
  pathname: generatePath(CreateInitialAccessTokenRoute.path, params),
});
