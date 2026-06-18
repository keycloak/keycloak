import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useAccess } from "../context/access/Access";

export enum Feature {
  AccountV3 = "ACCOUNT_V3",
  AdminFineGrainedAuthz = "ADMIN_FINE_GRAINED_AUTHZ",
  AdminFineGrainedAuthzV2 = "ADMIN_FINE_GRAINED_AUTHZ_V2",
  ClientPolicies = "CLIENT_POLICIES",
  Kerberos = "KERBEROS",
  ParameterizedScopes = "PARAMETERIZED_SCOPES",
  DPoP = "DPOP",
  DeviceFlow = "DEVICE_FLOW",
  TransientUsers = "TRANSIENT_USERS",
  ClientTypes = "CLIENT_TYPES",
  DeclarativeUI = "DECLARATIVE_UI",
  Organizations = "ORGANIZATION",
  OpenId4VCI = "OID4VC_VCI",
  QuickTheme = "QUICK_THEME",
  StandardTokenExchangeV2 = "TOKEN_EXCHANGE_STANDARD_V2",
  JWTAuthorizationGrant = "JWT_AUTHORIZATION_GRANT",
  Passkeys = "PASSKEYS",
  ClientAuthFederated = "CLIENT_AUTH_FEDERATED",
  Workflows = "WORKFLOWS",
  StepUpAuthenticationSaml = "STEP_UP_AUTHENTICATION_SAML",
  Ssf = "SSF",
  ScimApi = "SCIM_API",
  IdentityBrokeringAPIV1 = "IDENTITY_BROKERING_API_V1",
  IdentityBrokeringAPIV2 = "IDENTITY_BROKERING_API_V2",
}

export const unversionedName = (name: string) => name.replace(/_V\d+$/, "");

export default function useIsFeatureEnabled() {
  const { features } = useServerInfo();
  const { hasAccess } = useAccess();

  const hasFeatureAccess = (feature: Feature) => {
    switch (feature) {
      case Feature.Organizations:
        return hasAccess(({ hasAny }) =>
          hasAny("manage-realm", "query-organizations"),
        );
      default:
        return true;
    }
  };

  return function isFeatureEnabled(feature: Feature) {
    if (!features) {
      return false;
    }
    return features
      .filter((f) => f.enabled && hasFeatureAccess(f.name as Feature))
      .map((f) => f.name)
      .includes(feature);
  };
}

export function useIsFeatureDisabled() {
  const { features } = useServerInfo();

  return function isFeatureDisabled(feature: Feature) {
    if (!features) {
      return true;
    }
    return !features.some(
      (f) => f.enabled && unversionedName(f.name!) === unversionedName(feature),
    );
  };
}
