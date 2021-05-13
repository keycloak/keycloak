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

  const watchPeriodicSync = form.watch("config.periodicFullSync", false);
  const watchChangedSync = form.watch("config.periodicChangedUsersSync", false);
  
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
            defaultValue={["false"]}
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
            type="number"
            min={0}
            id="kc-batch-size"
            name="config.batchSizeForSync[0]"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label={"Periodic full sync"}
          labelIcon={
            <HelpItem
              helpText={helpText("periodicFullSyncHelp")}
              forLabel={"periodicFullSync"}
              forID="kc-periodic-full-sync"
            />
          }
          fieldId="kc-periodic-full-sync"
          hasNoPaddingTop
        >
          <Controller
            name="config.periodicFullSync"
            defaultValue={false}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-periodic-full-sync"}
                isDisabled={false}
                onChange={(value) => onChange(value)}
                isChecked={value === true}
                label={t("common:on")}
                labelOff={t("common:off")}
                ref={form.register}
              />
            )}
          ></Controller>
        </FormGroup>
        {watchPeriodicSync && (
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
              type="number"
              min={-1}
              defaultValue={604800}
              id="kc-full-sync-period"
              name="config.fullSyncPeriod[0]"
              ref={form.register}
            />
          </FormGroup>
        )}
        <FormGroup
          label={"Periodic Changed Users Sync"}
          labelIcon={
            <HelpItem
              helpText={helpText("periodicChangedUsersSyncHelp")}
              forLabel={"periodicChangedUsersSync"}
              forID="kc-periodic-changed-users-sync"
            />
          }
          fieldId="kc-periodic-changed-users-sync"
          hasNoPaddingTop
        >
          <Controller
            name="config.periodicChangedUsersSync"
            defaultValue={false}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-periodic-changed-users-sync"}
                isDisabled={false}
                onChange={(value) => onChange(value)}
                isChecked={value === true}
                label={t("common:on")}
                labelOff={t("common:off")}
                ref={form.register}
              />
            )}
          ></Controller>
        </FormGroup>
        {watchChangedSync && (
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
              type="number"
              min={-1}
              defaultValue={86400}
              id="kc-changed-users-sync-period"
              name="config.changedSyncPeriod[0]"
              ref={form.register}
            />
          </FormGroup>
        )}
      </FormAccess>
    </>
  );
};
