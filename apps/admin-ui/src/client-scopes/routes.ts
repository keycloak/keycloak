import type { RouteDef } from "../route-config";
import {
  ClientScopeRoute,
  ClientScopeWithTypeRoute,
} from "./routes/ClientScope";
import { ClientScopesRoute } from "./routes/ClientScopes";
import { MapperRoute } from "./routes/Mapper";
import { NewClientScopeRoute } from "./routes/NewClientScope";

const routes: RouteDef[] = [
  NewClientScopeRoute,
  MapperRoute,
  ClientScopeRoute,
  ClientScopeWithTypeRoute,
  ClientScopesRoute,
];

export default routes;
