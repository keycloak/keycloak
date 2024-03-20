import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import type { FieldProps } from "../component/FormGroupField";
import { FormGroupField } from "../component/FormGroupField";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

const LoginFlow = ({
  field,
  label,
  defaultValue,
  labelForEmpty = "none",
}: FieldProps & { defaultValue: string; labelForEmpty?: string }) => {
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
          <Select
            toggleId={label}
            required
            onToggle={() => setOpen(!open)}
            onSelect={(_, value) => {
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
          </Select>
        )}
      />
    </FormGroup>
  );
};

const syncModes = ["import", "legacy", "force"];
type AdvancedSettingsProps = { isOIDC: boolean; isSAML: boolean };

export const AdvancedSettings = ({ isOIDC, isSAML }: AdvancedSettingsProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    setValue,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  const filteredByClaim = useWatch({
    control,
    name: "config.filteredByClaim",
    defaultValue: "false",
  });
  const claimFilterRequired = filteredByClaim === "true";
  const isFeatureEnabled = useIsFeatureEnabled();
  const isTransientUsersEnabled = isFeatureEnabled(Feature.TransientUsers);
  const transientUsers = useWatch({
    control,
    name: "config.doNotStoreUsers",
    defaultValue: "false",
  });
  const syncModeAvailable = transientUsers === "false";
  return (
    <>
      {!isOIDC && !isSAML && (
        <TextField field="config.defaultScope" label="scopes" />
      )}
      <SwitchField field="storeToken" label="storeTokens" fieldType="boolean" />
      {(isSAML || isOIDC) && (
        <SwitchField
          field="addReadTokenRoleOnCreate"
          label="storedTokensReadable"
          fieldType="boolean"
        />
      )}
      {!isOIDC && !isSAML && (
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
      <SwitchField field="config.hideOnLoginPage" label="hideOnLoginPage" />

      {(!isSAML || isOIDC) && (
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
                onChange={(value) => {
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
            validated={
              errors.config?.claimFilterName
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("required")}
          >
            <KeycloakTextInput
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
            validated={
              errors.config?.claimFilterValue
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("required")}
          >
            <KeycloakTextInput
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
                onChange={(value) => {
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
        <FormGroup
          className="pf-u-pb-3xl"
          label={t("syncMode")}
          labelIcon={
            <HelpItem helpText={t("syncModeHelp")} fieldLabelId="syncMode" />
          }
          fieldId="syncMode"
        >
          <Controller
            name="config.syncMode"
            defaultValue={syncModes[0].toUpperCase()}
            control={control}
            render={({ field }) => (
              <Select
                toggleId="syncMode"
                required
                direction="up"
                onToggle={() => setSyncModeOpen(!syncModeOpen)}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setSyncModeOpen(false);
                }}
                selections={t(`syncModes.${field.value.toLowerCase()}`)}
                variant={SelectVariant.single}
                aria-label={t("syncMode")}
                isOpen={syncModeOpen}
              >
                {syncModes.map((option) => (
                  <SelectOption
                    selected={option === field.value}
                    key={option}
                    value={option.toUpperCase()}
                  >
                    {t(`syncModes.${option}`)}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
      )}
    </>
  );
};
