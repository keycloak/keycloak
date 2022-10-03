import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormAccess } from "../components/form-access/FormAccess";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../components/keycloak-text-area/KeycloakTextArea";

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
  } = useFormContext<ClientRepresentation>();
  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <FormGroup
        labelIcon={
          <HelpItem helpText="clients-help:clientId" fieldLabelId="clientId" />
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
          ref={register({ required: true })}
          type="text"
          id="kc-client-id"
          data-testid="kc-client-id"
          name="clientId"
          validated={
            errors.clientId ? ValidatedOptions.error : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup
        labelIcon={
          <HelpItem helpText="clients-help:clientName" fieldLabelId="name" />
        }
        label={t("common:name")}
        fieldId="kc-name"
      >
        <KeycloakTextInput
          ref={register()}
          type="text"
          id="kc-name"
          name="name"
        />
      </FormGroup>
      <FormGroup
        labelIcon={
          <HelpItem
            helpText="clients-help:description"
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
          ref={register({
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
          type="text"
          id="kc-description"
          name="description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup
        label={t("alwaysDisplayInConsole")}
        labelIcon={
          <HelpItem
            helpText="clients-help:alwaysDisplayInConsole"
            fieldLabelId="clients:alwaysDisplayInConsole"
          />
        }
        fieldId="kc-always-display-in-console"
        hasNoPaddingTop
      >
        <Controller
          name="alwaysDisplayInConsole"
          defaultValue={false}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="kc-always-display-in-console-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value}
              onChange={onChange}
              aria-label={t("alwaysDisplayInConsole")}
            />
          )}
        />
      </FormGroup>
    </FormAccess>
  );
};
