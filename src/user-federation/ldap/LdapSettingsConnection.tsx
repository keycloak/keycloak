import {
  Button,
  FormGroup,
  InputGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useEffect, useState } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { Controller, useForm } from "react-hook-form";
import { convertToFormValues } from "../../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { EyeIcon } from "@patternfly/react-icons";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useParams } from "react-router-dom";

export const LdapSettingsConnection = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;
  const adminClient = useAdminClient();
  const { register, control, setValue } = useForm<ComponentRepresentation>();
  const { id } = useParams<{ id: string }>();

  const convertTruststoreSpiValues = (truststoreValue: string) => {
    switch (truststoreValue) {
      case "always":
        return `${t("always")}`;
      case "never":
        return `${t("never")}`;
      case "ldapsOnly":
      default:
        return `${t("onlyLdaps")}`;
    }
  };

  const [
    isTruststoreSpiDropdownOpen,
    setIsTruststoreSpiDropdownOpen,
  ] = useState(false);

  const [isBindTypeDropdownOpen, setIsBindTypeDropdownOpen] = useState(false);

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", setValue);
        if (entry[1].useTruststoreSpi) {
          setValue(
            "config.useTruststoreSpi",
            convertTruststoreSpiValues(entry[1].useTruststoreSpi[0])
          );
        }
      } else {
        setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("connectionURL")}
          labelIcon={
            <HelpItem
              helpText={helpText("consoleDisplayConnectionUrlHelp")}
              forLabel={t("connectionURL")}
              forID="kc-console-connection-url"
            />
          }
          fieldId="kc-console-connection-url"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-console-connection-url"
            name="config.connectionUrl"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("enableStartTls")}
          labelIcon={
            <HelpItem
              helpText={helpText("enableStartTlsHelp")}
              forLabel={t("enableStartTls")}
              forID="kc-enable-start-tls"
            />
          }
          fieldId="kc-enable-start-tls"
          hasNoPaddingTop
        >
          <Controller
            name="config.startTls"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-enable-start-tls"}
                isChecked={value[0] === "true"}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
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
          <Controller
            name="config.useTruststoreSpi"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-use-truststore-spi"
                onToggle={() =>
                  setIsTruststoreSpiDropdownOpen(!isTruststoreSpiDropdownOpen)
                }
                isOpen={isTruststoreSpiDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsTruststoreSpiDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption key={0} value={t("always")} />
                <SelectOption key={1} value={t("onlyLdaps")} />
                <SelectOption key={2} value={t("never")} />
              </Select>
            )}
          ></Controller>
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
          <Controller
            name="config.connectionPooling"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-connection-pooling"}
                isDisabled={false}
                onChange={onChange}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("connectionTimeout")}
          labelIcon={
            <HelpItem
              helpText={helpText("connectionTimeoutHelp")}
              forLabel={t("connectionTimeout")}
              forID="kc-console-connection-timeout"
            />
          }
          fieldId="kc-console-connection-timeout"
        >
          <TextInput
            type="text"
            id="kc-console-connection-timeout"
            name="config.connectionTimeout"
            ref={register}
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
          <Controller
            name="config.authType"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-bind-type"
                required
                onToggle={() =>
                  setIsBindTypeDropdownOpen(!isBindTypeDropdownOpen)
                }
                isOpen={isBindTypeDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsBindTypeDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption key={3} value="simple" />
                <SelectOption key={4} value="none" />
              </Select>
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("bindDn")}
          labelIcon={
            <HelpItem
              helpText={helpText("bindDnHelp")}
              forLabel={t("bindDn")}
              forID="kc-console-bind-dn"
            />
          }
          fieldId="kc-console-bind-dn"
        >
          <TextInput
            type="text"
            id="kc-console-bind-dn"
            name="config.bindDn"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("bindCredentials")}
          labelIcon={
            <HelpItem
              helpText={helpText("bindCredentialsHelp")}
              forLabel={t("bindCredentials")}
              forID="kc-console-bind-credentials"
            />
          }
          fieldId="kc-console-bind-credentials"
          isRequired
        >
          {/* TODO: MF The input group below throws a 'React does not recognize the `isDisabled` prop on a DOM element' error */}
          <InputGroup>
            <TextInput // TODO: Make password field switch to type=text with button
              isRequired
              type="password"
              id="kc-console-bind-credentials"
              name="config.bindCredential"
              ref={register}
            />
            <Button
              variant="control"
              aria-label="show password button for bind credentials"
            >
              <EyeIcon />
            </Button>
          </InputGroup>
        </FormGroup>
        <FormGroup fieldId="kc-test-button">
          {" "}
          {/* TODO: whatever this button is supposed to do */}
          <Button variant="secondary" id="kc-test-button">
            Test
          </Button>
        </FormGroup>
      </FormAccess>
    </>
  );
};
