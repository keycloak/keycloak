import type { RouteDef } from "../route-config";
import { NewKerberosUserFederationRoute } from "./routes/NewKerberosUserFederation";
import { NewLdapUserFederationRoute } from "./routes/NewLdapUserFederation";
import {
  CustomEditProviderRoute,
  CustomProviderRoute,
} from "./routes/NewProvider";
import { UserFederationRoute } from "./routes/UserFederation";
import { UserFederationKerberosRoute } from "./routes/UserFederationKerberos";
import {
  UserFederationLdapRoute,
  UserFederationLdapWithTabRoute,
} from "./routes/UserFederationLdap";
import { UserFederationLdapMapperRoute } from "./routes/UserFederationLdapMapper";
import { UserFederationsKerberosRoute } from "./routes/UserFederationsKerberos";
import { UserFederationsLdapRoute } from "./routes/UserFederationsLdap";

const routes: RouteDef[] = [
  UserFederationRoute,
  UserFederationsKerberosRoute,
  NewKerberosUserFederationRoute,
  UserFederationKerberosRoute,
  UserFederationsLdapRoute,
  NewLdapUserFederationRoute,
  UserFederationLdapRoute,
  UserFederationLdapWithTabRoute,
  UserFederationLdapMapperRoute,
  CustomProviderRoute,
  CustomEditProviderRoute,
];

export default routes;
