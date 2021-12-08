import React, { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  DropdownSeparator,
  Form,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";

import { LdapSettingsAdvanced } from "./ldap/LdapSettingsAdvanced";
import { LdapSettingsKerberosIntegration } from "./ldap/LdapSettingsKerberosIntegration";
import { SettingsCache } from "./shared/SettingsCache";
import { LdapSettingsSynchronization } from "./ldap/LdapSettingsSynchronization";
import { LdapSettingsGeneral } from "./ldap/LdapSettingsGeneral";
import { LdapSettingsConnection } from "./ldap/LdapSettingsConnection";
import { LdapSettingsSearching } from "./ldap/LdapSettingsSearching";

import { useRealm } from "../context/realm-context/RealmContext";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";

import { Controller, useForm } from "react-hook-form";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useTranslation } from "react-i18next";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useHistory, useParams } from "react-router-dom";
import { ScrollForm } from "../components/scroll-form/ScrollForm";

import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { LdapMapperList } from "./ldap/mappers/LdapMapperList";

type ldapComponentRepresentation = ComponentRepresentation & {
  config?: {
    periodicChangedUsersSync?: boolean;
    periodicFullSync?: boolean;
  };
};

type LdapSettingsHeaderProps = {
  onChange: (value: string) => void;
  value: string;
  editMode?: string | string[];
  save: () => void;
  toggleDeleteDialog: () => void;
  toggleRemoveUsersDialog: () => void;
};

const LdapSettingsHeader = ({
  onChange,
  value,
  editMode,
  save,
  toggleDeleteDialog,
  toggleRemoveUsersDialog,
}: LdapSettingsHeaderProps) => {
  const { t } = useTranslation("user-federation");
  const { id } = useParams<{ id: string }>();
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "user-federation:userFedDisableConfirmTitle",
    messageKey: "user-federation:userFedDisableConfirm",
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange("false");
      save();
    },
  });

  const [toggleUnlinkUsersDialog, UnlinkUsersDialog] = useConfirmDialog({
    titleKey: "user-federation:userFedUnlinkUsersConfirmTitle",
    messageKey: "user-federation:userFedUnlinkUsersConfirm",
    continueButtonLabel: "user-federation:unlinkUsers",
    onConfirm: () => unlinkUsers(),
  });

  const syncChangedUsers = async () => {
    try {
      if (id) {
        const response = await adminClient.userStorageProvider.sync({
          id: id,
          action: "triggerChangedUsersSync",
        });
        if (response.ignored) {
          addAlert(`${response.status}.`, AlertVariant.warning);
        } else {
          addAlert(
            t("syncUsersSuccess") +
              `${response.added} users added, ${response.updated} users updated, ${response.removed} users removed, ${response.failed} users failed.`,
            AlertVariant.success
          );
        }
      }
    } catch (error) {
      addError("user-federation:syncUsersError", error);
    }
  };

  const syncAllUsers = async () => {
    try {
      if (id) {
        const response = await adminClient.userStorageProvider.sync({
          id: id,
          action: "triggerFullSync",
        });
        if (response.ignored) {
          addAlert(`${response.status}.`, AlertVariant.warning);
        } else {
          addAlert(
            t("syncUsersSuccess") +
              `${response.added} users added, ${response.updated} users updated, ${response.removed} users removed, ${response.failed} users failed.`,
            AlertVariant.success
          );
        }
      }
    } catch (error) {
      addError("user-federation:syncUsersError", error);
    }
  };

  const unlinkUsers = async () => {
    try {
      if (id) {
        await adminClient.userStorageProvider.unlinkUsers({ id });
      }
      addAlert(t("unlinkUsersSuccess"), AlertVariant.success);
    } catch (error) {
      addError("user-federation:unlinkUsersError", error);
    }
  };

  return (
    <>
      <DisableConfirm />
      <UnlinkUsersDialog />
      {!id ? (
        <ViewHeader titleKey={t("addOneLdap")} />
      ) : (
        <ViewHeader
          titleKey="LDAP"
          dropdownItems={[
            <DropdownItem key="sync" onClick={syncChangedUsers}>
              {t("syncChangedUsers")}
            </DropdownItem>,
            <DropdownItem key="syncall" onClick={syncAllUsers}>
              {t("syncAllUsers")}
            </DropdownItem>,
            <DropdownItem
              key="unlink"
              isDisabled={editMode ? !editMode.includes("UNSYNCED") : false}
              onClick={toggleUnlinkUsersDialog}
            >
              {t("unlinkUsers")}
            </DropdownItem>,
            <DropdownItem key="remove" onClick={toggleRemoveUsersDialog}>
              {t("removeImported")}
            </DropdownItem>,
            <DropdownSeparator key="separator" />,
            <DropdownItem
              key="delete"
              onClick={toggleDeleteDialog}
              data-testid="delete-ldap-cmd"
            >
              {t("deleteProvider")}
            </DropdownItem>,
          ]}
          isEnabled={value === "true"}
          onToggle={(value) => {
            if (!value) {
              toggleDisableDialog();
            } else {
              onChange("" + value);
              save();
            }
          }}
        />
      )}
    </>
  );
};

