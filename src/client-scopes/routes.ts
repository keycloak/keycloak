import type { RouteDef } from "../route-config";
import { ClientScopeRoute } from "./routes/ClientScope";
import { ClientScopesRoute } from "./routes/ClientScopes";
import { MapperRoute } from "./routes/Mapper";
import { NewClientScopeRoute } from "./routes/NewClientScope";
import { OidcRoleNameMapperRoute } from "./routes/OidcRoleNameMapper";

const routes: RouteDef[] = [
  NewClientScopeRoute,
  OidcRoleNameMapperRoute,
  MapperRoute,
  ClientScopeRoute,
  ClientScopesRoute,
];

export default routes;
