import { FormGroup, Switch, TextInput } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { UseFormMethods, Controller, useWatch } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

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
  const helpText = useTranslation("user-federation-help").t;

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
              helpText={helpText("allowKerberosAuthenticationHelp")}
              forLabel={t("allowKerberosAuthentication")}
              forID="kc-allow-kerberos-authentication"
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
                id={"kc-allow-kerberos-authentication"}
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
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
                name="config.kerberosRealm[0]"
                ref={form.register({
                  required: {
                    value: true,
                    message: `${t("validateRealm")}`,
                  },
                })}
                data-testid="kerberos-realm"
              />
              {form.errors.config?.kerberosRealm?.[0] && (
                <div className="error">
                  {form.errors.config.kerberosRealm[0].message}
                </div>
              )}
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
                name="config.serverPrincipal[0]"
                ref={form.register({
                  required: {
                    value: true,
                    message: `${t("validateServerPrincipal")}`,
                  },
                })}
                data-testid="kerberos-principal"
              />
              {form.errors.config?.serverPrincipal?.[0] && (
                <div className="error">
                  {form.errors.config.serverPrincipal[0].message}
                </div>
              )}
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
                name="config.keyTab[0]"
                ref={form.register({
                  required: {
                    value: true,
                    message: `${t("validateKeyTab")}`,
                  },
                })}
                data-testid="kerberos-keytab"
              />
              {form.errors.config?.keyTab?.[0] && (
                <div className="error">
                  {form.errors.config.keyTab[0].message}
                </div>
              )}
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
              {" "}
              <Controller
                name="config.debug"
                defaultValue={["false"]}
                control={form.control}
                render={({ onChange, value }) => (
                  <Switch
                    id={"kc-debug"}
                    isDisabled={false}
                    onChange={(value) => onChange([`${value}`])}
                    isChecked={value[0] === "true"}
                    label={t("common:on")}
                    labelOff={t("common:off")}
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
              helpText={helpText("useKerberosForPasswordAuthenticationHelp")}
              forLabel={t("useKerberosForPasswordAuthentication")}
              forID="kc-use-kerberos-password-authentication"
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
                id={"kc-use-kerberos-password-authentication"}
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
