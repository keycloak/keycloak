import React, { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
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

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";

import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  routableTab,
  RoutableTabs,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import { useRealms } from "../context/RealmsContext";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useAlerts } from "../components/alert/Alerts";
import {
  convertFormValuesToObject,
  convertToFormValues,
  KEY_PROVIDER_TYPE,
  toUpperCase,
} from "../util";

import { RealmSettingsEmailTab } from "./EmailTab";
import { EventsTab } from "./event-config/EventsTab";
import { RealmSettingsGeneralTab } from "./GeneralTab";
import { KeysListTab } from "./KeysListTab";
import { KeysProvidersTab } from "./KeysProvidersTab";
import { RealmSettingsLoginTab } from "./LoginTab";
import { SecurityDefences } from "./security-defences/SecurityDefences";
import { RealmSettingsSessionsTab } from "./SessionsTab";
import { RealmSettingsThemesTab } from "./ThemesTab";
import { RealmSettingsTokensTab } from "./TokensTab";
import ProfilesTab from "./ProfilesTab";
import { PoliciesTab } from "./PoliciesTab";
import { PartialImportDialog } from "./PartialImport";
import { PartialExportDialog } from "./PartialExport";
import { toRealmSettings } from "./routes/RealmSettings";
import { LocalizationTab } from "./LocalizationTab";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { UserRegistration } from "./UserRegistration";
import { toDashboard } from "../dashboard/routes/Dashboard";
import environment from "../environment";
import helpUrls from "../help-urls";
import { UserProfileTab } from "./user-profile/UserProfileTab";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";

type RealmSettingsHeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean;
  save: () => void;
  realmName: string;
  refresh: () => void;
};

