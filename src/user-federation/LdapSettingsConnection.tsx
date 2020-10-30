import {
  Button,
  Form,
  FormGroup,
  InputGroup,
  Select,
  SelectOption,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { EyeIcon } from "@patternfly/react-icons";

export const LdapSettingsConnection = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      {/* Cache settings */}
      <Form isHorizontal>
        <FormGroup
          label={t("connectionURL")}
          labelIcon={
            <HelpItem
              helpText={helpText("consoleDisplayConnectionUrlHelp")}
              forLabel={t("connectionURL")}
              forID="kc-connection-url"
            />
          }
          fieldId="kc-connection-url"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-connection-url"
            name="kc-connection-url"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("enableStarttls")}
          labelIcon={
            <HelpItem
              helpText={helpText("enableStarttlsHelp")}
              forLabel={t("enableStarttls")}
              forID="kc-enable-start-tls"
            />
          }
          fieldId="kc-enable-start-tls"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-enable-start-tls"}
            isChecked={true}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>

        <FormGroup
          label={t("useTruststoreSpi")}
          labelIcon={
            <HelpItem
              helpText={helpText("useTruststoreSpiHelp")}
              forLabel={t("useTruststoreSpi")}
              forID="kc-use-truststore-spi"
            />
          }
          fieldId="kc-use-truststore-spi"
        >
          <Select
            toggleId="kc-use-truststore-spi"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="Only for LDAPS" // TODO
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
          label={t("connectionPooling")}
          labelIcon={
            <HelpItem
              helpText={helpText("connectionPoolingHelp")}
              forLabel={t("connectionPooling")}
              forID="kc-connection-pooling"
            />
          }
          fieldId="kc-connection-pooling"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-connection-pooling"}
            isChecked={true}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>

        <FormGroup
          label={t("connectionTimeout")}
          labelIcon={
            <HelpItem
              helpText={helpText("connectionTimeoutHelp")}
              forLabel={t("connectionTimeout")}
              forID="kc-connection-timeout"
            />
          }
          fieldId="kc-connection-timeout"
        >
          <TextInput
            type="text"
            id="kc-connection-timeout"
            name="kc-connection-timeout"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("bindType")}
          labelIcon={
            <HelpItem
              helpText={helpText("bindTypeHelp")}
              forLabel={t("bindType")}
              forID="kc-bind-type"
            />
          }
          fieldId="kc-bind-type"
          isRequired
        >
          <Select
            toggleId="kc-bind-type"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="simple" // TODO
          ></Select>
        </FormGroup>

        <FormGroup
          label={t("bindDn")}
          labelIcon={
            <HelpItem
              helpText={helpText("bindDnHelp")}
              forLabel={t("bindDn")}
              forID="kc-bind-dn"
            />
          }
          fieldId="kc-bind-dn"
        >
          <TextInput
            type="text"
            id="kc-bind-dn"
            name="kc-bind-dn"
            // value={value1}
            // onChange={this.handleTextInputChange1}
          />
        </FormGroup>

        <FormGroup
          label={t("bindCredentials")}
          labelIcon={
            <HelpItem
              helpText={helpText("bindCredentialsHelp")}
              forLabel={t("bindCredentials")}
              forID="kc-bind-credentials"
            />
          }
          fieldId="kc-bind-credentials"
          isRequired
        >
          <InputGroup>
            <TextInput
              name="kc-bind-credentials"
              id="kc-bind-credentials"
              type="password"
              aria-label="bind credentials"
              isRequired
            />
            <Button
              variant="control"
              aria-label="show password button for bind credentials"
            >
              <EyeIcon />
            </Button>
          </InputGroup>
        </FormGroup>
      </Form>
    </>
  );
};
