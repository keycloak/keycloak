import React, { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, FormProvider, useForm } from "react-hook-form";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  DropdownSeparator,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";

import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { toUpperCase } from "../util";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { RealmSettingsLoginTab } from "./LoginTab";
import { RealmSettingsGeneralTab } from "./GeneralTab";
import { PartialImportDialog } from "./PartialImport";
import { RealmSettingsThemesTab } from "./ThemesTab";
import { RealmSettingsEmailTab } from "./EmailTab";
import { KeysListTab } from "./KeysListTab";
import { EventsTab } from "./event-config/EventsTab";
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { KeysProviderTab } from "./KeysProvidersTab";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";

type RealmSettingsHeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean;
  save: () => void;
  realmName: string;
};

const RealmSettingsHeader = ({
  save,
  onChange,
  value,
  realmName,
}: RealmSettingsHeaderProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { refresh } = useRealm();
  const [partialImportOpen, setPartialImportOpen] = useState(false);

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "realm-settings:disableConfirmTitle",
    messageKey: "realm-settings:disableConfirm",
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange(!value);
      save();
    },
  });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "realm-settings:deleteConfirmTitle",
    messageKey: "realm-settings:deleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.realms.del({ realm: realmName });
        addAlert(t("deletedSuccess"), AlertVariant.success);
        history.push("/master/");
        refresh();
      } catch (error) {
        addAlert(t("deleteError", { error }), AlertVariant.danger);
      }
    },
  });

  return (
    <>
      <DisableConfirm />
      <DeleteConfirm />
      <PartialImportDialog
        open={partialImportOpen}
        toggleDialog={() => setPartialImportOpen(!partialImportOpen)}
      />
      <ViewHeader
        titleKey={toUpperCase(realmName)}
        divider={false}
        dropdownItems={[
          <DropdownItem
            key="import"
            data-testid="openPartialImportModal"
            onClick={() => {
              setPartialImportOpen(true);
            }}
          >
            {t("partialImport")}
          </DropdownItem>,
          <DropdownItem key="export" onClick={() => {}}>
            {t("partialExport")}
          </DropdownItem>,
          <DropdownSeparator key="separator" />,
          <DropdownItem key="delete" onClick={toggleDeleteDialog}>
            {t("common:delete")}
          </DropdownItem>,
        ]}
        isEnabled={value}
        onToggle={(value) => {
          if (!value) {
            toggleDisableDialog();
          } else {
            onChange(value);
            save();
          }
        }}
      />
    </>
  );
};

export const RealmSettingsSection = () => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert } = useAlerts();
  const form = useForm();
  const { control, getValues, setValue, reset: resetForm } = form;
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [activeTab, setActiveTab] = useState(0);
  const [key, setKey] = useState(0);
  const [realmComponents, setRealmComponents] = useState<
    ComponentRepresentation[]
  >();

  const kpComponentTypes = useServerInfo().componentTypes![
    "org.keycloak.keys.KeyProvider"
  ];

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      setupForm(realm);
      setRealm(realm);
    },
    []
  );

  useEffect(() => {
    const update = async () => {
      const realmComponents = await adminClient.components.find({
        type: "org.keycloak.keys.KeyProvider",
        realm: realmName,
      });
      setRealmComponents(realmComponents);
    };
    setTimeout(update, 100);
  }, [key]);

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      const realmComponents = await adminClient.components.find({
        type: "org.keycloak.keys.KeyProvider",
        realm: realmName,
      });

      return { realm, realmComponents };
    },
    (result) => {
      setRealm(result.realm);
      setRealmComponents(result.realmComponents);
    },
    []
  );

  const refresh = () => {
    setKey(new Date().getTime());
  };

  useEffect(() => {
    const update = async () => {
      const realmComponents = await adminClient.components.find({
        type: "org.keycloak.keys.KeyProvider",
        realm: realmName,
      });
      setRealmComponents(realmComponents);
    };
    setTimeout(update, 100);
  }, [key]);

  useEffect(() => {
    if (realm) setupForm(realm);
  }, [realm]);

  const setupForm = (realm: RealmRepresentation) => {
    resetForm(realm);
    Object.entries(realm).map((entry) => setValue(entry[0], entry[1]));
  };

  const save = async (realm: RealmRepresentation) => {
    try {
      await adminClient.realms.update({ realm: realmName }, realm);
      setRealm(realm);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("saveError", { error: error.response?.data?.errorMessage || error }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <Controller
        name="enabled"
        control={control}
        defaultValue={true}
        render={({ onChange, value }) => (
          <RealmSettingsHeader
            value={value}
            onChange={onChange}
            realmName={realmName}
            save={() => save(getValues())}
          />
        )}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...form}>
          <KeycloakTabs isBox>
            <Tab
              eventKey="general"
              title={<TabTitleText>{t("general")}</TabTitleText>}
              data-testid="rs-general-tab"
            >
              <RealmSettingsGeneralTab
                save={save}
                reset={() => setupForm(realm!)}
              />
            </Tab>
            <Tab
              eventKey="login"
              title={<TabTitleText>{t("login")}</TabTitleText>}
              data-testid="rs-login-tab"
            >
              <RealmSettingsLoginTab save={save} realm={realm!} />
            </Tab>
            <Tab
              eventKey="email"
              title={<TabTitleText>{t("email")}</TabTitleText>}
              data-testid="rs-email-tab"
            >
              {realm && <RealmSettingsEmailTab realm={realm} />}
            </Tab>
            <Tab
              eventKey="themes"
              title={<TabTitleText>{t("themes")}</TabTitleText>}
              data-testid="rs-themes-tab"
            >
              <RealmSettingsThemesTab
                save={save}
                reset={() => setupForm(realm!)}
              />
            </Tab>
            <Tab
              eventKey="keys"
              title={<TabTitleText>{t("realm-settings:keys")}</TabTitleText>}
              data-testid="rs-keys-tab"
            >
              {realmComponents && (
                <Tabs
                  activeKey={activeTab}
                  onSelect={(_, key) => setActiveTab(key as number)}
                >
                  <Tab
                    id="keysList"
                    eventKey={0}
                    data-testid="rs-keys-list-tab"
                    title={<TabTitleText>{t("keysList")}</TabTitleText>}
                  >
                    <KeysListTab realmComponents={realmComponents} />
                  </Tab>
                  <Tab
                    id="providers"
                    data-testid="rs-providers-tab"
                    eventKey={1}
                    title={<TabTitleText>{t("providers")}</TabTitleText>}
                  >
                    <KeysProviderTab
                      realmComponents={realmComponents}
                      keyProviderComponentTypes={kpComponentTypes}
                      refresh={refresh}
                    />
                  </Tab>
                </Tabs>
              )}
            </Tab>
            <Tab
              eventKey="events"
              title={<TabTitleText>{t("events")}</TabTitleText>}
              data-testid="rs-realm-events-tab"
            >
              <EventsTab />
            </Tab>
          </KeycloakTabs>
        </FormProvider>
      </PageSection>
    </>
  );
};
