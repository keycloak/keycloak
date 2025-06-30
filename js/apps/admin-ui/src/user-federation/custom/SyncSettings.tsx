import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, FormProvider, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";

export const SyncSettings = () => {
  const { t } = useTranslation();
  const form = useFormContext();
  const { control, watch } = form;
  const watchPeriodicSync = watch("config.fullSyncPeriod", "-1");
  const watchChangedSync = watch("config.changedSyncPeriod", "-1");

  return (
    <FormProvider {...form}>
      <FormGroup
        label={t("periodicFullSync")}
        labelIcon={
          <HelpItem
            helpText={t("periodicFullSyncHelp")}
            fieldLabelId="periodicFullSync"
          />
        }
        fieldId="kc-periodic-full-sync"
        hasNoPaddingTop
      >
        <Controller
          name="config.fullSyncPeriod"
          defaultValue="-1"
          control={control}
          render={({ field }) => (
            <Switch
              id="kc-periodic-full-sync"
              data-testid="periodic-full-sync"
              onChange={(_event, value) => {
                field.onChange(value ? "604800" : "-1");
              }}
              isChecked={field.value !== "-1"}
              label={t("on")}
              labelOff={t("off")}
              aria-label={t("periodicFullSync")}
            />
          )}
        />
      </FormGroup>
      {watchPeriodicSync !== "-1" && (
        <TextControl
          name="config.fullSyncPeriod"
          label={t("fullSyncPeriod")}
          labelIcon={t("fullSyncPeriodHelp")}
          type="number"
          min={-1}
          defaultValue="604800"
        />
      )}
      <FormGroup
        label={t("periodicChangedUsersSync")}
        labelIcon={
          <HelpItem
            helpText={t("periodicChangedUsersSyncHelp")}
            fieldLabelId="periodicChangedUsersSync"
          />
        }
        fieldId="kc-periodic-changed-users-sync"
        hasNoPaddingTop
      >
        <Controller
          name="config.changedSyncPeriod"
          defaultValue="-1"
          control={control}
          render={({ field }) => (
            <Switch
              id="kc-periodic-changed-users-sync"
              data-testid="periodic-changed-users-sync"
              onChange={(_event, value) => {
                field.onChange(value ? "86400" : "-1");
              }}
              isChecked={field.value !== "-1"}
              label={t("on")}
              labelOff={t("off")}
              aria-label={t("periodicChangedUsersSync")}
            />
          )}
        />
      </FormGroup>
      {watchChangedSync !== "-1" && (
        <TextControl
          name="config.changedSyncPeriod"
          label={t("changedUsersSyncPeriod")}
          labelIcon={t("changedUsersSyncHelp")}
          type="number"
          min={-1}
          defaultValue="86400"
        />
      )}
    </FormProvider>
  );
};
