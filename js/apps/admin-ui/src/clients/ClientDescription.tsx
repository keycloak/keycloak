import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl, TextAreaControl } from "ui-shared";

import { FormAccess } from "../components/form-access/FormAccess";
import { FormFields } from "./ClientDetails";

type ClientDescriptionProps = {
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const ClientDescription = ({
  hasConfigureAccess: configure,
}: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext<FormFields>();
  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <TextControl
        name="clientId"
        label={t("common:clientId")}
        labelIcon={t("clients-help:clientId")}
        rules={{ required: { value: true, message: t("common:required") } }}
      />
      <TextControl
        name="name"
        label={t("common:name")}
        labelIcon={t("clients-help:clientName")}
      />
      <TextAreaControl
        name="description"
        label={t("common:description")}
        labelIcon={t("clients-help:description")}
        rules={{
          maxLength: {
            value: 255,
            message: t("common:maxLength", { length: 255 }),
          },
        }}
      />
      <FormGroup
        label={t("alwaysDisplayInUI")}
        labelIcon={
          <HelpItem
            helpText={t("clients-help:alwaysDisplayInUI")}
            fieldLabelId="clients:alwaysDisplayInUI"
          />
        }
        fieldId="kc-always-display-in-ui"
        hasNoPaddingTop
      >
        <Controller
          name="alwaysDisplayInConsole"
          defaultValue={false}
          control={control}
          render={({ field }) => (
            <Switch
              id="kc-always-display-in-ui-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value}
              onChange={field.onChange}
              aria-label={t("alwaysDisplayInUI")}
            />
          )}
        />
      </FormGroup>
    </FormAccess>
  );
};
