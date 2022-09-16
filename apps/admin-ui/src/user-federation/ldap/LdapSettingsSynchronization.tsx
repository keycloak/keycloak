import { FormGroup, Switch } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { UseFormMethods, Controller } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

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
              helpText="user-federation-help:importUsersHelp"
              fieldLabelId="user-federation:importUsers"
            />
          }
          fieldId="kc-import-users"
        >
          <Controller
            name="config.importEnabled"
            defaultValue={["true"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id="kc-import-users"
                data-testid="import-users"
                name="importEnabled"
                label={t("common:on")}
                labelOff={t("common:off")}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
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
              helpText="user-federation-help:syncRegistrations"
              fieldLabelId="user-federation:syncRegistrations"
            />
          }
          fieldId="syncRegistrations"
        >
          <Controller
            name="config.syncRegistrations"
            defaultValue={["true"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id="syncRegistrations"
                data-testid="syncRegistrations"
                label={t("common:on")}
                labelOff={t("common:off")}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                aria-label={t("syncRegistrations")}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("batchSize")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:batchSizeHelp"
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
            name="config.batchSizeForSync[0]"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label={t("periodicFullSync")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:periodicFullSyncHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id="kc-periodic-full-sync"
                data-testid="periodic-full-sync"
                isDisabled={false}
                onChange={(value) => onChange(value)}
                isChecked={value === true}
                label={t("common:on")}
                labelOff={t("common:off")}
                ref={form.register}
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
                helpText="user-federation-help:fullSyncPeriodHelp"
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
              name="config.fullSyncPeriod[0]"
              ref={form.register}
            />
          </FormGroup>
        )}
        <FormGroup
          label={t("periodicChangedUsersSync")}
          labelIcon={
            <HelpItem
              helpText="user-federation-help:periodicChangedUsersSyncHelp"
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
            render={({ onChange, value }) => (
              <Switch
                id="kc-periodic-changed-users-sync"
                data-testid="periodic-changed-users-sync"
                isDisabled={false}
                onChange={(value) => onChange(value)}
                isChecked={value === true}
                label={t("common:on")}
                labelOff={t("common:off")}
                ref={form.register}
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
                helpText="user-federation-help:changedUsersSyncHelp"
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
              name="config.changedSyncPeriod[0]"
              ref={form.register}
            />
          </FormGroup>
        )}
      </FormAccess>
    </>
  );
};
