import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  DropdownSeparator,
  PageSection,
  Tab,
  TabTitleText,
  Tooltip,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useRealms } from "../context/RealmsContext";
import { toDashboard } from "../dashboard/routes/Dashboard";
import environment from "../environment";
import helpUrls from "../help-urls";
import { convertFormValuesToObject, convertToFormValues } from "../util";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { RealmSettingsEmailTab } from "./EmailTab";
import { EventsTab } from "./event-config/EventsTab";
import { RealmSettingsGeneralTab } from "./GeneralTab";
import { KeysTab } from "./keys/KeysTab";
import { LocalizationTab } from "./LocalizationTab";
import { RealmSettingsLoginTab } from "./LoginTab";
import { PartialExportDialog } from "./PartialExport";
import { PartialImportDialog } from "./PartialImport";
import { PoliciesTab } from "./PoliciesTab";
import ProfilesTab from "./ProfilesTab";
import { ClientPoliciesTab, toClientPolicies } from "./routes/ClientPolicies";
import { RealmSettingsTab, toRealmSettings } from "./routes/RealmSettings";
import { SecurityDefenses } from "./security-defences/SecurityDefenses";
import { RealmSettingsSessionsTab } from "./SessionsTab";
import { RealmSettingsThemesTab } from "./ThemesTab";
import { RealmSettingsTokensTab } from "./TokensTab";
import { UserProfileTab } from "./user-profile/UserProfileTab";
import { UserRegistration } from "./UserRegistration";

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
  const { adminClient } = useAdminClient();
  const { refresh: refreshRealms } = useRealms();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
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
        navigate(toDashboard({ realm: environment.masterRealm }));
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
        titleKey={realmName}
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
};

