import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const SyncSettings = () => {
  const { t } = useTranslation();
  const { control, register, watch } = useFormContext();
  const watchPeriodicSync = watch("config.fullSyncPeriod", "-1");
  const watchChangedSync = watch("config.changedSyncPeriod", "-1");

  return (
    <>
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
              onChange={(value) => {
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
        <FormGroup
          hasNoPaddingTop
          label={t("fullSyncPeriod")}
          labelIcon={
            <HelpItem
              helpText={t("fullSyncPeriodHelp")}
              fieldLabelId="fullSyncPeriod"
            />
          }
          fieldId="kc-full-sync-period"
        >
          <KeycloakTextInput
            type="number"
            min={-1}
            defaultValue="604800"
            id="kc-full-sync-period"
            data-testid="full-sync-period"
            {...register("config.fullSyncPeriod")}
          />
        </FormGroup>
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
              onChange={(value) => {
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
        <FormGroup
          label={t("changedUsersSyncPeriod")}
          labelIcon={
            <HelpItem
              helpText={t("changedUsersSyncHelp")}
              fieldLabelId="changedUsersSyncPeriod"
            />
          }
          fieldId="kc-changed-users-sync-period"
          hasNoPaddingTop
        >
          <KeycloakTextInput
            type="number"
            min={-1}
            defaultValue="86400"
            id="kc-changed-users-sync-period"
            data-testid="changed-users-sync-period"
            {...register("config.changedSyncPeriod")}
          />
        </FormGroup>
      )}
    </>
  );
};
