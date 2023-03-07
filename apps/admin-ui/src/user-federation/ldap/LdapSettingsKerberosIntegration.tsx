// @ts-nocheck
import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, UseFormReturn, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type LdapSettingsKerberosIntegrationProps = {
  form: UseFormReturn;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const LdapSettingsKerberosIntegration = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsKerberosIntegrationProps) => {
  const { t } = useTranslation("user-federation");
  const { t: helpText } = useTranslation("user-federation-help");

  const allowKerberosAuth: [string] = useWatch({
    control: form.control,
    name: "config.allowKerberosAuthentication",
    defaultValue: ["false"],
  });

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("kerberosIntegration")}
          description={helpText("ldapKerberosSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("allowKerberosAuthentication")}
          labelIcon={
            <HelpItem
              helpText={t(
                "user-federation-help:allowKerberosAuthenticationHelp"
              )}
              fieldLabelId="user-federation:allowKerberosAuthentication"
            />
          }
          fieldId="kc-allow-kerberos-authentication"
          hasNoPaddingTop
        >
          <Controller
            name="config.allowKerberosAuthentication"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-allow-kerberos-authentication"
                data-testid="allow-kerberos-auth"
                isDisabled={false}
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("allowKerberosAuthentication")}
              />
            )}
          ></Controller>
        </FormGroup>

        {allowKerberosAuth[0] === "true" && (
          <>
            <FormGroup
              label={t("kerberosRealm")}
              labelIcon={
                <HelpItem
                  helpText={t("user-federation-help:kerberosRealmHelp")}
                  fieldLabelId="user-federation:kerberosRealm"
                />
              }
              fieldId="kc-kerberos-realm"
              isRequired
              validated={
                form.formState.errors.config?.kerberosRealm?.[0]
                  ? "error"
                  : "default"
              }
              helperTextInvalid={
                form.formState.errors.config?.kerberosRealm?.[0].message
              }
            >
              <KeycloakTextInput
                isRequired
                id="kc-kerberos-realm"
                data-testid="kerberos-realm"
                validated={
                  form.formState.errors.config?.kerberosRealm?.[0]
                    ? "error"
                    : "default"
                }
                {...form.register("config.kerberosRealm.0", {
                  required: {
                    value: true,
                    message: t("validateRealm").toString(),
                  },
                })}
              />
            </FormGroup>

            <FormGroup
              label={t("serverPrincipal")}
              labelIcon={
                <HelpItem
                  helpText={t("user-federation-help:serverPrincipalHelp")}
                  fieldLabelId="user-federation:serverPrincipal"
                />
              }
              fieldId="kc-server-principal"
              isRequired
              validated={
                form.formState.errors.config?.serverPrincipal?.[0]
                  ? "error"
                  : "default"
              }
              helperTextInvalid={
                form.formState.errors.config?.serverPrincipal?.[0].message
              }
            >
              <KeycloakTextInput
                isRequired
                id="kc-server-principal"
                data-testid="kerberos-principal"
                validated={
                  form.formState.errors.config?.serverPrincipal?.[0]
                    ? "error"
                    : "default"
                }
                {...form.register("config.serverPrincipal.0", {
                  required: {
                    value: true,
                    message: `${t("validateServerPrincipal")}`,
                  },
                })}
              />
            </FormGroup>

            <FormGroup
              label={t("keyTab")}
              labelIcon={
                <HelpItem
                  helpText={t("user-federation-help:keyTabHelp")}
                  fieldLabelId="user-federation:keyTab"
                />
              }
              fieldId="kc-key-tab"
              isRequired
              validated={
                form.formState.errors.config?.keyTab?.[0] ? "error" : "default"
              }
              helperTextInvalid={
                form.formState.errors.config?.keyTab?.[0].message
              }
            >
              <KeycloakTextInput
                isRequired
                id="kc-key-tab"
                data-testid="kerberos-keytab"
                validated={
                  form.formState.errors.config?.keyTab?.[0]
                    ? "error"
                    : "default"
                }
                {...form.register("config.keyTab.0", {
                  required: {
                    value: true,
                    message: `${t("validateKeyTab")}`,
                  },
                })}
              />
            </FormGroup>

            <FormGroup
              label={t("debug")}
              labelIcon={
                <HelpItem
                  helpText={t("user-federation-help:debugHelp")}
                  fieldLabelId="user-federation:debug"
                />
              }
              fieldId="kc-debug"
              hasNoPaddingTop
            >
              {" "}
              <Controller
                name="config.debug"
                defaultValue={["false"]}
                control={form.control}
                render={({ field }) => (
                  <Switch
                    id="kc-debug"
                    data-testid="debug"
                    isDisabled={false}
                    onChange={(value) => field.onChange([`${value}`])}
                    isChecked={field.value[0] === "true"}
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    aria-label={t("debug")}
                  />
                )}
              ></Controller>
            </FormGroup>
          </>
        )}
        <FormGroup
          label={t("useKerberosForPasswordAuthentication")}
          labelIcon={
            <HelpItem
              helpText={t(
                "user-federation-help:useKerberosForPasswordAuthenticationHelp"
              )}
              fieldLabelId="user-federation:useKerberosForPasswordAuthentication"
            />
          }
          fieldId="kc-use-kerberos-password-authentication"
          hasNoPaddingTop
        >
          <Controller
            name="config.useKerberosForPasswordAuthentication"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-use-kerberos-password-authentication"
                data-testid="use-kerberos-pw-auth"
                isDisabled={false}
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("useKerberosForPasswordAuthentication")}
              />
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
