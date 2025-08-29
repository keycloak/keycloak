import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  FormErrorText,
  HelpItem,
  KeycloakSelect,
  SelectControl,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  FormGroup,
  SelectOption,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import type { FieldProps } from "../component/FormGroupField";
import { FormGroupField } from "../component/FormGroupField";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

const LoginFlow = ({
  field,
  label,
  defaultValue,
  labelForEmpty = "none",
}: FieldProps & { defaultValue: string; labelForEmpty?: string }) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { control } = useFormContext();

  const [flows, setFlows] = useState<AuthenticationFlowRepresentation[]>();
  const [open, setOpen] = useState(false);

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) =>
      setFlows(flows.filter((flow) => flow.providerId === "basic-flow")),
    [],
  );

  return (
    <FormGroup
      label={t(label)}
      labelIcon={<HelpItem helpText={t(`${label}Help`)} fieldLabelId={label} />}
      fieldId={label}
    >
      <Controller
        name={field}
        defaultValue={defaultValue}
        control={control}
        render={({ field }) => (
          <KeycloakSelect
            toggleId={label}
            onToggle={() => setOpen(!open)}
            onSelect={(value) => {
              field.onChange(value as string);
              setOpen(false);
            }}
            selections={field.value || t(labelForEmpty)}
            variant={SelectVariant.single}
            aria-label={t(label)}
            isOpen={open}
          >
            {[
              ...(defaultValue === ""
                ? [
                    <SelectOption key="empty" value="">
                      {t(labelForEmpty)}
                    </SelectOption>,
                  ]
                : []),
              ...(flows?.map((option) => (
                <SelectOption
                  selected={option.alias === field.value}
                  key={option.id}
                  value={option.alias}
                >
                  {option.alias}
                </SelectOption>
              )) || []),
            ]}
          </KeycloakSelect>
        )}
      />
    </FormGroup>
  );
};

const SYNC_MODES = ["IMPORT", "LEGACY", "FORCE"];
const SHOW_IN_ACCOUNT_CONSOLE_VALUES = ["ALWAYS", "WHEN_LINKED", "NEVER"];
type AdvancedSettingsProps = {
  isOIDC: boolean;
  isSAML: boolean;
  isOAuth2: boolean;
};

