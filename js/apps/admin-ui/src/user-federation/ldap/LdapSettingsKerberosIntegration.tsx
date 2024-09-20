import { FormGroup, Switch } from "@patternfly/react-core";
import {
  Controller,
  FormProvider,
  UseFormReturn,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
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
  const { t } = useTranslation();

  const allowKerberosAuth: [string] = useWatch({
    control: form.control,
    name: "config.allowKerberosAuthentication",
    defaultValue: ["false"],
  });

  return (
    <FormProvider {...form}>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("kerberosIntegration")}
          description={t("ldapKerberosSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("allowKerberosAuthentication")}
          labelIcon={
            <HelpItem
              helpText={t("allowKerberosAuthenticationHelp")}
              fieldLabelId="allowKerberosAuthentication"
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
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("allowKerberosAuthentication")}
              />
            )}
          ></Controller>
        </FormGroup>

        {allowKerberosAuth[0] === "true" && (
          <>
            <TextControl
              name="config.kerberosRealm.0"
              label={t("kerberosRealm")}
              labelIcon={t("kerberosRealmHelp")}
              rules={{
                required: t("validateRealm"),
              }}
            />
            <TextControl
              name="config.serverPrincipal.0"
              label={t("serverPrincipal")}
              labelIcon={t("serverPrincipalHelp")}
              rules={{
                required: t("validateServerPrincipal"),
              }}
            />
            <TextControl
              name="config.keyTab.0"
              label={t("keyTab")}
              labelIcon={t("keyTabHelp")}
              rules={{
                required: t("validateKeyTab"),
              }}
            />
            <TextControl
              name="config.krbPrincipalAttribute.0"
              label={t("krbPrincipalAttribute")}
              labelIcon={t("krbPrincipalAttributeHelp")}
            />

            <FormGroup
              label={t("debug")}
              labelIcon={
                <HelpItem helpText={t("debugHelp")} fieldLabelId="debug" />
              }
              fieldId="kc-debug"
              hasNoPaddingTop
            >
              <Controller
                name="config.debug"
                defaultValue={["false"]}
                control={form.control}
                render={({ field }) => (
                  <Switch
                    id="kc-debug"
                    data-testid="debug"
                    isDisabled={false}
                    onChange={(_event, value) => field.onChange([`${value}`])}
                    isChecked={field.value[0] === "true"}
                    label={t("on")}
                    labelOff={t("off")}
                    aria-label={t("debug")}
                  />
                )}
              />
            </FormGroup>
          </>
        )}
        <FormGroup
          label={t("useKerberosForPasswordAuthentication")}
          labelIcon={
            <HelpItem
              helpText={t("useKerberosForPasswordAuthenticationHelp")}
              fieldLabelId="useKerberosForPasswordAuthentication"
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
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("useKerberosForPasswordAuthentication")}
              />
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </FormProvider>
  );
};
