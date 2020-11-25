import { Form, FormGroup, Switch, TextInput } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

export const LdapSettingsSynchronization = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const { register, handleSubmit, control } = useForm<
    ComponentRepresentation
  >();
  const onSubmit = (data: ComponentRepresentation) => {
    console.log(data);
  };

  return (
    <>
      {/* Synchronization settings */}
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <FormGroup
          label={t("importUsers")}
          labelIcon={
            <HelpItem
              helpText={helpText("importUsersHelp")}
              forLabel={t("importUsers")}
              forID="kc-import-users"
            />
          }
          fieldId="kc-import-users"
          hasNoPaddingTop
        >
          <Controller
            name="importUsers"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-import-users"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                aria-label={t("importUsers") + " " + t("common:on")}
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
            name="batchSize"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("periodicFullSync")}
          labelIcon={
            <HelpItem
              helpText={helpText("periodicFullSyncHelp")}
              forLabel={t("periodicFullSync")}
              forID="kc-periodic-full-sync"
            />
          }
          fieldId="kc-periodic-full-sync"
          hasNoPaddingTop
        >
          <Controller
            name="periodicFullSync"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-periodic-full-sync"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
        <FormGroup
          label={t("periodicChangedUsersSync")}
          labelIcon={
            <HelpItem
              helpText={helpText("periodicChangedUsersSyncHelp")}
              forLabel={t("periodicChangedUsersSync")}
              forID="kc-periodic-changed-users-sync"
            />
          }
          fieldId="kc-periodic-changed-users-sync"
          hasNoPaddingTop
        >
          <Controller
            name="periodicChangedUsersSync"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-periodic-changed-users-sync"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
        <button type="submit">Test submit</button>
      </Form>
    </>
  );
};
