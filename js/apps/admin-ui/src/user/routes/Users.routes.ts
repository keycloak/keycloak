import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type UserTab = "list" | "permissions";

export type UsersParams = { realm: string; tab?: UserTab };

export const UsersRoutePath = "/:realm/users";
export const UsersRouteWithTabPath = "/:realm/users/:tab";

export const toUsers = (params: UsersParams): Partial<Path> => {
  const path = params.tab ? UsersRouteWithTabPath : UsersRoutePath;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
