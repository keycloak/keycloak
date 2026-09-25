import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type AuthenticationTab = "flows" | "required-actions" | "policies";

export type AuthenticationParams = { realm: string; tab?: AuthenticationTab };

export const AuthenticationRoutePath = "/:realm/authentication";
export const AuthenticationRouteWithTabPath = "/:realm/authentication/:tab";

export const toAuthentication = (
  params: AuthenticationParams,
): Partial<Path> => {
  const path = params.tab
    ? AuthenticationRouteWithTabPath
    : AuthenticationRoutePath;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
