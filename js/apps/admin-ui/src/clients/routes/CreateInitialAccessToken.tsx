import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type CreateInitialAccessTokenParams = { realm: string };

const CreateInitialAccessToken = lazy(
  () => import("../initial-access/CreateInitialAccessToken"),
);

export const CreateInitialAccessTokenRoute: AppRouteObject = {
  path: "/:realm/clients/initialAccessToken/create",
  element: <CreateInitialAccessToken />,
  handle: {
    access: "manage-clients",
    breadcrumb: (t) => t("createToken"),
  },
};

export const toCreateInitialAccessToken = (
  params: CreateInitialAccessTokenParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(CreateInitialAccessTokenRoute.path, params),
});
