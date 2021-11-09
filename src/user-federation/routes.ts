import type { RouteDef } from "../route-config";
import { NewKerberosUserFederationRoute } from "./routes/NewKerberosUserFederation";
import { NewLdapUserFederationRoute } from "./routes/NewLdapUserFederation";
import { UserFederationRoute } from "./routes/UserFederation";
import { UserFederationKerberosRoute } from "./routes/UserFederationKerberos";
import { UserFederationLdapRoute } from "./routes/UserFederationLdap";
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
  UserFederationLdapMapperRoute,
];

export default routes;
