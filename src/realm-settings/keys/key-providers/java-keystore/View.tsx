import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, Controller } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";

import { useServerInfo } from "../../../../context/server-info/ServerInfoProvider";
import { HelpItem } from "../../../../components/help-enabler/HelpItem";
import { KEY_PROVIDER_TYPE } from "../../../../util";
import useToggle from "../../../../utils/useToggle";

export default function View() {
  const { t } = useTranslation("realm-settings");
  const { control } = useFormContext();
  const [isAlgorithmDropdownOpen, toggleDropdown] = useToggle();

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const javaKeystoreAlgorithmOptions =
    allComponentTypes[3].properties[3].options ?? [];

  return (
    <>
      <FormGroup
        label={t("algorithm")}
        fieldId="kc-algorithm"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:algorithm"
            fieldLabelId="realm-settings:algorithm"
          />
        }
      >
        <Controller
          name="config.algorithm"
          control={control}
          defaultValue={[javaKeystoreAlgorithmOptions[0]]}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-elliptic"
              onToggle={toggleDropdown}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                toggleDropdown();
              }}
              selections={[value.toString()]}
              variant={SelectVariant.single}
              aria-label={t("algorithm")}
              isOpen={isAlgorithmDropdownOpen}
            >
              {javaKeystoreAlgorithmOptions!.map((p, idx) => (
                <SelectOption selected={p === value} key={idx} value={p} />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keystore")}
        fieldId="kc-keystore"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keystore"
            fieldLabelId="realm-settings:keystore"
          />
        }
      >
        <Controller
          name="config.keystore"
          control={control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("keystore")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="keystore"
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keystorePassword")}
        fieldId="kc-keystore-password"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keystorePassword"
            fieldLabelId="realm-settings:keystorePassword"
          />
        }
      >
        <Controller
          name="config.keystorePassword"
          control={control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("consoleDisplayName")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="keystorePassword"
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keyAlias")}
        fieldId="kc-key-alias"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keyAlias"
            fieldLabelId="realm-settings:keyAlias"
          />
        }
      >
        <Controller
          name="config.keyAlias"
          control={control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("keyAlias")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="key-alias"
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("keyPassword")}
        fieldId="kc-key-password"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keyPassword"
            fieldLabelId="realm-settings:keyPassword"
          />
        }
      >
        <Controller
          name="config.keyPassword"
          control={control}
          defaultValue={[]}
          render={({ onChange }) => (
            <TextInput
              aria-label={t("keyPassword")}
              onChange={(value) => {
                onChange([value.toString()]);
              }}
              data-testid="key-password"
            />
          )}
        />
      </FormGroup>
    </>
  );
}
