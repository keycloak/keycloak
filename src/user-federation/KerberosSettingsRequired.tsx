import {
  Form,
  FormGroup,
  Select,
  SelectOption,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";

export const KerberosSettingsRequired = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      {/* Required settings */}
      <Form isHorizontal>
        <FormGroup
          label={t("consoleDisplayName")}
          labelIcon={
            <HelpItem
              helpText={helpText("consoleDisplayNameHelp")}
              forLabel={t("consoleDisplayName")}
              forID="kc-console-display-name"
            />
          }
          fieldId="kc-console-display-name"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-console-display-name"
            name="kc-console-display-name"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("kerberosRealm")}
          labelIcon={
            <HelpItem
              helpText={helpText("kerberosRealmHelp")}
              forLabel={t("kerberosRealm")}
              forID="kc-kerberos-realm"
            />
          }
          fieldId="kc-kerberos-realm"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-kerberos-realm"
            name="kc-kerberos-realm"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("serverPrincipal")}
          labelIcon={
            <HelpItem
              helpText={helpText("serverPrincipalHelp")}
              forLabel={t("serverPrincipal")}
              forID="kc-server-principal"
            />
          }
          fieldId="kc-server-principal"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-server-principal"
            name="kc-server-principal"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("keyTab")}
          labelIcon={
            <HelpItem
              helpText={helpText("keyTabHelp")}
              forLabel={t("keyTab")}
              forID="kc-key-tab"
            />
          }
          fieldId="kc-key-tab"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-key-tab"
            name="kc-key-tab"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("debug")}
          labelIcon={
            <HelpItem
              helpText={helpText("debugHelp")}
              forLabel={t("debug")}
              forID="kc-debug"
            />
          }
          fieldId="kc-debug"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-debug"}
            isChecked={true}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>

        <FormGroup
          label={t("allowPasswordAuthentication")}
          labelIcon={
            <HelpItem
              helpText={helpText("allowPasswordAuthenticationHelp")}
              forLabel={t("allowPasswordAuthentication")}
              forID="kc-allow-password-authentication"
            />
          }
          fieldId="kc-allow-password-authentication"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-allow-password-authentication"}
            isChecked={true}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>

        <FormGroup
          label={t("editMode")}
          labelIcon={
            <HelpItem
              helpText={helpText("editModeHelp")}
              forLabel={t("editMode")}
              forID="kc-edit-mode"
            />
          }
          fieldId="kc-edit-mode"
        >
          <Select
            toggleId="kc-edit-mode"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="edit mode" // TODO
          >
            {/* {configFormats.map((configFormat) => ( */}
            <SelectOption
              key={"key"}
              value={"value"}
              // isSelected={selected === configFormat.id}
            >
              {"display name"}
            </SelectOption>
            {/* ))} */}
          </Select>
        </FormGroup>

        <FormGroup
          label={t("updateFirstLogin")}
          labelIcon={
            <HelpItem
              helpText={helpText("updateFirstLoginHelp")}
              forLabel={t("updateFirstLogin")}
              forID="kc-update-first-login"
            />
          }
          fieldId="kc-update-first-login"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-update-first-login"}
            isChecked={true}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>
      </Form>
    </>
  );
};
