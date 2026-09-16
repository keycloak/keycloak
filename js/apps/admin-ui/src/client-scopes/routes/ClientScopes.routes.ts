import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type ClientScopesParams = { realm: string };

export const ClientScopesRoutePath = "/:realm/client-scopes";

export const toClientScopes = (params: ClientScopesParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientScopesRoutePath, params),
});
