import { FormGroup, Switch } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { UseFormMethods, Controller, useWatch } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export type LdapSettingsKerberosIntegrationProps = {
  form: UseFormMethods;
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
              helpText="user-federation-help:allowKerberosAuthenticationHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id="kc-allow-kerberos-authentication"
                data-testid="allow-kerberos-auth"
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
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
                  helpText="user-federation-help:kerberosRealmHelp"
                  fieldLabelId="user-federation:kerberosRealm"
                />
              }
              fieldId="kc-kerberos-realm"
              isRequired
              validated={
                form.errors.config?.kerberosRealm?.[0] ? "error" : "default"
              }
              helperTextInvalid={form.errors.config?.kerberosRealm?.[0].message}
            >
              <KeycloakTextInput
                isRequired
                type="text"
                id="kc-kerberos-realm"
                name="config.kerberosRealm[0]"
                ref={form.register({
                  required: {
                    value: true,
                    message: `${t("validateRealm")}`,
                  },
                })}
                data-testid="kerberos-realm"
                validated={
                  form.errors.config?.kerberosRealm?.[0] ? "error" : "default"
                }
              />
            </FormGroup>

            <FormGroup
              label={t("serverPrincipal")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:serverPrincipalHelp"
                  fieldLabelId="user-federation:serverPrincipal"
                />
              }
              fieldId="kc-server-principal"
              isRequired
              validated={
                form.errors.config?.serverPrincipal?.[0] ? "error" : "default"
              }
              helperTextInvalid={
                form.errors.config?.serverPrincipal?.[0].message
              }
            >
              <KeycloakTextInput
                isRequired
                type="text"
                id="kc-server-principal"
                name="config.serverPrincipal[0]"
                ref={form.register({
                  required: {
                    value: true,
                    message: `${t("validateServerPrincipal")}`,
                  },
                })}
                data-testid="kerberos-principal"
                validated={
                  form.errors.config?.serverPrincipal?.[0] ? "error" : "default"
                }
              />
            </FormGroup>

            <FormGroup
              label={t("keyTab")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:keyTabHelp"
                  fieldLabelId="user-federation:keyTab"
                />
              }
              fieldId="kc-key-tab"
              isRequired
              validated={form.errors.config?.keyTab?.[0] ? "error" : "default"}
              helperTextInvalid={form.errors.config?.keyTab?.[0].message}
            >
              <KeycloakTextInput
                isRequired
                type="text"
                id="kc-key-tab"
                name="config.keyTab[0]"
                ref={form.register({
                  required: {
                    value: true,
                    message: `${t("validateKeyTab")}`,
                  },
                })}
                data-testid="kerberos-keytab"
                validated={
                  form.errors.config?.keyTab?.[0] ? "error" : "default"
                }
              />
            </FormGroup>

            <FormGroup
              label={t("debug")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:debugHelp"
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
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-debug"
                    data-testid="debug"
                    isDisabled={false}
                    onChange={(value) => onChange([`${value}`])}
                    isChecked={value[0] === "true"}
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
              helpText="user-federation-help:useKerberosForPasswordAuthenticationHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id="kc-use-kerberos-password-authentication"
                data-testid="use-kerberos-pw-auth"
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
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
