import { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { useNavigate } from "react-router-dom-v5-compat";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
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

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  routableTab,
  RoutableTabs,
} from "../components/routable-tabs/RoutableTabs";
import { useRealm } from "../context/realm-context/RealmContext";
import { useRealms } from "../context/RealmsContext";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { convertFormValuesToObject, convertToFormValues } from "../util";

import { RealmSettingsEmailTab } from "./EmailTab";
import { EventsTab } from "./event-config/EventsTab";
import { RealmSettingsGeneralTab } from "./GeneralTab";
import { RealmSettingsLoginTab } from "./LoginTab";
import { SecurityDefenses } from "./security-defences/SecurityDefenses";
import { RealmSettingsSessionsTab } from "./SessionsTab";
import { RealmSettingsThemesTab } from "./ThemesTab";
import { RealmSettingsTokensTab } from "./TokensTab";
import ProfilesTab from "./ProfilesTab";
import { PoliciesTab } from "./PoliciesTab";
import { PartialImportDialog } from "./PartialImport";
import { PartialExportDialog } from "./PartialExport";
import { RealmSettingsTab, toRealmSettings } from "./routes/RealmSettings";
import { LocalizationTab } from "./LocalizationTab";
import { UserRegistration } from "./UserRegistration";
import { toDashboard } from "../dashboard/routes/Dashboard";
import environment from "../environment";
import helpUrls from "../help-urls";
import { UserProfileTab } from "./user-profile/UserProfileTab";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { ClientPoliciesTab, toClientPolicies } from "./routes/ClientPolicies";
import { KeysTab } from "./keys/KeysTab";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";

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
  const history = useHistory();
  const navigate = useNavigate();
  const isFeatureEnabled = useIsFeatureEnabled();

  const { control, setValue, getValues } = useForm({
    mode: "onChange",
    shouldUnregister: false,
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

  const route = (tab: RealmSettingsTab | undefined = "general") =>
    routableTab({
      to: toRealmSettings({ realm: realmName, tab }),
      history,
    });

  const policiesRoute = (tab: ClientPoliciesTab) =>
    routableTab({
      to: toClientPolicies({
        realm: realmName,
        tab,
      }),
      history,
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
            {...route()}
          >
            <RealmSettingsGeneralTab realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("login")}</TabTitleText>}
            data-testid="rs-login-tab"
            {...route("login")}
          >
            <RealmSettingsLoginTab refresh={refresh} realm={realm} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("email")}</TabTitleText>}
            data-testid="rs-email-tab"
            {...route("email")}
          >
            <RealmSettingsEmailTab realm={realm} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("themes")}</TabTitleText>}
            data-testid="rs-themes-tab"
            {...route("themes")}
          >
            <RealmSettingsThemesTab realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("realm-settings:keys")}</TabTitleText>}
            data-testid="rs-keys-tab"
            {...route("keys")}
          >
            <KeysTab />
          </Tab>
          <Tab
            title={<TabTitleText>{t("events")}</TabTitleText>}
            data-testid="rs-realm-events-tab"
            {...route("events")}
          >
            <EventsTab realm={realm} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("localization")}</TabTitleText>}
            data-testid="rs-localization-tab"
            {...route("localization")}
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
            {...route("security-defenses")}
          >
            <SecurityDefenses realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("realm-settings:sessions")}</TabTitleText>}
            data-testid="rs-sessions-tab"
            {...route("sessions")}
          >
            <RealmSettingsSessionsTab key={key} realm={realm} save={save} />
          </Tab>
          <Tab
            title={<TabTitleText>{t("realm-settings:tokens")}</TabTitleText>}
            data-testid="rs-tokens-tab"
            {...route("tokens")}
          >
            <RealmSettingsTokensTab save={save} realm={realm} />
          </Tab>
          <Tab
            title={
              <TabTitleText>{t("realm-settings:clientPolicies")}</TabTitleText>
            }
            data-testid="rs-clientPolicies-tab"
            {...route("client-policies")}
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
                {...policiesRoute("profiles")}
              >
                <ProfilesTab />
              </Tab>
              <Tab
                id="policies"
                data-testid="rs-policies-clientPolicies-tab"
                aria-label={t("clientPoliciesSubTab")}
                {...policiesRoute("policies")}
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
                {...route("user-profile")}
              >
                <UserProfileTab />
              </Tab>
            )}
          <Tab
            title={<TabTitleText>{t("userRegistration")}</TabTitleText>}
            data-testid="rs-userRegistration-tab"
            {...route("user-registration")}
          >
            <UserRegistration />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
};
