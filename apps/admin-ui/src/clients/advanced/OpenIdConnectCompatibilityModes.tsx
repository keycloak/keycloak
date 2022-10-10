import { useTranslation } from "react-i18next";
import { Control, Controller } from "react-hook-form";
import { ActionGroup, Button, FormGroup, Switch } from "@patternfly/react-core";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { convertAttributeNameToForm } from "../../util";

type OpenIdConnectCompatibilityModesProps = {
  control: Control<Record<string, any>>;
  save: () => void;
  reset: () => void;
  hasConfigureAccess?: boolean;
};

export const OpenIdConnectCompatibilityModes = ({
  control,
  save,
  reset,
  hasConfigureAccess,
}: OpenIdConnectCompatibilityModesProps) => {
  const { t } = useTranslation("clients");
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
          name={convertAttributeNameToForm(
            "attributes.exclude.session.state.from.auth.response"
          )}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="excludeSessionStateFromAuthenticationResponse-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange(value.toString())}
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
          name={convertAttributeNameToForm("attributes.use.refresh.tokens")}
          defaultValue="true"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="useRefreshTokens"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange(value.toString())}
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
          name={convertAttributeNameToForm(
            "attributes.client_credentials.use_refresh_token"
          )}
          defaultValue="false"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="useRefreshTokenForClientCredentialsGrant"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange(value.toString())}
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
          name={convertAttributeNameToForm(
            "attributes.token.response.type.bearer.lower-case"
          )}
          defaultValue="false"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="useLowerCaseBearerType"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange(value.toString())}
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
