import {
  AlertVariant,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { get, isEqual } from "lodash-es";

import type TestLdapConnectionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/testLdapConnection";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods, useWatch } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { PasswordInput } from "../../components/password-input/PasswordInput";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export type LdapSettingsConnectionProps = {
  form: UseFormMethods;
  id?: string;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

const testLdapProperties: Array<keyof TestLdapConnectionRepresentation> = [
  "connectionUrl",
  "bindDn",
  "bindCredential",
  "useTruststoreSpi",
  "connectionTimeout",
  "startTls",
  "authType",
];

type TestTypes = "testConnection" | "testAuthentication";

export const convertFormToSettings = (form: UseFormMethods) => {
  const settings: TestLdapConnectionRepresentation = {};

  testLdapProperties.forEach((key) => {
    const value = get(form.getValues(), `config.${key}`);
    settings[key] = Array.isArray(value) ? value[0] : "";
  });

  return settings;
};

export const LdapSettingsConnection = ({
  form,
  id,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsConnectionProps) => {
  const { t } = useTranslation("user-federation");
  const { t: helpText } = useTranslation("user-federation-help");
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const edit = !!id;

  const testLdap = async (testType: TestTypes) => {
    try {
      const settings = convertFormToSettings(form);
      await adminClient.realms.testLDAPConnection(
        { realm },
        { ...settings, action: testType, componentId: id }
      );
      addAlert(t("testSuccess"), AlertVariant.success);
    } catch (error) {
      addError("user-federation:testError", error);
    }
  };

  const [isTruststoreSpiDropdownOpen, setIsTruststoreSpiDropdownOpen] =
    useState(false);

  const [isBindTypeDropdownOpen, setIsBindTypeDropdownOpen] = useState(false);

  const ldapBindType = useWatch({
    control: form.control,
    name: "config.authType",
    defaultValue: ["simple"],
  });

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("connectionAndAuthenticationSettings")}
          description={helpText(
            "ldapConnectionAndAuthorizationSettingsDescription"
          )}
          showDescription={showSectionDescription}
        />
      )}
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("connectionURL")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:consoleDisplayConnectionUrlHelp"
              fieldLabelId="user-federation:connectionURL"
            />
          }
          fieldId="kc-ui-connection-url"
          isRequired
          validated={
            form.errors.config?.connectionUrl?.[0] ? "error" : "default"
          }
          helperTextInvalid={form.errors.config?.connectionUrl?.[0].message}
        >
          <KeycloakTextInput
            isRequired
            type="url"
            id="kc-ui-connection-url"
            data-testid="ldap-connection-url"
            name="config.connectionUrl[0]"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateConnectionUrl")}`,
              },
            })}
            validated={
              form.errors.config?.connectionUrl?.[0] ? "error" : "default"
            }
          />
        </FormGroup>
        <FormGroup
          label={t("enableStartTls")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:enableStartTlsHelp"
              fieldLabelId="user-federation:enableStartTls"
            />
          }
          fieldId="kc-enable-start-tls"
          hasNoPaddingTop
        >
          <Controller
            name="config.startTls"
            defaultValue={["false"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-enable-start-tls"}
                data-testid="enable-start-tls"
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("enableStartTls")}
              />
            )}
          ></Controller>
        </FormGroup>

        <FormGroup
          label={t("useTruststoreSpi")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:useTruststoreSpiHelp"
              fieldLabelId="user-federation:useTruststoreSpi"
            />
          }
          fieldId="kc-use-truststore-spi"
        >
          <Controller
            name="config.useTruststoreSpi[0]"
            control={form.control}
            defaultValue="ldapsOnly"
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-use-truststore-spi"
                onToggle={() =>
                  setIsTruststoreSpiDropdownOpen(!isTruststoreSpiDropdownOpen)
                }
                isOpen={isTruststoreSpiDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value.toString());
                  setIsTruststoreSpiDropdownOpen(false);
                }}
                selections={value}
              >
                <SelectOption value="always">{t("always")}</SelectOption>
                <SelectOption value="ldapsOnly">{t("onlyLdaps")}</SelectOption>
                <SelectOption value="never">{t("never")}</SelectOption>
              </Select>
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("connectionPooling")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:connectionPoolingHelp"
              fieldLabelId="user-federation:connectionPooling"
            />
          }
          fieldId="kc-connection-pooling"
          hasNoPaddingTop
        >
          <Controller
            name="config.connectionPooling"
            defaultValue={["false"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-connection-pooling"}
                data-testid="connection-pooling"
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("connectionPooling")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("connectionTimeout")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:connectionTimeoutHelp"
              fieldLabelId="user-federation:consoleTimeout"
            />
          }
          fieldId="kc-ui-connection-timeout"
        >
          <KeycloakTextInput
            type="number"
            min={0}
            id="kc-ui-connection-timeout"
            data-testid="connection-timeout"
            name="config.connectionTimeout[0]"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup fieldId="kc-test-connection-button">
          <Button
            variant="secondary"
            id="kc-test-connection-button"
            data-testid="test-connection-button"
            onClick={() => testLdap("testConnection")}
          >
            {t("common:testConnection")}
          </Button>
        </FormGroup>
        <FormGroup
          label={t("bindType")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:bindTypeHelp"
              fieldLabelId="user-federation:bindType"
            />
          }
          fieldId="kc-bind-type"
          isRequired
        >
          <Controller
            name="config.authType[0]"
            defaultValue="simple"
            control={form.control}
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
                data-testid="ldap-bind-type"
              >
                <SelectOption value="simple" />
                <SelectOption value="none" />
              </Select>
            )}
          ></Controller>
        </FormGroup>

        {isEqual(ldapBindType, ["simple"]) && (
          <>
            <FormGroup
              label={t("bindDn")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:bindDnHelp"
                  fieldLabelId="user-federation:bindDn"
                />
              }
              fieldId="kc-ui-bind-dn"
              helperTextInvalid={t("validateBindDn")}
              validated={
                form.errors.config?.bindDn
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isRequired
            >
              <KeycloakTextInput
                type="text"
                id="kc-ui-bind-dn"
                data-testid="ldap-bind-dn"
                name="config.bindDn[0]"
                ref={form.register({ required: true })}
                validated={
                  form.errors.config?.bindDn
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
            <FormGroup
              label={t("bindCredentials")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:bindCredentialsHelp"
                  fieldLabelId="user-federation:bindCredentials"
                />
              }
              fieldId="kc-ui-bind-credentials"
              helperTextInvalid={t("validateBindCredentials")}
              validated={
                form.errors.config?.bindCredential
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isRequired
            >
              <PasswordInput
                hasReveal={!edit}
                isRequired
                id="kc-ui-bind-credentials"
                data-testid="ldap-bind-credentials"
                name="config.bindCredential[0]"
                ref={form.register({
                  required: true,
                })}
                validated={
                  form.errors.config?.bindCredential
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
          </>
        )}
        <FormGroup fieldId="kc-test-auth-button">
          <Button
            variant="secondary"
            id="kc-test-auth-button"
            data-testid="test-auth-button"
            onClick={() => testLdap("testAuthentication")}
          >
            {t("testAuthentication")}
          </Button>
        </FormGroup>
      </FormAccess>
    </>
  );
};
