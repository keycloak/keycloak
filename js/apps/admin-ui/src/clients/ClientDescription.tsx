import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { FormAccess } from "../components/form-access/FormAccess";
import { KeycloakTextArea } from "../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { FormFields } from "./ClientDetails";

type ClientDescriptionProps = {
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const ClientDescription = ({
  hasConfigureAccess: configure,
}: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<FormFields>();
  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <FormGroup
        labelIcon={
          <HelpItem
            helpText={t("clients-help:clientId")}
            fieldLabelId="clientId"
          />
        }
        label={t("common:clientId")}
        fieldId="kc-client-id"
        helperTextInvalid={t("common:required")}
        validated={
          errors.clientId ? ValidatedOptions.error : ValidatedOptions.default
        }
        isRequired
      >
        <KeycloakTextInput
          {...register("clientId", { required: true })}
          id="kc-client-id"
          data-testid="kc-client-id"
          validated={
            errors.clientId ? ValidatedOptions.error : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup
        labelIcon={
          <HelpItem
            helpText={t("clients-help:clientName")}
            fieldLabelId="name"
          />
        }
        label={t("common:name")}
        fieldId="kc-name"
      >
        <KeycloakTextInput {...register("name")} id="kc-name" />
      </FormGroup>
      <FormGroup
        labelIcon={
          <HelpItem
            helpText={t("clients-help:description")}
            fieldLabelId="description"
          />
        }
        label={t("common:description")}
        fieldId="kc-description"
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={errors.description?.message}
      >
        <KeycloakTextArea
          {...register("description", {
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
          id="kc-description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        />
      </FormGroup>
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