export const AdvancedSettings = ({
  isOIDC,
  isSAML,
  isOAuth2,
}: AdvancedSettingsProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    setValue,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();
  const filteredByClaim = useWatch({
    control,
    name: "config.filteredByClaim",
    defaultValue: "false",
  });
  const claimFilterRequired = filteredByClaim === "true";
  const isFeatureEnabled = useIsFeatureEnabled();
  const isTransientUsersEnabled = isFeatureEnabled(Feature.TransientUsers);
  const isClientAuthFederatedEnabled = isFeatureEnabled(
    Feature.ClientAuthFederated,
  );
  const transientUsers = useWatch({
    control,
    name: "config.doNotStoreUsers",
    defaultValue: "false",
  });
  const syncModeAvailable = transientUsers === "false";
  return (
    <>
      {!isOIDC && !isSAML && !isOAuth2 && (
        <TextField field="config.defaultScope" label="scopes" />
      )}
      <SwitchField field="storeToken" label="storeTokens" fieldType="boolean" />
      {(isSAML || isOIDC || isOAuth2) && (
        <SwitchField
          field="addReadTokenRoleOnCreate"
          label="storedTokensReadable"
          fieldType="boolean"
        />
      )}
      {!isOIDC && !isSAML && !isOAuth2 && (
        <>
          <SwitchField
            field="config.acceptsPromptNoneForwardFromClient"
            label="acceptsPromptNone"
          />
          <SwitchField field="config.disableUserInfo" label="disableUserInfo" />
        </>
      )}
      {isOIDC && (
        <SwitchField field="config.isAccessTokenJWT" label="isAccessTokenJWT" />
      )}
      <SwitchField field="trustEmail" label="trustEmail" fieldType="boolean" />
      <SwitchField
        field="linkOnly"
        label="accountLinkingOnly"
        fieldType="boolean"
      />
      <SwitchField
        field="hideOnLogin"
        label="hideOnLoginPage"
        fieldType="boolean"
      />
      <SelectControl
        name="config.showInAccountConsole"
        label={t("showInAccountConsole")}
        labelIcon={t("showInAccountConsoleHelp")}
        options={SHOW_IN_ACCOUNT_CONSOLE_VALUES.map((showInAccountConsole) => ({
          key: showInAccountConsole,
          value: t(
            `showInAccountConsole.${showInAccountConsole.toLocaleLowerCase()}`,
          ),
        }))}
        controller={{
          defaultValue: SHOW_IN_ACCOUNT_CONSOLE_VALUES[0],
          rules: { required: t("required") },
        }}
      />

      {((!isSAML && !isOAuth2) || isOIDC) && (
        <FormGroupField label="filteredByClaim">
          <Controller
            name="config.filteredByClaim"
            defaultValue="false"
            control={control}
            render={({ field }) => (
              <Switch
                id="filteredByClaim"
                label={t("on")}
                labelOff={t("off")}
                isChecked={field.value === "true"}
                onChange={(_event, value) => {
                  field.onChange(value.toString());
                }}
              />
            )}
          />
        </FormGroupField>
      )}
      {(!isSAML || isOIDC) && claimFilterRequired && (
        <>
          <FormGroup
            label={t("claimFilterName")}
            labelIcon={
              <HelpItem
                helpText={t("claimFilterNameHelp")}
                fieldLabelId="claimFilterName"
              />
            }
            fieldId="kc-claim-filter-name"
            isRequired
          >
            <TextInput
              isRequired
              id="kc-claim-filter-name"
              data-testid="claimFilterName"
              validated={
                errors.config?.claimFilterName
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              {...register("config.claimFilterName", { required: true })}
            />
            {errors.config?.claimFilterName && (
              <FormErrorText message={t("required")} />
            )}
          </FormGroup>
          <FormGroup
            label={t("claimFilterValue")}
            labelIcon={
              <HelpItem
                helpText={t("claimFilterValueHelp")}
                fieldLabelId="claimFilterName"
              />
            }
            fieldId="kc-claim-filter-value"
            isRequired
          >
            <TextInput
              isRequired
              id="kc-claim-filter-value"
              data-testid="claimFilterValue"
              validated={
                errors.config?.claimFilterValue
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              {...register("config.claimFilterValue", { required: true })}
            />
            {errors.config?.claimFilterValue && (
              <FormErrorText message={t("required")} />
            )}
          </FormGroup>
        </>
      )}
      <LoginFlow
        field="firstBrokerLoginFlowAlias"
        label="firstBrokerLoginFlowAliasOverride"
        defaultValue=""
        labelForEmpty=""
      />
      <LoginFlow
        field="postBrokerLoginFlowAlias"
        label="postBrokerLoginFlowAlias"
        defaultValue=""
      />

      {isTransientUsersEnabled && (
        <FormGroupField label="doNotStoreUsers">
          <Controller
            name="config.doNotStoreUsers"
            defaultValue="false"
            control={control}
            render={({ field }) => (
              <Switch
                id="doNotStoreUsers"
                label={t("on")}
                labelOff={t("off")}
                isChecked={field.value === "true"}
                onChange={(_event, value) => {
                  field.onChange(value.toString());
                  // if field is checked, set sync mode to import
                  if (value) {
                    setValue("config.syncMode", "IMPORT");
                  }
                }}
              />
            )}
          />
        </FormGroupField>
      )}
      {syncModeAvailable && (
        <SelectControl
          name="config.syncMode"
          label={t("syncMode")}
          labelIcon={t("syncModeHelp")}
          options={SYNC_MODES.map((syncMode) => ({
            key: syncMode,
            value: t(`syncModes.${syncMode.toLocaleLowerCase()}`),
          }))}
          controller={{
            defaultValue: SYNC_MODES[0],
            rules: { required: t("required") },
          }}
        />
      )}
      <SwitchField
        field="config.caseSensitiveOriginalUsername"
        label="caseSensitiveOriginalUsername"
      />
      {isClientAuthFederatedEnabled && isOIDC && (
        <>
          <SwitchField
            field="config.supportsClientAssertions"
            label="supportsClientAssertions"
          />
          <SwitchField
            field="config.supportsClientAssertionReuse"
            label="supportsClientAssertionReuse"
          />
        </>
      )}
    </>
  );
};
