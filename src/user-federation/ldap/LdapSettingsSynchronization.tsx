import { FormGroup, Switch, TextInput } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useEffect } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import { convertToFormValues } from "../../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useParams } from "react-router-dom";

export const LdapSettingsSynchronization = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;
  const adminClient = useAdminClient();
  const { register, control, setValue } = useForm<ComponentRepresentation>();
  const { id } = useParams<{ id: string }>();

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", setValue);
      } else {
        setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  return (
    <>
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
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-import-users"}
                name="importEnabled"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value[0] === "true"}
                isDisabled={false}
                onChange={onChange}
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
            name="config.batchSizeForSync"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          hasNoPaddingTop
          label={t("periodicFullSync")}
          labelIcon={
            <HelpItem
              helpText={helpText("periodicFullSyncHelp")}
              forLabel={t("periodicFullSync")}
              forID="kc-periodic-full-sync"
            />
          }
          fieldId="kc-periodic-full-sync"
        >
          <TextInput
            type="text"
            id="kc-batch-size"
            name="config.fullSyncPeriod"
            ref={register}
          />
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
          <TextInput
            type="text"
            id="kc-batch-size"
            name="config.changedSyncPeriod"
            ref={register}
          />
        </FormGroup>
      </FormAccess>
    </>
  );
};
