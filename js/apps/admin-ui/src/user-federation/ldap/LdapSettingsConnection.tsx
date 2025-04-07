import type TestLdapConnectionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/testLdapConnection";
import {
  HelpItem,
  KeycloakSelect,
  PasswordControl,
  SelectControl,
  SelectVariant,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  FormGroup,
  SelectOption,
  Switch,
} from "@patternfly/react-core";
import { get, isEqual } from "lodash-es";
import { useState } from "react";
import {
  Controller,
  FormProvider,
  UseFormReturn,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useRealm } from "../../context/realm-context/RealmContext";

export type LdapSettingsConnectionProps = {
  form: UseFormReturn;
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

export const convertFormToSettings = (form: UseFormReturn) => {
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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const edit = !!id;

  const testLdap = async (testType: TestTypes) => {
    try {
      const settings = convertFormToSettings(form);
      await adminClient.realms.testLDAPConnection(
        { realm },
        { ...settings, action: testType, componentId: id },
      );
      addAlert(t("testSuccess"), AlertVariant.success);
    } catch (error) {
      addError("testError", error);
    }
  };

  const [isBindTypeDropdownOpen, setIsBindTypeDropdownOpen] = useState(false);

  const ldapBindType = useWatch({
    control: form.control,
    name: "config.authType",
    defaultValue: ["simple"],
  });

  return (
    <FormProvider {...form}>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("connectionAndAuthenticationSettings")}
          description={t("ldapConnectionAndAuthorizationSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}
      <FormAccess role="manage-realm" isHorizontal>
        <TextControl
          name="config.connectionUrl.0"
          label={t("connectionURL")}
          labelIcon={t("consoleDisplayConnectionUrlHelp")}
          type="url"
          rules={{
            required: t("validateConnectionUrl"),
          }}
        />
        <FormGroup
          label={t("enableStartTls")}
          labelIcon={
            <HelpItem
              helpText={t("enableStartTlsHelp")}
              fieldLabelId="enableStartTls"
            />
          }
          fieldId="kc-enable-start-tls"
          hasNoPaddingTop
        >
          <Controller
            name="config.startTls"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-enable-start-tls"}
                data-testid="enable-start-tls"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("enableStartTls")}
              />
            )}
          />
        </FormGroup>
        <SelectControl
          id="useTruststoreSpi"
          name="config.useTruststoreSpi[0]"
          label={t("useTruststoreSpi")}
          labelIcon={t("useTruststoreSpiHelp")}
          controller={{
            defaultValue: "always",
          }}
          options={[
            { key: "always", value: t("always") },
            { key: "never", value: t("never") },
          ]}
        />
        <FormGroup
          label={t("connectionPooling")}
          labelIcon={
            <HelpItem
              helpText={t("connectionPoolingHelp")}
              fieldLabelId="connectionPooling"
            />
          }
          fieldId="kc-connection-pooling"
          hasNoPaddingTop
        >
          <Controller
            name="config.connectionPooling"
            defaultValue={["true"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-connection-pooling"}
                data-testid="connection-pooling"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("connectionPooling")}
              />
            )}
          />
        </FormGroup>
        <TextControl
          name="config.connectionTimeout.0"
          label={t("connectionTimeout")}
          labelIcon={t("connectionTimeoutHelp")}
          type="number"
          min={0}
        />
        <FormGroup fieldId="kc-test-connection-button">
          <Button
            variant="secondary"
            id="kc-test-connection-button"
            data-testid="test-connection-button"
            onClick={() => testLdap("testConnection")}
          >
            {t("testConnection")}
          </Button>
        </FormGroup>
        <FormGroup
          label={t("bindType")}
          labelIcon={
            <HelpItem helpText={t("bindTypeHelp")} fieldLabelId="bindType" />
          }
          fieldId="kc-bind-type"
          isRequired
        >
          <Controller
            name="config.authType[0]"
            defaultValue="simple"
            control={form.control}
            render={({ field }) => (
              <KeycloakSelect
                toggleId="kc-bind-type"
                onToggle={() =>
                  setIsBindTypeDropdownOpen(!isBindTypeDropdownOpen)
                }
                isOpen={isBindTypeDropdownOpen}
                onSelect={(value) => {
                  field.onChange(value as string);
                  setIsBindTypeDropdownOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                data-testid="ldap-bind-type"
                aria-label={t("selectBindType")}
              >
                <SelectOption value="simple">simple</SelectOption>
                <SelectOption value="none">none</SelectOption>
              </KeycloakSelect>
            )}
          />
        </FormGroup>

        {isEqual(ldapBindType, ["simple"]) && (
          <>
            <TextControl
              name="config.bindDn.0"
              label={t("bindDn")}
              labelIcon={t("bindDnHelp")}
              rules={{
                required: t("validateBindDn"),
              }}
            />
            <PasswordControl
              name="config.bindCredential.0"
              label={t("bindCredentials")}
              labelIcon={t("bindCredentialsHelp")}
              hasReveal={!edit}
              rules={{
                required: t("validateBindCredentials"),
              }}
            />
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
    </FormProvider>
  );
};
