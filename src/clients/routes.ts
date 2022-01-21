import type { RouteDef } from "../route-config";
import { AddClientRoute } from "./routes/AddClient";
import { ClientRoute } from "./routes/Client";
import { ClientsRoute } from "./routes/Clients";
import { CreateInitialAccessTokenRoute } from "./routes/CreateInitialAccessToken";
import { ImportClientRoute } from "./routes/ImportClient";
import { MapperRoute } from "./routes/Mapper";
import { NewResourceRoute } from "./routes/NewResource";
import { ResourceDetailsRoute } from "./routes/Resource";
import { NewScopeRoute } from "./routes/NewScope";
import { ScopeDetailsRoute } from "./routes/Scope";
import { NewPolicyRoute } from "./routes/NewPolicy";
import { PolicyDetailsRoute } from "./routes/PolicyDetails";
import { NewPermissionRoute } from "./routes/NewPermission";
import { PermissionDetailsRoute } from "./routes/PermissionDetails";

const routes: RouteDef[] = [
  AddClientRoute,
  ImportClientRoute,
  ClientsRoute,
  CreateInitialAccessTokenRoute,
  ClientRoute,
  MapperRoute,
  NewResourceRoute,
  ResourceDetailsRoute,
  NewScopeRoute,
  ScopeDetailsRoute,
  NewPolicyRoute,
  PolicyDetailsRoute,
  NewPermissionRoute,
  PermissionDetailsRoute,
];

export default routes;
