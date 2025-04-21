import { ActionGroup, Button, FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem, SelectControl } from "@keycloak/keycloak-ui-shared";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";

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
  const { t } = useTranslation();
  const { control, watch } = useFormContext();
  const isFeatureEnabled = useIsFeatureEnabled();
  const tokenExchangeEnabled = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.standard.token.exchange.enabled",
    ),
  );
  const useRefreshTokens = watch(
    convertAttributeNameToForm<FormFields>("attributes.use.refresh.tokens"),
    "true",
  );
  return (
    <FormAccess
      role="manage-clients"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      <FormGroup
        label={t("excludeSessionStateFromAuthenticationResponse")}
        fieldId="excludeSessionStateFromAuthenticationResponse"
        hasNoPaddingTop
        labelIcon={
          <HelpItem
            helpText={t("excludeSessionStateFromAuthenticationResponseHelp")}
            fieldLabelId="excludeSessionStateFromAuthenticationResponse"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.exclude.session.state.from.auth.response",
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Switch
              id="excludeSessionStateFromAuthenticationResponse-switch"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(_event, value) => field.onChange(value.toString())}
              aria-label={t("excludeSessionStateFromAuthenticationResponse")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("excludeIssuerFromAuthenticationResponse")}
        fieldId="excludeIssuerFromAuthenticationResponse"
        hasNoPaddingTop
        labelIcon={
          <HelpItem
            helpText={t("excludeIssuerFromAuthenticationResponseHelp")}
            fieldLabelId="excludeIssuerFromAuthenticationResponse"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.exclude.issuer.from.auth.response",
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Switch
              id="excludeIssuerFromAuthenticationResponse-switch"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(_event, value) => field.onChange(value.toString())}
              aria-label={t("excludeIssuerFromAuthenticationResponse")}
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
            helpText={t("useRefreshTokensHelp")}
            fieldLabelId="useRefreshTokens"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.use.refresh.tokens",
          )}
          defaultValue="true"
          control={control}
          render={({ field }) => (
            <Switch
              id="useRefreshTokens"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(_event, value) => field.onChange(value.toString())}
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
            helpText={t("useRefreshTokenForClientCredentialsGrantHelp")}
            fieldLabelId="useRefreshTokenForClientCredentialsGrant"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.client_credentials.use_refresh_token",
          )}
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="useRefreshTokenForClientCredentialsGrant"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(_event, value) => field.onChange(value.toString())}
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
            helpText={t("useLowerCaseBearerTypeHelp")}
            fieldLabelId="useLowerCaseBearerType"
          />
        }
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.token.response.type.bearer.lower-case",
          )}
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="useLowerCaseBearerType"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(_event, value) => field.onChange(value.toString())}
              aria-label={t("useLowerCaseBearerType")}
            />
          )}
        />
      </FormGroup>

      {isFeatureEnabled(Feature.StandardTokenExchangeV2) && (
        <SelectControl
          name={convertAttributeNameToForm<FormFields>(
            "attributes.standard.token.exchange.enableRefreshRequestedTokenType",
          )}
          label={t("enableRefreshRequestedTokenType")}
          labelIcon={t("enableRefreshRequestedTokenTypeHelp")}
          controller={{
            defaultValue: "",
          }}
          isDisabled={
            tokenExchangeEnabled?.toString() !== "true" ||
            useRefreshTokens?.toString() !== "true"
          }
          options={[
            { key: "", value: t("choose") },
            { key: "NO", value: t("no") },
            { key: "SAME_SESSION", value: t("sameSession") },
          ]}
        />
      )}
      <ActionGroup>
        <Button
          variant="secondary"
          onClick={save}
          data-testid="OIDCCompatabilitySave"
        >
          {t("save")}
        </Button>
        <Button
          variant="link"
          onClick={reset}
          data-testid="OIDCCompatabilityRevert"
        >
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