const RealmSettingsHeader = ({
  save,
  onChange,
  value,
  realmName,
  refresh,
}: RealmSettingsHeaderProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { refresh: refreshRealms } = useRealms();
  const { addAlert, addError } = useAlerts();
  const history = useHistory();
  const [partialImportOpen, setPartialImportOpen] = useState(false);
  const [partialExportOpen, setPartialExportOpen] = useState(false);

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
        await refreshRealms();
        history.push(toDashboard({ realm: environment.masterRealm }));
        refresh();
      } catch (error) {
        addError("realm-settings:deleteError", error);
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
      <PartialExportDialog
        isOpen={partialExportOpen}
        onClose={() => setPartialExportOpen(false)}
      />
      <ViewHeader
        titleKey={toUpperCase(realmName)}
        subKey="realm-settings:realmSettingsExplain"
        helpUrl={helpUrls.realmSettingsUrl}
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
          <DropdownItem
            key="export"
            data-testid="openPartialExportModal"
            onClick={() => setPartialExportOpen(true)}
          >
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

type RealmSettingsTabsProps = {
  realm: RealmRepresentation;
  refresh: () => void;
  realmComponents: ComponentRepresentation[];
};

export const RealmSettingsTabs = ({
  realm,
  realmComponents,
  refresh,
}: RealmSettingsTabsProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const { refresh: refreshRealms } = useRealms();
  const history = useHistory();
  const isFeatureEnabled = useIsFeatureEnabled();

  const kpComponentTypes =
    useServerInfo().componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const form = useForm({ mode: "onChange", shouldUnregister: false });
  const { control, getValues, setValue, reset: resetForm } = form;

  const [activeTab, setActiveTab] = useState(0);
  const [key, setKey] = useState(0);

  const refreshHeader = () => {
    setKey(new Date().getTime());
  };

  const setupForm = (r: RealmRepresentation = realm) => {
    convertToFormValues(r, setValue);
    resetForm(getValues());
  };

  useEffect(() => {
    setupForm();
  }, []);

  const save = async (realm: RealmRepresentation) => {
    try {
      realm = convertFormValuesToObject(realm);

      await adminClient.realms.update(
        { realm: realmName },
        {
          ...realm,
          id: realmName,
        }
      );
      setupForm(realm);
      const isRealmRenamed = realmName !== realm.realm;
      if (isRealmRenamed) {
        await refreshRealms();
        history.push(toRealmSettings({ realm: realm.realm! }));
      }
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }
  };

  const userProfileEnabled = useWatch({
    control,
    name: "attributes.userProfileEnabled",
    defaultValue: "false",
  });

  return (
    <>
      <Controller
        name="enabled"
        defaultValue={true}
        control={control}
        render={({ onChange, value }) => (
          <RealmSettingsHeader
            value={value}
            onChange={onChange}
            realmName={realmName}
            refresh={refreshHeader}
            save={() => save(getValues())}
          />
        )}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...form}>
          <RoutableTabs isBox mountOnEnter>
            <Tab
              title={<TabTitleText>{t("general")}</TabTitleText>}
              data-testid="rs-general-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName }),
                history,
              })}
            >
              <RealmSettingsGeneralTab
                save={save}
                reset={() => resetForm(realm)}
              />
            </Tab>
            <Tab
              title={<TabTitleText>{t("login")}</TabTitleText>}
              data-testid="rs-login-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName, tab: "login" }),
                history,
              })}
            >
              <RealmSettingsLoginTab
                refresh={refresh}
                save={save}
                realm={realm}
              />
            </Tab>
            <Tab
              title={<TabTitleText>{t("email")}</TabTitleText>}
              data-testid="rs-email-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName, tab: "email" }),
                history,
              })}
            >
              <RealmSettingsEmailTab realm={realm} />
            </Tab>
            <Tab
              title={<TabTitleText>{t("themes")}</TabTitleText>}
              data-testid="rs-themes-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName, tab: "themes" }),
                history,
              })}
            >
              <RealmSettingsThemesTab
                save={save}
                reset={() => resetForm(realm)}
              />
            </Tab>
            <Tab
              title={<TabTitleText>{t("realm-settings:keys")}</TabTitleText>}
              data-testid="rs-keys-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName, tab: "keys" }),
                history,
              })}
            >
              <Tabs
                activeKey={activeTab}
                onSelect={(_, key) => setActiveTab(Number(key))}
              >
                <Tab
                  id="keysList"
                  eventKey={0}
                  data-testid="rs-keys-list-tab"
                  aria-label="keys-list-subtab"
                  title={<TabTitleText>{t("keysList")}</TabTitleText>}
                >
                  <KeysListTab realmComponents={realmComponents} />
                </Tab>
                <Tab
                  id="providers"
                  data-testid="rs-providers-tab"
                  aria-label="rs-providers-tab"
                  eventKey={1}
                  title={<TabTitleText>{t("providers")}</TabTitleText>}
                >
                  <KeysProvidersTab
                    realmComponents={realmComponents}
                    keyProviderComponentTypes={kpComponentTypes}
                    refresh={refresh}
                  />
                </Tab>
              </Tabs>
            </Tab>
            <Tab
              title={<TabTitleText>{t("events")}</TabTitleText>}
              data-testid="rs-realm-events-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName, tab: "events" }),
                history,
              })}
            >
              <EventsTab />
            </Tab>
            <Tab
              title={<TabTitleText>{t("localization")}</TabTitleText>}
              data-testid="rs-localization-tab"
              {...routableTab({
                to: toRealmSettings({ realm: realmName, tab: "localization" }),
                history,
              })}
            >
              <LocalizationTab
                key={key}
                refresh={refresh}
                save={save}
                reset={() => resetForm(realm)}
                realm={realm}
              />
            </Tab>
            <Tab
              title={<TabTitleText>{t("securityDefences")}</TabTitleText>}
              data-testid="rs-security-defenses-tab"
              {...routableTab({
                to: toRealmSettings({
                  realm: realmName,
                  tab: "securityDefences",
                }),
                history,
              })}
            >
              <SecurityDefences save={save} reset={() => resetForm(realm)} />
            </Tab>
            <Tab
              title={
                <TabTitleText>{t("realm-settings:sessions")}</TabTitleText>
              }
              data-testid="rs-sessions-tab"
              {...routableTab({
                to: toRealmSettings({
                  realm: realmName,
                  tab: "sessions",
                }),
                history,
              })}
            >
              <RealmSettingsSessionsTab key={key} realm={realm} save={save} />
            </Tab>
            <Tab
              title={<TabTitleText>{t("realm-settings:tokens")}</TabTitleText>}
              data-testid="rs-tokens-tab"
              {...routableTab({
                to: toRealmSettings({
                  realm: realmName,
                  tab: "tokens",
                }),
                history,
              })}
            >
              <RealmSettingsTokensTab
                save={save}
                realm={realm}
                reset={() => resetForm(realm)}
              />
            </Tab>
            <Tab
              title={
                <TabTitleText>
                  {t("realm-settings:clientPolicies")}
                </TabTitleText>
              }
              data-testid="rs-clientPolicies-tab"
              {...routableTab({
                to: toRealmSettings({
                  realm: realmName,
                  tab: "clientPolicies",
                }),
                history,
              })}
            >
              <Tabs
                activeKey={activeTab}
                onSelect={(_, key) => setActiveTab(Number(key))}
              >
                <Tab
                  id="profiles"
                  eventKey={0}
                  data-testid="rs-policies-clientProfiles-tab"
                  aria-label={t("clientProfilesSubTab")}
                  title={
                    <TabTitleText>
                      {t("profiles")}
                      <span className="kc-help-text">
                        <HelpItem
                          helpText="realm-settings:clientPoliciesProfilesHelpText"
                          fieldLabelId="realm-settings:clientPoliciesProfiles"
                        />
                      </span>
                    </TabTitleText>
                  }
                >
                  <ProfilesTab />
                </Tab>
                <Tab
                  id="policies"
                  data-testid="rs-policies-clientPolicies-tab"
                  aria-label={t("clientPoliciesSubTab")}
                  eventKey={1}
                  title={
                    <TabTitleText>
                      {t("policies")}
                      <span className="kc-help-text">
                        <HelpItem
                          helpText="realm-settings:clientPoliciesPoliciesHelpText"
                          fieldLabelId="realm-settings:clientPoliciesPolicies"
                        />
                      </span>
                    </TabTitleText>
                  }
                >
                  <PoliciesTab />
                </Tab>
              </Tabs>
            </Tab>
            {isFeatureEnabled(Feature.DeclarativeUserProfile) &&
              userProfileEnabled === "true" && (
                <Tab
                  title={
                    <TabTitleText>
                      {t("realm-settings:userProfile")}
                    </TabTitleText>
                  }
                  data-testid="rs-user-profile-tab"
                  {...routableTab({
                    to: toRealmSettings({
                      realm: realmName,
                      tab: "userProfile",
                    }),
                    history,
                  })}
                >
                  <UserProfileTab />
                </Tab>
              )}
            <Tab
              title={<TabTitleText>{t("userRegistration")}</TabTitleText>}
              data-testid="rs-userRegistration-tab"
              {...routableTab({
                to: toRealmSettings({
                  realm: realmName,
                  tab: "userRegistration",
                }),
                history,
              })}
            >
              <UserRegistration />
            </Tab>
          </RoutableTabs>
        </FormProvider>
      </PageSection>
    </>
  );
};
