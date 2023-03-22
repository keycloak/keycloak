import { Button, FormGroup, Switch } from "@patternfly/react-core";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
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
  const { t } = useTranslation("user-federation");
  const { t: helpText } = useTranslation("user-federation-help");

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const testLdap = async () => {
    if (!(await form.trigger())) return;
    try {
      const settings = convertFormToSettings(form);
      const ldapOids = await adminClient.realms.ldapServerCapabilities(
        { realm },
        { ...settings, componentId: id }
      );
      addAlert(t("testSuccess"));
      const passwordModifyOid = ldapOids.filter(
        (id: { oid: string }) => id.oid === PASSWORD_MODIFY_OID
      );
      form.setValue("config.usePasswordModifyExtendedOp", [
        (passwordModifyOid.length > 0).toString(),
      ]);
    } catch (error) {
      addError("user-federation:testError", error);
    }
  };

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("advancedSettings")}
          description={helpText("ldapAdvancedSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}

      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("enableLdapv3Password")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:enableLdapv3PasswordHelp")}
              fieldLabelId="user-federation:enableLdapv3Password"
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
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("enableLdapv3Password")}
              />
            )}
          ></Controller>
        </FormGroup>

        <FormGroup
          label={t("validatePasswordPolicy")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:validatePasswordPolicyHelp")}
              fieldLabelId="user-federation:validatePasswordPolicy"
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
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("validatePasswordPolicy")}
              />
            )}
          ></Controller>
        </FormGroup>

        <FormGroup
          label={t("trustEmail")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:trustEmailHelp")}
              fieldLabelId="user-federation:trustEmail"
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
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("trustEmail")}
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
