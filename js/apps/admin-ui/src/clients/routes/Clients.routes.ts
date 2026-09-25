import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type ClientsTab =
  | "list"
  | "initial-access-token"
  | "client-registration";

export type ClientsParams = {
  realm: string;
  tab?: ClientsTab;
};

export const ClientsRoutePath = "/:realm/clients";
export const ClientsRouteWithTabPath = "/:realm/clients/:tab";

export const toClients = (params: ClientsParams): Partial<Path> => {
  const path = params.tab ? ClientsRouteWithTabPath : ClientsRoutePath;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
