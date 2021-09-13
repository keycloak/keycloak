import type { RouteDef } from "../route-config";
import { IdentityProviderRoute } from "./routes/IdentityProvider";
import { IdentityProviderKeycloakOidcRoute } from "./routes/IdentityProviderKeycloakOidc";
import { IdentityProviderOidcRoute } from "./routes/IdentityProviderOidc";
import { IdentityProviderSamlRoute } from "./routes/IdentityProviderSaml";
import { IdentityProvidersRoute } from "./routes/IdentityProviders";
import { IdentityProviderTabRoute } from "./routes/IdentityProviderTab";
import { IdentityProviderAddMapperRoute } from "./routes/AddMapper";

const routes: RouteDef[] = [
  IdentityProvidersRoute,
  IdentityProviderOidcRoute,
  IdentityProviderSamlRoute,
  IdentityProviderKeycloakOidcRoute,
  IdentityProviderRoute,
  IdentityProviderTabRoute,
  IdentityProviderAddMapperRoute,
];

export default routes;
