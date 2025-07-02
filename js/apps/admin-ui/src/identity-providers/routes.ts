import type { AppRouteObject } from "../routes";
import { IdentityProviderAddMapperRoute } from "./routes/AddMapper";
import { IdentityProviderEditMapperRoute } from "./routes/EditMapper";
import { IdentityProviderRoute } from "./routes/IdentityProvider";
import { IdentityProviderCreateRoute } from "./routes/IdentityProviderCreate";
import { IdentityProviderKeycloakOidcRoute } from "./routes/IdentityProviderKeycloakOidc";
import { IdentityProviderOAuth2Route } from "./routes/IdentityProviderOAuth2";
import { IdentityProviderOidcRoute } from "./routes/IdentityProviderOidc";
import { IdentityProviderSamlRoute } from "./routes/IdentityProviderSaml";
import { IdentityProvidersRoute } from "./routes/IdentityProviders";
import { IdentityProviderCustomOidcRoute } from "./routes/IdentityProviderCustomOidc";

const routes: AppRouteObject[] = [
  IdentityProviderAddMapperRoute,
  IdentityProviderEditMapperRoute,
  IdentityProvidersRoute,
  IdentityProviderOidcRoute,
  IdentityProviderSamlRoute,
  IdentityProviderKeycloakOidcRoute,
  IdentityProviderCreateRoute,
  IdentityProviderRoute,
  IdentityProviderOAuth2Route,
  IdentityProviderCustomOidcRoute,
];

export default routes;