export default function UserFederationLdapSettings() {
  const { t } = useTranslation("user-federation");
  const form = useForm<ComponentRepresentation>({ mode: "onChange" });
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const { id } = useParams<{ id: string }>();
  const { addAlert, addError } = useAlerts();
  const [component, setComponent] = useState<ComponentRepresentation>();
  const [refreshCount, setRefreshCount] = useState(0);

  const editMode = component?.config?.editMode;
  const refresh = () => setRefreshCount((count) => count + 1);

  useFetch(
    async () => {
      if (id) {
        return await adminClient.components.findOne({ id });
      }
      return undefined;
    },
    (fetchedComponent) => {
      if (fetchedComponent) {
        setupForm(fetchedComponent);
        setComponent(fetchedComponent);
      } else if (id) {
        throw new Error(t("common:notFound"));
      }
    },
    [refreshCount]
  );

  const setupForm = (component: ComponentRepresentation) => {
    form.reset({ ...component });
    form.setValue(
      "config.periodicChangedUsersSync",
      component.config?.["changedSyncPeriod"][0] !== "-1"
    );

    form.setValue(
      "config.periodicFullSync",
      component.config?.["fullSyncPeriod"][0] !== "-1"
    );
  };

  const removeImportedUsers = async () => {
    try {
      if (id) {
        await adminClient.userStorageProvider.removeImportedUsers({ id });
      }
      addAlert(t("removeImportedUsersSuccess"), AlertVariant.success);
    } catch (error) {
      addError("user-federation:removeImportedUsersError", error);
    }
  };

  const save = async (component: ldapComponentRepresentation) => {
    if (component.config?.periodicChangedUsersSync !== null) {
      if (component.config?.periodicChangedUsersSync === false) {
        component.config.changedSyncPeriod = ["-1"];
      }
      delete component.config?.periodicChangedUsersSync;
    }
    if (component.config?.periodicFullSync !== null) {
      if (component.config?.periodicFullSync === false) {
        component.config.fullSyncPeriod = ["-1"];
      }
      delete component.config?.periodicFullSync;
    }
    try {
      if (!id) {
        await adminClient.components.create(component);
        history.push(`/${realm}/user-federation`);
      } else {
        await adminClient.components.update({ id }, component);
      }
      addAlert(t(id ? "saveSuccess" : "createSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError(`user-federation:${id ? "saveError" : "createError"}`, error);
    }
  };

  const [toggleRemoveUsersDialog, RemoveUsersConfirm] = useConfirmDialog({
    titleKey: t("removeImportedUsers"),
    messageKey: t("removeImportedUsersMessage"),
    continueButtonLabel: "common:remove",
    onConfirm: async () => {
      try {
        removeImportedUsers();
        addAlert(t("removeImportedUsersSuccess"), AlertVariant.success);
      } catch (error) {
        addError("user-federation:removeImportedUsersError", error);
      }
    },
  });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "user-federation:userFedDeleteConfirmTitle",
    messageKey: "user-federation:userFedDeleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id });
        addAlert(t("userFedDeletedSuccess"), AlertVariant.success);
        history.replace(`/${realm}/user-federation`);
      } catch (error) {
        addError("user-federation:userFedDeleteError", error);
      }
    },
  });

  const addLdapFormContent = () => {
    return (
      <>
        <ScrollForm
          sections={[
            t("generalOptions"),
            t("connectionAndAuthenticationSettings"),
            t("ldapSearchingAndUpdatingSettings"),
            t("synchronizationSettings"),
            t("kerberosIntegration"),
            t("cacheSettings"),
            t("advancedSettings"),
          ]}
        >
          <LdapSettingsGeneral form={form} vendorEdit={!!id} />
          <LdapSettingsConnection form={form} edit={!!id} />
          <LdapSettingsSearching form={form} />
          <LdapSettingsSynchronization form={form} />
          <LdapSettingsKerberosIntegration form={form} />
          <SettingsCache form={form} />
          <LdapSettingsAdvanced form={form} />
        </ScrollForm>
        <Form onSubmit={form.handleSubmit(save)}>
          <ActionGroup className="keycloak__form_actions">
            <Button
              isDisabled={!form.formState.isDirty}
              variant="primary"
              type="submit"
              data-testid="ldap-save"
            >
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              onClick={() => history.push(`/${realm}/user-federation`)}
              data-testid="ldap-cancel"
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </>
    );
  };

  return (
    <>
      <DeleteConfirm />
      <RemoveUsersConfirm />
      <Controller
        name="config.enabled[0]"
        defaultValue={["true"][0]}
        control={form.control}
        render={({ onChange, value }) => (
          <LdapSettingsHeader
            editMode={editMode}
            value={value}
            save={() => save(form.getValues())}
            onChange={onChange}
            toggleDeleteDialog={toggleDeleteDialog}
            toggleRemoveUsersDialog={toggleRemoveUsersDialog}
          />
        )}
      />
      <PageSection variant="light" isFilled>
        {id ? (
          <KeycloakTabs isBox>
            <Tab
              id="settings"
              eventKey="settings"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            >
              {addLdapFormContent()}
            </Tab>
            <Tab
              id="mappers"
              eventKey="mappers"
              title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
              data-testid="ldap-mappers-tab"
            >
              <LdapMapperList />
            </Tab>
          </KeycloakTabs>
        ) : (
          addLdapFormContent()
        )}
      </PageSection>
    </>
  );
}
