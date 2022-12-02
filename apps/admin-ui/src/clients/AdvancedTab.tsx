import { AlertVariant, PageSection, Text } from "@patternfly/react-core";
import type { TFunction } from "i18next";
import { useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type GlobalRequestResult from "@keycloak/keycloak-admin-client/lib/defs/globalRequestResult";

import type { AddAlertFunction } from "../components/alert/Alerts";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { convertAttributeNameToForm, toUpperCase } from "../util";
import { AdvancedSettings } from "./advanced/AdvancedSettings";
import { AuthenticationOverrides } from "./advanced/AuthenticationOverrides";
import { ClusteringPanel } from "./advanced/ClusteringPanel";
import { FineGrainOpenIdConnect } from "./advanced/FineGrainOpenIdConnect";
import { FineGrainSamlEndpointConfig } from "./advanced/FineGrainSamlEndpointConfig";
import { OpenIdConnectCompatibilityModes } from "./advanced/OpenIdConnectCompatibilityModes";
import { RevocationPanel } from "./advanced/RevocationPanel";
import type { FormFields, SaveOptions } from "./ClientDetails";

export const parseResult = (
  result: GlobalRequestResult,
  prefixKey: string,
  addAlert: AddAlertFunction,
  t: TFunction
) => {
  const successCount = result.successRequests?.length || 0;
  const failedCount = result.failedRequests?.length || 0;

  if (successCount === 0 && failedCount === 0) {
    addAlert(t("noAdminUrlSet"), AlertVariant.warning);
  } else if (failedCount > 0) {
    addAlert(
      t(prefixKey + "Success", { successNodes: result.successRequests }),
      AlertVariant.success
    );
    addAlert(
      t(prefixKey + "Fail", { failedNodes: result.failedRequests }),
      AlertVariant.danger
    );
  } else {
    addAlert(
      t(prefixKey + "Success", { successNodes: result.successRequests }),
      AlertVariant.success
    );
  }
};

export type AdvancedProps = {
  save: (options?: SaveOptions) => void;
  client: ClientRepresentation;
};

export const AdvancedTab = ({ save, client }: AdvancedProps) => {
  const { t } = useTranslation("clients");
  const openIdConnect = "openid-connect";

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
        attributes?.[name] || ""
      );
    }
  };

  return (
    <PageSection variant="light" className="pf-u-py-0">
      <ScrollForm
        sections={[
          {
            title: t("revocation"),
            isHidden: protocol !== openIdConnect,
            panel: <RevocationPanel client={client} save={save} />,
          },
          {
            title: t("clustering"),
            isHidden: !publicClient,
            panel: <ClusteringPanel client={client} save={save} />,
          },
          {
            title: t("fineGrainOpenIdConnectConfiguration"),
            isHidden: protocol !== openIdConnect,
            panel: (
              <>
                <Text className="pf-u-pb-lg">
                  {t("clients-help:fineGrainOpenIdConnectConfiguration")}
                </Text>
                <FineGrainOpenIdConnect
                  save={save}
                  reset={() => {
                    resetFields([
                      "logoUri",
                      "policyUri",
                      "tosUri",
                      "access.token.signed.response.alg",
                      "id.token.signed.response.alg",
                      "id.token.encrypted.response.alg",
                      "id.token.encrypted.response.enc",
                      "user.info.response.signature.alg",
                      "request.object.signature.alg",
                      "request.object.encryption.alg",
                      "request.object.encryption.enc",
                      "request.object.required",
                      "request.uris",
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
            isHidden: protocol !== openIdConnect,
            panel: (
              <>
                <Text className="pf-u-pb-lg">
                  {t("clients-help:openIdConnectCompatibilityModes")}
                </Text>
                <OpenIdConnectCompatibilityModes
                  save={() => save()}
                  reset={() =>
                    resetFields(["exclude.session.state.from.auth.response"])
                  }
                />
              </>
            ),
          },
          {
            title: t("fineGrainSamlEndpointConfig"),
            isHidden: protocol === openIdConnect,
            panel: (
              <>
                <Text className="pf-u-pb-lg">
                  {t("clients-help:fineGrainSamlEndpointConfig")}
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
                <Text className="pf-u-pb-lg">
                  {t(
                    "clients-help:advancedSettings" +
                      toUpperCase(protocol || "")
                  )}
                </Text>
                <AdvancedSettings
                  protocol={protocol}
                  save={() => save()}
                  reset={() => {
                    resetFields([
                      "saml.assertion.lifespan",
                      "access.token.lifespan",
                      "tls.client.certificate.bound.access.tokens",
                      "pkce.code.challenge.method",
                    ]);
                  }}
                />
              </>
            ),
          },
          {
            title: t("authenticationOverrides"),
            panel: (
              <>
                <Text className="pf-u-pb-lg">
                  {t("clients-help:authenticationOverrides")}
                </Text>
                <AuthenticationOverrides
                  protocol={protocol}
                  save={() => save()}
                  reset={() => {
                    setValue(
                      "authenticationFlowBindingOverrides.browser",
                      authenticationFlowBindingOverrides?.browser
                    );
                    setValue(
                      "authenticationFlowBindingOverrides.direct_grant",
                      authenticationFlowBindingOverrides?.direct_grant
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
