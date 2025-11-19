import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type GlobalRequestResult from "@keycloak/keycloak-admin-client/lib/defs/globalRequestResult";
import { AlertVariant, PageSection, Text } from "@patternfly/react-core";
import type { TFunction } from "i18next";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { ScrollForm } from "@keycloak/keycloak-ui-shared";
import type { AddAlertFunction } from "@keycloak/keycloak-ui-shared";
import { convertAttributeNameToForm, toUpperCase } from "../util";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import type { FormFields, SaveOptions } from "./ClientDetails";
import { AdvancedSettings } from "./advanced/AdvancedSettings";
import { AuthenticationOverrides } from "./advanced/AuthenticationOverrides";
import { ClusteringPanel } from "./advanced/ClusteringPanel";
import { FineGrainOpenIdConnect } from "./advanced/FineGrainOpenIdConnect";
import { FineGrainSamlEndpointConfig } from "./advanced/FineGrainSamlEndpointConfig";
import { OpenIdConnectCompatibilityModes } from "./advanced/OpenIdConnectCompatibilityModes";
import { OpenIdVerifiableCredentials } from "./advanced/OpenIdVerifiableCredentials";
import { PROTOCOL_OIDC, PROTOCOL_OID4VC } from "./constants";

export const parseResult = (
  result: GlobalRequestResult,
  prefixKey: string,
  addAlert: AddAlertFunction,
  t: TFunction,
) => {
  const successCount = result.successRequests?.length || 0;
  const failedCount = result.failedRequests?.length || 0;

  if (successCount === 0 && failedCount === 0) {
    addAlert(t("noAdminUrlSet"), AlertVariant.warning);
  } else if (failedCount > 0) {
    addAlert(
      t(prefixKey + "Success", { successNodes: result.successRequests }),
      AlertVariant.success,
    );
    addAlert(
      t(prefixKey + "Fail", { failedNodes: result.failedRequests }),
      AlertVariant.danger,
    );
  } else {
    addAlert(
      t(prefixKey + "Success", { successNodes: result.successRequests }),
      AlertVariant.success,
    );
  }
};

export type AdvancedProps = {
  save: (options?: SaveOptions) => void;
  client: ClientRepresentation;
};

export const AdvancedTab = ({ save, client }: AdvancedProps) => {
  const { t } = useTranslation();
  const isFeatureEnabled = useIsFeatureEnabled();

  const { setValue } = useFormContext();
  const {
    publicClient,
    attributes,
    protocol,
    authenticationFlowBindingOverrides,
  } = client;

  const resetFields = (names: string[]) => {
    for (const name of names) {
      setValue(
        convertAttributeNameToForm<FormFields>(`attributes.${name}`),
        attributes?.[name] || "",
      );
    }
  };

  return (
    <PageSection variant="light" className="pf-v5-u-py-0">
      <ScrollForm
        label={t("jumpToSection")}
        sections={[
          {
            title: t("clustering"),
            isHidden: !publicClient,
            panel: <ClusteringPanel client={client} save={save} />,
          },
          {
            title: t("fineGrainOpenIdConnectConfiguration"),
            isHidden: protocol !== PROTOCOL_OIDC,
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">
                  {t("fineGrainOpenIdConnectConfigurationHelp")}
                </Text>
                <FineGrainOpenIdConnect
                  save={save}
                  reset={() => {
                    resetFields([
                      "logoUri",
                      "policyUri",
                      "tosUri",
                      "access.token.signed.response.alg",
                      "access.token.header.type.rfc9068",
                      "id.token.signed.response.alg",
                      "id.token.encrypted.response.alg",
                      "id.token.encrypted.response.enc",
                      "id.token.as.detached.signature",
                      "user.info.response.signature.alg",
                      "user.info.encrypted.response.alg",
                      "user.info.encrypted.response.enc",
                      "request.object.signature.alg",
                      "request.object.encryption.alg",
                      "request.object.encryption.enc",
                      "request.object.required",
                      "request.uris",
                      "authorization.signed.response.alg",
                      "authorization.encrypted.response.alg",
                      "authorization.encrypted.response.enc",
                    ]);
                  }}
                />
              </>
            ),
          },
          {
            title: t("openIdConnectCompatibilityModes"),
            isHidden: protocol !== PROTOCOL_OIDC,
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">
                  {t("openIdConnectCompatibilityModesHelp")}
                </Text>
                <OpenIdConnectCompatibilityModes
                  save={() => save()}
                  reset={() =>
                    resetFields([
                      "exclude.session.state.from.auth.response",
                      "use.refresh.tokens",
                      "client_credentials.use_refresh_token",
                      "token.response.type.bearer.lower-case",
                    ])
                  }
                />
              </>
            ),
          },
          {
            title: t("fineGrainSamlEndpointConfig"),
            isHidden: protocol === PROTOCOL_OIDC,
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">
                  {t("fineGrainSamlEndpointConfigHelp")}
                </Text>
                <FineGrainSamlEndpointConfig
                  save={() => save()}
                  reset={() =>
                    resetFields([
                      "logoUri",
                      "policyUri",
                      "tosUri",
                      "saml_assertion_consumer_url_post",
                      "saml_assertion_consumer_url_redirect",
                      "saml_single_logout_service_url_post",
                      "saml_single_logout_service_url_redirect",
                      "saml_single_logout_service_url_artifact",
                      "saml_artifact_binding_url",
                      "saml_artifact_resolution_service_url",
                    ])
                  }
                />
              </>
            ),
          },
          {
            title: t("advancedSettings"),
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">
                  {t("advancedSettings" + toUpperCase(protocol || ""))}
                </Text>
                <AdvancedSettings
                  protocol={protocol}
                  save={() => save()}
                  reset={() => {
                    resetFields([
                      "saml.assertion.lifespan",
                      "access.token.lifespan",
                      "session.idle.timeout",
                      "client.session.max.lifespan",
                      "client.offline.session.idle.timeout",
                      "client.offline.session.max.lifespan",
                      "dpop.bound.access.tokens",
                      "tls.client.certificate.bound.access.tokens",
                      "require.pushed.authorization.requests",
                      "client.use.lightweight.access.token.enabled",
                      "client.introspection.response.allow.jwt.claim.enabled",
                      "pkce.code.challenge.method",
                      "acr.loa.map",
                      "default.acr.values",
                      "minimum.acr.value",
                    ]);
                  }}
                />
              </>
            ),
          },
          {
            title: t("openIdVerifiableCredentials"),
            isHidden:
              (protocol !== PROTOCOL_OIDC && protocol !== PROTOCOL_OID4VC) ||
              !isFeatureEnabled(Feature.OpenId4VCI),
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">
                  {t("openIdVerifiableCredentialsHelp")}
                </Text>
                <OpenIdVerifiableCredentials
                  client={client}
                  save={save}
                  reset={() => resetFields(["oid4vci.enabled"])}
                />
              </>
            ),
          },
          {
            title: t("authenticationOverrides"),
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">
                  {t("authenticationOverridesHelp")}
                </Text>
                <AuthenticationOverrides
                  protocol={protocol}
                  save={() => save()}
                  reset={() => {
                    setValue(
                      "authenticationFlowBindingOverrides.browser",
                      authenticationFlowBindingOverrides?.browser,
                    );
                    setValue(
                      "authenticationFlowBindingOverrides.direct_grant",
                      authenticationFlowBindingOverrides?.direct_grant,
                    );
                  }}
                />
              </>
            ),
          },
        ]}
        borders
      />
    </PageSection>
  );
};
