import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { AlertVariant, PageSection, Text } from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type GlobalRequestResult from "@keycloak/keycloak-admin-client/lib/defs/globalRequestResult";

import type { AddAlertFunction } from "../components/alert/Alerts";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { convertToFormValues, toUpperCase } from "../util";
import { AdvancedSettings } from "./advanced/AdvancedSettings";
import { AuthenticationOverrides } from "./advanced/AuthenticationOverrides";
import { FineGrainOpenIdConnect } from "./advanced/FineGrainOpenIdConnect";
import { FineGrainSamlEndpointConfig } from "./advanced/FineGrainSamlEndpointConfig";
import { OpenIdConnectCompatibilityModes } from "./advanced/OpenIdConnectCompatibilityModes";
import type { SaveOptions } from "./ClientDetails";
import type { TFunction } from "i18next";
import { RevocationPanel } from "./advanced/RevocationPanel";
import { ClusteringPanel } from "./advanced/ClusteringPanel";

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

  const { setValue, control, reset } = useFormContext();
  const {
    publicClient,
    attributes,
    protocol,
    authenticationFlowBindingOverrides,
  } = client;

  const resetFields = (names: string[]) => {
    const values: { [name: string]: string } = {};
    for (const name of names) {
      values[`attributes.${name}`] = attributes?.[name];
    }
    reset(values);
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
                  reset={() =>
                    convertToFormValues(attributes, (key, value) =>
                      setValue(`attributes.${key}`, value)
                    )
                  }
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
                  control={control}
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
                  control={control}
                  save={() => save()}
                  reset={() =>
                    convertToFormValues(attributes, (key, value) =>
                      setValue(`attributes.${key}`, value)
                    )
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
                  control={control}
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
                  control={control}
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
