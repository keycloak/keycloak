import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type LdapSettingsSynchronizationProps = {
  form: UseFormReturn;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const LdapSettingsSynchronization = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsSynchronizationProps) => {
  const { t } = useTranslation("user-federation");
  const { t: helpText } = useTranslation("user-federation-help");

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
              helpText={t("user-federation-help:importUsersHelp")}
              fieldLabelId="user-federation:importUsers"
            />
          }
          fieldId="kc-import-users"
        >
          <Controller
            name="config.importEnabled"
            defaultValue={["true"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-import-users"
                data-testid="import-users"
                name="importEnabled"
                label={t("common:on")}
                labelOff={t("common:off")}
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                isDisabled={false}
                aria-label={t("importUsers")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          hasNoPaddingTop
          label={t("syncRegistrations")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:syncRegistrations")}
              fieldLabelId="user-federation:syncRegistrations"
            />
          }
          fieldId="syncRegistrations"
        >
          <Controller
            name="config.syncRegistrations"
            defaultValue={["true"]}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="syncRegistrations"
                data-testid="syncRegistrations"
                label={t("common:on")}
                labelOff={t("common:off")}
                onChange={(value) => field.onChange([`${value}`])}
                isChecked={field.value[0] === "true"}
                aria-label={t("syncRegistrations")}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("batchSize")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:batchSizeHelp")}
              fieldLabelId="user-federation:batchSize"
            />
          }
          fieldId="kc-batch-size"
        >
          <KeycloakTextInput
            type="number"
            min={0}
            id="kc-batch-size"
            data-testid="batch-size"
            {...form.register("config.batchSizeForSync.0")}
          />
        </FormGroup>
        <FormGroup
          label={t("periodicFullSync")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:periodicFullSyncHelp")}
              fieldLabelId="user-federation:periodicFullSync"
            />
          }
          fieldId="kc-periodic-full-sync"
          hasNoPaddingTop
        >
          <Controller
            name="config.periodicFullSync"
            defaultValue={false}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-periodic-full-sync"
                data-testid="periodic-full-sync"
                isDisabled={false}
                onChange={(value) => field.onChange(value)}
                isChecked={field.value === true}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("periodicFullSync")}
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
                helpText={t("user-federation-help:fullSyncPeriodHelp")}
                fieldLabelId="user-federation:fullSyncPeriod"
              />
            }
            fieldId="kc-full-sync-period"
          >
            <KeycloakTextInput
              type="number"
              min={-1}
              defaultValue={604800}
              id="kc-full-sync-period"
              data-testid="full-sync-period"
              {...form.register("config.fullSyncPeriod.0")}
            />
          </FormGroup>
        )}
        <FormGroup
          label={t("periodicChangedUsersSync")}
          labelIcon={
            <HelpItem
              helpText={t("user-federation-help:periodicChangedUsersSyncHelp")}
              fieldLabelId="user-federation:periodicChangedUsersSync"
            />
          }
          fieldId="kc-periodic-changed-users-sync"
          hasNoPaddingTop
        >
          <Controller
            name="config.periodicChangedUsersSync"
            defaultValue={false}
            control={form.control}
            render={({ field }) => (
              <Switch
                id="kc-periodic-changed-users-sync"
                data-testid="periodic-changed-users-sync"
                isDisabled={false}
                onChange={(value) => field.onChange(value)}
                isChecked={field.value === true}
                label={t("common:on")}
                labelOff={t("common:off")}
                aria-label={t("periodicChangedUsersSync")}
              />
            )}
          ></Controller>
        </FormGroup>
        {watchChangedSync && (
          <FormGroup
            label={t("changedUsersSyncPeriod")}
            labelIcon={
              <HelpItem
                helpText={t("user-federation-help:changedUsersSyncHelp")}
                fieldLabelId="user-federation:changedUsersSyncPeriod"
              />
            }
            fieldId="kc-changed-users-sync-period"
            hasNoPaddingTop
          >
            <KeycloakTextInput
              type="number"
              min={-1}
              defaultValue={86400}
              id="kc-changed-users-sync-period"
              data-testid="changed-users-sync-period"
              {...form.register("config.changedSyncPeriod.0")}
            />
          </FormGroup>
        )}
      </FormAccess>
    </>
  );
};
