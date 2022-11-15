import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, Switch } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const SyncSettings = () => {
  const { t } = useTranslation("user-federation");
  const { control, register, watch } = useFormContext();
  const watchPeriodicSync = watch("config.fullSyncPeriod", "-1");
  const watchChangedSync = watch("config.changedSyncPeriod", "-1");

  return (
    <>
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
          name="config.fullSyncPeriod"
          defaultValue="-1"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="kc-periodic-full-sync"
              data-testid="periodic-full-sync"
              onChange={(value) => {
                onChange(value ? "604800" : "-1");
              }}
              isChecked={value !== "-1"}
              label={t("common:on")}
              labelOff={t("common:off")}
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
              helpText="user-federation-help:fullSyncPeriodHelp"
              fieldLabelId="user-federation:fullSyncPeriod"
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
            name="config.fullSyncPeriod"
            ref={register}
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
          name="config.changedSyncPeriod"
          defaultValue="-1"
          control={control}
          render={({ onChange, value }) => (
            <Switch
              id="kc-periodic-changed-users-sync"
              data-testid="periodic-changed-users-sync"
              onChange={(value) => {
                onChange(value ? "86400" : "-1");
              }}
              isChecked={value !== "-1"}
              label={t("common:on")}
              labelOff={t("common:off")}
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
            defaultValue="86400"
            id="kc-changed-users-sync-period"
            data-testid="changed-users-sync-period"
            name="config.changedSyncPeriod"
            ref={register}
          />
        </FormGroup>
      )}
    </>
  );
};