export const RealmSettingsTabs = ({
  realm,
  refresh,
}: RealmSettingsTabsProps) => {
  const { t } = useTranslation("realm-settings");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const { refresh: refreshRealms } = useRealms();
  const navigate = useNavigate();
  const isFeatureEnabled = useIsFeatureEnabled();

  const { control, setValue, getValues } = useForm({
    mode: "onChange",
  });
  const [key, setKey] = useState(0);

  const refreshHeader = () => {
    setKey(key + 1);
  };

  const setupForm = (r: RealmRepresentation = realm) => {
    convertToFormValues(r, setValue);
  };

  useEffect(setupForm, []);

  const save = async (r: RealmRepresentation) => {
    r = convertFormValuesToObject(r);
    if (
      r.attributes?.["acr.loa.map"] &&
      typeof r.attributes["acr.loa.map"] !== "string"
    ) {
      r.attributes["acr.loa.map"] = JSON.stringify(
        Object.fromEntries(
          (r.attributes["acr.loa.map"] as KeyValueType[])
            .filter(({ key }) => key !== "")
            .map(({ key, value }) => [key, value])
        )
      );
    }

    try {
      await adminClient.realms.update(
        { realm: realmName },
        {
          ...realm,
          ...r,
          id: r.realm,
        }
      );
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }

    const isRealmRenamed = realmName !== (r.realm || realm.realm);
    if (isRealmRenamed) {
      await refreshRealms();
      navigate(toRealmSettings({ realm: r.realm!, tab: "general" }));
    }
    refresh();
  };

  const useTab = (tab: RealmSettingsTab) =>
    useRoutableTab(toRealmSettings({ realm: realmName, tab }));

  const generalTab = useTab("general");
  const loginTab = useTab("login");
  const emailTab = useTab("email");
  const themesTab = useTab("themes");
  const keysTab = useTab("keys");
  const eventsTab = useTab("events");
  const localizationTab = useTab("localization");
  const securityDefensesTab = useTab("security-defenses");
  const sessionsTab = useTab("sessions");
  const tokensTab = useTab("tokens");
  const clientPoliciesTab = useTab("client-policies");
  const userProfileTab = useTab("user-profile");
  const userRegistrationTab = useTab("user-registration");

  const useClientPoliciesTab = (tab: ClientPoliciesTab) =>
    useRoutableTab(
      toClientPolicies({
        realm: realmName,
        tab,
      })
    );

  const clientPoliciesProfilesTab = useClientPoliciesTab("profiles");
  const clientPoliciesPoliciesTab = useClientPoliciesTab("policies");

  return (
    <>
      <Controller
        name="enabled"
        defaultValue={true}
        control={control}
        render={({ field }) => (
          <RealmSettingsHeader
            value={field.value}
            onChange={field.onChange}
            realmName={realmName}
            refresh={refreshHeader}
            save={() => save(getValues())}
          />
        )}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs
          isBox
          mountOnEnter
          defaultLocation={toRealmSettings({
            realm: realmName,
            tab: "general",
          })}
        >
          <Tab
            title={<TabTitleText>{t("general")}</TabTitleText>}
            data-testid="rs-general-tab"
            {...generalTab}
          >
            <RealmSettingsGeneralTab realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("login")}</TabTitleText>}
            data-testid="rs-login-tab"
            {...loginTab}
          >
            <RealmSettingsLoginTab refresh={refresh} realm={realm} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("email")}</TabTitleText>}
            data-testid="rs-email-tab"
            {...emailTab}
          >
            <RealmSettingsEmailTab realm={realm} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("themes")}</TabTitleText>}
            data-testid="rs-themes-tab"
            {...themesTab}
          >
            <RealmSettingsThemesTab realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("realm-settings:keys")}</TabTitleText>}
            data-testid="rs-keys-tab"
            {...keysTab}
          >
            <KeysTab />
          </Tab>
          <Tab
            title={<TabTitleText>{t("events")}</TabTitleText>}
            data-testid="rs-realm-events-tab"
            {...eventsTab}
          >
            <EventsTab realm={realm} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("localization")}</TabTitleText>}
            data-testid="rs-localization-tab"
            {...localizationTab}
          >
            <LocalizationTab
              key={key}
              refresh={refresh}
              save={save}
              realm={realm}
            />
          </Tab>
          <Tab
            title={<TabTitleText>{t("securityDefences")}</TabTitleText>}
            data-testid="rs-security-defenses-tab"
            {...securityDefensesTab}
          >
            <SecurityDefenses realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("realm-settings:sessions")}</TabTitleText>}
            data-testid="rs-sessions-tab"
            {...sessionsTab}
          >
            <RealmSettingsSessionsTab key={key} realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("realm-settings:tokens")}</TabTitleText>}
            data-testid="rs-tokens-tab"
            {...tokensTab}
          >
            <RealmSettingsTokensTab save={save} realm={realm} />
          </Tab>
          <Tab
            title={
              <TabTitleText>{t("realm-settings:clientPolicies")}</TabTitleText>
            }
            data-testid="rs-clientPolicies-tab"
            {...clientPoliciesTab}
          >
            <RoutableTabs
              mountOnEnter
              defaultLocation={toClientPolicies({
                realm: realmName,
                tab: "profiles",
              })}
            >
              <Tab
                id="profiles"
                data-testid="rs-policies-clientProfiles-tab"
                aria-label={t("clientProfilesSubTab")}
                title={<TabTitleText>{t("profiles")}</TabTitleText>}
                tooltip={
                  <Tooltip
                    content={t("realm-settings:clientPoliciesProfilesHelpText")}
                  />
                }
                {...clientPoliciesProfilesTab}
              >
                <ProfilesTab />
              </Tab>
              <Tab
                id="policies"
                data-testid="rs-policies-clientPolicies-tab"
                aria-label={t("clientPoliciesSubTab")}
                {...clientPoliciesPoliciesTab}
                title={<TabTitleText>{t("policies")}</TabTitleText>}
                tooltip={
                  <Tooltip
                    content={t("realm-settings:clientPoliciesPoliciesHelpText")}
                  />
                }
              >
                <PoliciesTab />
              </Tab>
            </RoutableTabs>
          </Tab>
          {isFeatureEnabled(Feature.DeclarativeUserProfile) &&
            realm.attributes?.userProfileEnabled === "true" && (
              <Tab
                title={
                  <TabTitleText>{t("realm-settings:userProfile")}</TabTitleText>
                }
                data-testid="rs-user-profile-tab"
                {...userProfileTab}
              >
                <UserProfileTab />
              </Tab>
            )}
          <Tab
            title={<TabTitleText>{t("userRegistration")}</TabTitleText>}
            data-testid="rs-userRegistration-tab"
            {...userRegistrationTab}
          >
            <UserRegistration />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
};
