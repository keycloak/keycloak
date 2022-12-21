import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type { FieldProps } from "../component/FormGroupField";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

const LoginFlow = ({
  field,
  label,
  defaultValue,
}: FieldProps & { defaultValue: string }) => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();

  const { adminClient } = useAdminClient();
  const [flows, setFlows] = useState<AuthenticationFlowRepresentation[]>();
  const [open, setOpen] = useState(false);

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) =>
      setFlows(flows.filter((flow) => flow.providerId === "basic-flow")),
    []
  );

  return (
    <FormGroup
      label={t(label)}
      labelIcon={
        <HelpItem
          helpText={`identity-providers-help:${label}`}
          fieldLabelId={`identity-providers:${label}`}
        />
      }
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
            selections={field.value || t("common:none")}
            variant={SelectVariant.single}
            aria-label={t(label)}
            isOpen={open}
          >
            {/* The type for the children of Select are incorrect, so we need a fragment here. */}
            {/* eslint-disable-next-line react/jsx-no-useless-fragment */}
            <>
              {defaultValue === "" && (
                <SelectOption key="empty" value={defaultValue}>
                  {t("common:none")}
                </SelectOption>
              )}
            </>
            {/* The type for the children of Select are incorrect, so we need a fragment here. */}
            {/* eslint-disable-next-line react/jsx-no-useless-fragment */}
            <>
              {flows?.map((option) => (
                <SelectOption
                  selected={option.alias === field.value}
                  key={option.id}
                  value={option.alias}
                >
                  {option.alias}
                </SelectOption>
              ))}
            </>
          </Select>
        )}
      />
    </FormGroup>
  );
};

const syncModes = ["import", "legacy", "force"];
type AdvancedSettingsProps = { isOIDC: boolean; isSAML: boolean };

export const AdvancedSettings = ({ isOIDC, isSAML }: AdvancedSettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();
  const [syncModeOpen, setSyncModeOpen] = useState(false);
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
      <SwitchField field="trustEmail" label="trustEmail" fieldType="boolean" />
      <SwitchField
        field="linkOnly"
        label="accountLinkingOnly"
        fieldType="boolean"
      />
      <SwitchField field="config.hideOnLoginPage" label="hideOnLoginPage" />

      <LoginFlow
        field="firstBrokerLoginFlowAlias"
        label="firstBrokerLoginFlowAlias"
        defaultValue="fist broker login"
      />
      <LoginFlow
        field="postBrokerLoginFlowAlias"
        label="postBrokerLoginFlowAlias"
        defaultValue=""
      />

      <FormGroup
        className="pf-u-pb-3xl"
        label={t("syncMode")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:syncMode"
            fieldLabelId="identity-providers:syncMode"
          />
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
    </>
  );
};
