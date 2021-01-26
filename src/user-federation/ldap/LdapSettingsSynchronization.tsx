import { FormGroup, Switch, TextInput } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { UseFormMethods, Controller } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type LdapSettingsSynchronizationProps = {
  form: UseFormMethods;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const LdapSettingsSynchronization = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsSynchronizationProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("synchronizationSettings")}
          description={helpText("ldapSynchronizationSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          hasNoPaddingTop
          label={t("importUsers")}
          labelIcon={
            <HelpItem
              helpText={helpText("importUsersHelp")}
              forLabel={t("importUsers")}
              forID="kc-import-users"
            />
          }
          fieldId="kc-import-users"
        >
          <Controller
            name="config.importEnabled"
            defaultValue={false}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-import-users"}
                name="importEnabled"
                label={t("common:on")}
                labelOff={t("common:off")}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                isDisabled={false}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("batchSize")}
          labelIcon={
            <HelpItem
              helpText={helpText("batchSizeHelp")}
              forLabel={t("batchSize")}
              forID="kc-batch-size"
            />
          }
          fieldId="kc-batch-size"
        >
          <TextInput
            type="text"
            id="kc-batch-size"
            name="config.batchSizeForSync[0]"
            ref={form.register}
          />
        </FormGroup>

        {/* Enter -1 to switch off, otherwise enter value */}
        <FormGroup
          hasNoPaddingTop
          label={t("fullSyncPeriod")}
          labelIcon={
            <HelpItem
              helpText={helpText("fullSyncPeriodHelp")}
              forLabel={t("fullSyncPeriod")}
              forID="kc-full-sync-period"
            />
          }
          fieldId="kc-full-sync-period"
        >
          <TextInput
            type="text"
            id="kc-full-sync-period"
            name="config.fullSyncPeriod[0]"
            ref={form.register}
          />
        </FormGroup>

        {/* Enter -1 to switch off, otherwise enter value */}
        <FormGroup
          label={t("changedUsersSyncPeriod")}
          labelIcon={
            <HelpItem
              helpText={helpText("changedUsersSyncHelp")}
              forLabel={t("changedUsersSyncPeriod")}
              forID="kc-changed-users-sync-period"
            />
          }
          fieldId="kc-changed-users-sync-period"
          hasNoPaddingTop
        >
          <TextInput
            type="text"
            id="kc-changed-users-sync-period"
            name="config.changedSyncPeriod[0]"
            ref={form.register}
          />
        </FormGroup>
      </FormAccess>
    </>
  );
};
