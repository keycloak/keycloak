import type { AppRouteObject } from "../routes";
import { IdentityProviderRoute } from "./routes/IdentityProvider";
import { IdentityProviderKeycloakOidcRoute } from "./routes/IdentityProviderKeycloakOidc";
import { IdentityProviderOidcRoute } from "./routes/IdentityProviderOidc";
import { IdentityProviderSamlRoute } from "./routes/IdentityProviderSaml";
import { IdentityProviderSpiffeRoute } from "./routes/IdentityProviderSpiffe";
import { IdentityProviderKubernetesRoute } from "./routes/IdentityProviderKubernetes";
import { IdentityProvidersRoute } from "./routes/IdentityProviders";
import { IdentityProviderAddMapperRoute } from "./routes/AddMapper";
import { IdentityProviderEditMapperRoute } from "./routes/EditMapper";
import { IdentityProviderCreateRoute } from "./routes/IdentityProviderCreate";
import { IdentityProviderOAuth2Route } from "./routes/IdentityProviderOAuth2";
import { IdentityProviderJWTAuthorizationGrantRoute } from "./routes/IdentityProviderJWTAuthorizationGrant";

const routes: AppRouteObject[] = [
  IdentityProviderAddMapperRoute,
  IdentityProviderEditMapperRoute,
  IdentityProvidersRoute,
  IdentityProviderOidcRoute,
  IdentityProviderSamlRoute,
  IdentityProviderSpiffeRoute,
  IdentityProviderJWTAuthorizationGrantRoute,
  IdentityProviderKubernetesRoute,
  IdentityProviderKeycloakOidcRoute,
  IdentityProviderCreateRoute,
  IdentityProviderRoute,
  IdentityProviderOAuth2Route,
];

export default routes;
