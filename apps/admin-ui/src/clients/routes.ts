import type { RouteDef } from "../route-config";
import { AddClientRoute } from "./routes/AddClient";
import { ClientRoute } from "./routes/Client";
import { ClientsRoute, ClientsRouteWithTab } from "./routes/Clients";
import { CreateInitialAccessTokenRoute } from "./routes/CreateInitialAccessToken";
import { ImportClientRoute } from "./routes/ImportClient";
import { MapperRoute } from "./routes/Mapper";
import { ClientScopesRoute } from "./routes/ClientScopeTab";
import { AuthorizationRoute } from "./routes/AuthenticationTab";
import { NewResourceRoute } from "./routes/NewResource";
import {
  ResourceDetailsRoute,
  ResourceDetailsWithResourceIdRoute,
} from "./routes/Resource";
import { NewScopeRoute } from "./routes/NewScope";
import {
  ScopeDetailsRoute,
  ScopeDetailsWithScopeIdRoute,
} from "./routes/Scope";
import { NewPolicyRoute } from "./routes/NewPolicy";
import { PolicyDetailsRoute } from "./routes/PolicyDetails";
import {
  NewPermissionRoute,
  NewPermissionWithSelectedIdRoute,
} from "./routes/NewPermission";
import { PermissionDetailsRoute } from "./routes/PermissionDetails";
import {
  DedicatedScopeDetailsRoute,
  DedicatedScopeDetailsWithTabRoute,
} from "./routes/DedicatedScopeDetails";

const routes: RouteDef[] = [
  AddClientRoute,
  ImportClientRoute,
  ClientsRoute,
  ClientsRouteWithTab,
  CreateInitialAccessTokenRoute,
  ClientRoute,
  MapperRoute,
  DedicatedScopeDetailsRoute,
  DedicatedScopeDetailsWithTabRoute,
  ClientScopesRoute,
  AuthorizationRoute,
  NewResourceRoute,
  ResourceDetailsRoute,
  ResourceDetailsWithResourceIdRoute,
  NewScopeRoute,
  ScopeDetailsRoute,
  ScopeDetailsWithScopeIdRoute,
  NewPolicyRoute,
  PolicyDetailsRoute,
  NewPermissionRoute,
  NewPermissionWithSelectedIdRoute,
  PermissionDetailsRoute,
];

export default routes;
