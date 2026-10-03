import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type NewClientScopeParams = { realm: string };

export const NewClientScopeRoutePath = "/:realm/client-scopes/new";

export const toNewClientScope = (
  params: NewClientScopeParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(NewClientScopeRoutePath, params),
});
