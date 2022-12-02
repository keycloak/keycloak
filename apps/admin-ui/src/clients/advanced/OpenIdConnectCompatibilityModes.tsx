import { ActionGroup, Button, FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

type OpenIdConnectCompatibilityModesProps = {
  save: () => void;
  reset: () => void;
  hasConfigureAccess?: boolean;
};

export const OpenIdConnectCompatibilityModes = ({
  save,
  reset,
  hasConfigureAccess,
}: OpenIdConnectCompatibilityModesProps) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext();
  return (
    <FormAccess
      role="manage-realm"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      <FormGroup
        label={t("excludeSessionStateFromAuthenticationResponse")}
        fieldId="excludeSessionStateFromAuthenticationResponse"
        hasNoPaddingTop
        labelIcon={
          <HelpItem
            helpText="clients-help:excludeSessionStateFromAuthenticationResponse"
            fieldLabelId="clients:excludeSessionStateFromAuthenticationResponse"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.exclude.session.state.from.auth.response"
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Switch
              id="excludeSessionStateFromAuthenticationResponse-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
              aria-label={t("excludeSessionStateFromAuthenticationResponse")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("useRefreshTokens")}
        fieldId="useRefreshTokens"
        hasNoPaddingTop
        labelIcon={
          <HelpItem
            helpText="clients-help:useRefreshTokens"
            fieldLabelId="clients:useRefreshTokens"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.use.refresh.tokens"
          )}
          defaultValue="true"
          control={control}
          render={({ field }) => (
            <Switch
              id="useRefreshTokens"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
              aria-label={t("useRefreshTokens")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("useRefreshTokenForClientCredentialsGrant")}
        fieldId="useRefreshTokenForClientCredentialsGrant"
        hasNoPaddingTop
        labelIcon={
          <HelpItem
            helpText="clients-help:useRefreshTokenForClientCredentialsGrant"
            fieldLabelId="clients:useRefreshTokenForClientCredentialsGrant"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.client_credentials.use_refresh_token"
          )}
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="useRefreshTokenForClientCredentialsGrant"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
              aria-label={t("useRefreshTokenForClientCredentialsGrant")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("useLowerCaseBearerType")}
        fieldId="useLowerCaseBearerType"
        hasNoPaddingTop
        labelIcon={
          <HelpItem
            helpText="clients-help:useLowerCaseBearerType"
            fieldLabelId="clients:useLowerCaseBearerType"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.token.response.type.bearer.lower-case"
          )}
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="useLowerCaseBearerType"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
              aria-label={t("useLowerCaseBearerType")}
            />
          )}
        />
      </FormGroup>
      <ActionGroup>
        <Button
          variant="secondary"
          onClick={save}
          data-testid="OIDCCompatabilitySave"
        >
          {t("common:save")}
        </Button>
        <Button
          variant="link"
          onClick={reset}
          data-testid="OIDCCompatabilityRevert"
        >
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
