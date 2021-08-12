import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation from "keycloak-admin/lib/defs/authenticationFlowRepresentation";
import { useFetch, useAdminClient } from "../../context/auth/AdminClient";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import type { FieldProps } from "../component/FormGroupField";

const LoginFlow = ({
  field,
  label,
  defaultValue,
}: FieldProps & { defaultValue: string }) => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();

  const adminClient = useAdminClient();
  const [flows, setFlows] = useState<AuthenticationFlowRepresentation[]>();
  const [open, setOpen] = useState(false);

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) =>
      setFlows(flows?.filter((flow) => flow.providerId === "basic-flow")),
    []
  );

  return (
    <FormGroup
      label={t(label)}
      labelIcon={
        <HelpItem
          helpText={`identity-providers-help:${label}`}
          forLabel={t(label)}
          forID={label}
        />
      }
      fieldId={label}
    >
      <Controller
        name={field}
        defaultValue={defaultValue}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId={label}
            required
            onToggle={() => setOpen(!open)}
            onSelect={(_, value) => {
              onChange(value as string);
              setOpen(false);
            }}
            selections={value || t("common:none")}
            variant={SelectVariant.single}
            aria-label={t(label)}
            isOpen={open}
          >
            <>
              {defaultValue === "" && (
                <SelectOption key="empty" value={defaultValue}>
                  {t("common:none")}
                </SelectOption>
              )}
            </>
            <>
              {flows?.map((option) => (
                <SelectOption
                  selected={option.alias === value}
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

export const AdvancedSettings = ({ isOIDC }: { isOIDC: boolean }) => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  return (
    <>
      {!isOIDC && <TextField field="config.defaultScope" label="scopes" />}
      <SwitchField field="storeToken" label="storeTokens" fieldType="boolean" />
      {!isOIDC && (
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
        label={t("syncMode")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:syncMode"
            forLabel={t("syncMode")}
            forID="syncMode"
          />
        }
        fieldId="syncMode"
      >
        <Controller
          name="config.syncMode"
          defaultValue={syncModes[0]}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="syncMode"
              required
              direction="up"
              onToggle={() => setSyncModeOpen(!syncModeOpen)}
              onSelect={(_, value) => {
                onChange(value as string);
                setSyncModeOpen(false);
              }}
              selections={t(`syncModes.${value.toLowerCase()}`)}
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
            >
              {syncModes.map((option) => (
                <SelectOption
                  selected={option === value}
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
