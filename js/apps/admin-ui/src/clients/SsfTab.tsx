import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  NumberControl,
  ScrollForm,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button, PageSection, Text } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { convertAttributeNameToForm } from "../util";
import type { FormFields, SaveOptions } from "./ClientDetails";

export type SsfTabProps = {
  save: (options?: SaveOptions) => void;
  client: ClientRepresentation;
};

export const SsfTab = ({ save, client }: SsfTabProps) => {
  const { t } = useTranslation();

  const { watch, setValue } = useFormContext<FormFields>();

  const ssfVerificationTrigger = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.ssf.verificationTrigger",
    ),
  );
  const ssfDelivery = watch(
    convertAttributeNameToForm<FormFields>("attributes.ssf.delivery"),
  );

  const resetFields = (names: string[]) => {
    for (const name of names) {
      setValue(
        convertAttributeNameToForm<FormFields>(`attributes.${name}`),
        client.attributes?.[name] || "",
      );
    }
  };

  const reset = () =>
    resetFields([
      "ssf.streamAudience",
      "ssf.profile",
      "ssf.verificationTrigger",
      "ssf.verificationDelayMillis",
      "ssf.status",
      "ssf.delivery",
      "ssf.pushEndpointConnectTimeoutMillis",
      "ssf.pushEndpointSocketTimeoutMillis",
    ]);

  return (
    <PageSection variant="light" className="pf-v5-u-py-0">
      <ScrollForm
        label={t("jumpToSection")}
        sections={[
          {
            title: t("ssfReceiver"),
            panel: (
              <>
                <Text className="pf-v5-u-pb-lg">{t("ssfReceiverHelp")}</Text>
                <FormAccess
                  role="manage-clients"
                  fineGrainedAccess={client.access?.configure}
                  isHorizontal
                >
                  <TextControl
                    name={convertAttributeNameToForm<FormFields>(
                      "attributes.ssf.streamAudience",
                    )}
                    label={t("ssfStreamAudience")}
                    labelIcon={t("ssfStreamAudienceHelp")}
                  />
                  <SelectControl
                    name={convertAttributeNameToForm<FormFields>(
                      "attributes.ssf.profile",
                    )}
                    label={t("ssfProfile")}
                    labelIcon={t("ssfProfileHelp")}
                    controller={{
                      defaultValue: "SSF_1_0",
                    }}
                    options={[
                      { key: "SSF_1_0", value: t("ssfProfile.SSF_1_0") },
                      { key: "SSE_CAEP", value: t("ssfProfile.SSE_CAEP") },
                    ]}
                  />
                  <SelectControl
                    name={convertAttributeNameToForm<FormFields>(
                      "attributes.ssf.verificationTrigger",
                    )}
                    label={t("ssfVerification")}
                    labelIcon={t("ssfVerificationHelp")}
                    controller={{
                      defaultValue: "RECEIVER_INITIATED",
                    }}
                    options={[
                      {
                        key: "RECEIVER_INITIATED",
                        value: t("ssfVerification.RECEIVER_INITIATED"),
                      },
                      {
                        key: "TRANSMITTER_INITIATED",
                        value: t("ssfVerification.TRANSMITTER_INITIATED"),
                      },
                    ]}
                  />
                  {ssfVerificationTrigger === "TRANSMITTER_INITIATED" && (
                    <NumberControl
                      name={convertAttributeNameToForm<FormFields>(
                        "attributes.ssf.verificationDelayMillis",
                      )}
                      label={t("ssfVerificationDelay")}
                      labelIcon={t("ssfVerificationDelayHelp")}
                      controller={{
                        defaultValue: 1500,
                        rules: {
                          min: 0,
                        },
                      }}
                    />
                  )}
                  <SelectControl
                    name={convertAttributeNameToForm<FormFields>(
                      "attributes.ssf.status",
                    )}
                    label={t("ssfStreamStatus")}
                    labelIcon={t("ssfStreamStatusHelp")}
                    controller={{
                      defaultValue: "enabled",
                    }}
                    options={[
                      { key: "enabled", value: t("ssfStreamStatus.enabled") },
                      { key: "paused", value: t("ssfStreamStatus.paused") },
                      { key: "disabled", value: t("ssfStreamStatus.disabled") },
                    ]}
                  />
                  <SelectControl
                    name={convertAttributeNameToForm<FormFields>(
                      "attributes.ssf.delivery",
                    )}
                    label={t("ssfDelivery")}
                    labelIcon={t("ssfDeliveryHelp")}
                    controller={{
                      defaultValue: "PUSH",
                    }}
                    options={[
                      { key: "PUSH", value: t("ssfDelivery.PUSH") },
                      // PULL delivery is not yet supported
                      // { key: "PULL", value: t("ssfDelivery.PULL") },
                    ]}
                  />
                  {(ssfDelivery === "PUSH" || !ssfDelivery) && (
                    <>
                      <NumberControl
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.pushEndpointConnectTimeoutMillis",
                        )}
                        label={t("ssfPushEndpointConnectTimeout")}
                        labelIcon={t("ssfPushEndpointConnectTimeoutHelp")}
                        controller={{
                          defaultValue: 1000,
                          rules: {
                            min: 0,
                          },
                        }}
                      />
                      <NumberControl
                        name={convertAttributeNameToForm<FormFields>(
                          "attributes.ssf.pushEndpointSocketTimeoutMillis",
                        )}
                        label={t("ssfPushEndpointSocketTimeout")}
                        labelIcon={t("ssfPushEndpointSocketTimeoutHelp")}
                        controller={{
                          defaultValue: 750,
                          rules: {
                            min: 0,
                          },
                        }}
                      />
                    </>
                  )}
                  <ActionGroup>
                    <Button
                      variant="secondary"
                      onClick={() => save()}
                      data-testid="ssfSave"
                    >
                      {t("save")}
                    </Button>
                    <Button
                      variant="link"
                      onClick={reset}
                      data-testid="ssfRevert"
                    >
                      {t("revert")}
                    </Button>
                  </ActionGroup>
                </FormAccess>
              </>
            ),
          },
        ]}
        borders
      />
    </PageSection>
  );
};
