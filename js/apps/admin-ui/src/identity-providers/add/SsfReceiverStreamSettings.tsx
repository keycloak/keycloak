import {
  PasswordControl,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

export const SsfReceiverStreamSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  const deliveryMethod = useWatch({
    control,
    name: "config.deliveryMethod",
  });

  return (
    <>
      <TextControl
        name="config.streamId"
        label={t("ssfStreamId")}
        labelIcon={t("ssfStreamIdHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.streamAudience"
        label={t("ssfStreamAudience")}
        labelIcon={t("ssfStreamAudienceHelp")}
      />

      <SelectControl
        name="config.deliveryMethod"
        label={t("ssfDeliveryMethod")}
        labelIcon={t("ssfDeliveryMethodHelp")}
        options={[
          {
            key: "PUSH",
            value: t("ssfDeliveryMethod.push"),
          },
        ]}
        controller={{ defaultValue: "PUSH" }}
      />

      {(!deliveryMethod || deliveryMethod === "PUSH") && (
        <PasswordControl
          name="config.pushAuthorizationHeader"
          label={t("ssfPushAuthorizationHeader")}
          labelIcon={t("ssfPushAuthorizationHeaderHelp")}
        />
      )}
    </>
  );
};
