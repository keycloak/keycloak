import type { AppRouteObject } from "../routes";
import { AddClientRoute } from "./routes/AddClient";
import {
  AddRegistrationProviderRoute,
  EditRegistrationProviderRoute,
} from "./routes/AddRegistrationProvider";
import { AuthorizationRoute } from "./routes/AuthenticationTab";
import { ClientRoute } from "./routes/Client";
import { ClientRegistrationRoute } from "./routes/ClientRegistration";
import { ClientRoleRoute } from "./routes/ClientRole";
import { ClientsRoute, ClientsRouteWithTab } from "./routes/Clients";
import { ClientScopesRoute } from "./routes/ClientScopeTab";
import { CreateInitialAccessTokenRoute } from "./routes/CreateInitialAccessToken";
import {
  DedicatedScopeDetailsRoute,
  DedicatedScopeDetailsWithTabRoute,
} from "./routes/DedicatedScopeDetails";
import { ImportClientRoute } from "./routes/ImportClient";
import { MapperRoute } from "./routes/Mapper";
import {
  NewPermissionRoute,
  NewPermissionWithSelectedIdRoute,
} from "./routes/NewPermission";
import { NewPolicyRoute } from "./routes/NewPolicy";
import { NewResourceRoute } from "./routes/NewResource";
import { NewRoleRoute } from "./routes/NewRole";
import { NewScopeRoute } from "./routes/NewScope";
import { PermissionDetailsRoute } from "./routes/PermissionDetails";
import { PolicyDetailsRoute } from "./routes/PolicyDetails";
import {
  ResourceDetailsRoute,
  ResourceDetailsWithResourceIdRoute,
} from "./routes/Resource";
import {
  ScopeDetailsRoute,
  ScopeDetailsWithScopeIdRoute,
} from "./routes/Scope";

const routes: AppRouteObject[] = [
  ClientRegistrationRoute,
  AddRegistrationProviderRoute,
  EditRegistrationProviderRoute,
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
  ClientRoleRoute,
  AuthorizationRoute,
  NewResourceRoute,
  ResourceDetailsRoute,
  ResourceDetailsWithResourceIdRoute,
  NewRoleRoute,
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
