import { Button, FormGroup, Switch } from "@patternfly/react-core";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormToSettings } from "./LdapSettingsConnection";

export type LdapSettingsAdvancedProps = {
  id?: string;
  form: UseFormReturn;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

const PASSWORD_MODIFY_OID = "1.3.6.1.4.1.4203.1.11.1";

export const LdapSettingsAdvanced = ({
  id,
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsAdvancedProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const testLdap = async () => {
    if (!(await form.trigger())) return;
    try {
      const settings = convertFormToSettings(form);
      const ldapOids = await adminClient.realms.ldapServerCapabilities(
        { realm },
        { ...settings, componentId: id },
      );
      addAlert(t("testSuccess"));
      const passwordModifyOid = ldapOids.filter(
        (id: { oid: string }) => id.oid === PASSWORD_MODIFY_OID,
      );
      form.setValue("config.usePasswordModifyExtendedOp", [
        (passwordModifyOid.length > 0).toString(),
      ]);
    } catch (error) {
      addError("testError", error);
    }
  };

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("advancedSettings")}
          description={t("ldapAdvancedSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("enableLdapv3Password")}
          labelIcon={
            <HelpItem
              helpText={t("enableLdapv3PasswordHelp")}
              fieldLabelId="enableLdapv3Password"
            />
          }
          fieldId="kc-enable-ldapv3-password"
          hasNoPaddingTop
        >
          <Controller
            name="config.usePasswordModifyExtendedOp"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-enable-ldapv3-password"}
                data-testid="ldapv3-password"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("enableLdapv3Password")}
              />
            )}
          ></Controller>
        </FormGroup>

        <FormGroup
          label={t("validatePasswordPolicy")}
          labelIcon={
            <HelpItem
              helpText={t("validatePasswordPolicyHelp")}
              fieldLabelId="validatePasswordPolicy"
            />
          }
          fieldId="kc-validate-password-policy"
          hasNoPaddingTop
        >
          <Controller
            name="config.validatePasswordPolicy"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-validate-password-policy"}
                data-testid="password-policy"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("validatePasswordPolicy")}
              />
            )}
          ></Controller>
        </FormGroup>

        <FormGroup
          label={t("trustEmail")}
          labelIcon={
            <HelpItem
              helpText={t("trustEmailHelp")}
              fieldLabelId="trustEmail"
            />
          }
          fieldId="kc-trust-email"
          hasNoPaddingTop
        >
          <Controller
            name="config.trustEmail"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-trust-email"}
                data-testid="trust-email"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("trustEmail")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("connectionTrace")}
          labelIcon={
            <HelpItem
              helpText={t("connectionTraceHelp")}
              fieldLabelId="connectionTrace"
            />
          }
          fieldId="kc-connection-trace"
          hasNoPaddingTop
        >
          <Controller
            name="config.connectionTrace"
            defaultValue={["false"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id={"kc-connection-trace"}
                data-testid="connection-trace"
                isDisabled={false}
                onChange={(_event, value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("on")}
                labelOff={t("off")}
                aria-label={t("connectionTrace")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup fieldId="query-extensions">
          <Button
            variant="secondary"
            id="query-extensions"
            data-testid="query-extensions"
            onClick={testLdap}
          >
            {t("queryExtensions")}
          </Button>
        </FormGroup>
      </FormAccess>
    </>
  );
};
