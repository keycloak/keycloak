import type {
  UserProfileMetadata,
  UserProfileConfig,
} from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  Label,
  PageSection,
  Tab,
  TabTitleText,
  Tooltip,
} from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { TFunction } from "i18next";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { isUserProfileError, setUserProfileServerError } from "ui-shared";

import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { UserProfileProvider } from "../realm-settings/user-profile/UserProfileContext";
import { useFetch } from "../utils/useFetch";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { useParams } from "../utils/useParams";
import { UserAttributes } from "./UserAttributes";
import { UserConsents } from "./UserConsents";
import { UserCredentials } from "./UserCredentials";
import { BruteForced, UserForm } from "./UserForm";
import { UserGroups } from "./UserGroups";
import { UserIdentityProviderLinks } from "./UserIdentityProviderLinks";
import { UserRoleMapping } from "./UserRoleMapping";
import { UserSessions } from "./UserSessions";
import {
  UserFormFields,
  toUserFormFields,
  toUserRepresentation,
  filterManagedAttributes,
  UIUserRepresentation,
} from "./form-state";
import { UserParams, UserTab, toUser } from "./routes/User";
import { toUsers } from "./routes/Users";
import { isLightweightUser } from "./utils";
import { getUnmanagedAttributes } from "../components/users/resource";
import "./user-section.css";

export default function EditUser() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { hasAccess } = useAccess();
  const { id } = useParams<UserParams>();
  const { realm: realmName } = useRealm();
  const isFeatureEnabled = useIsFeatureEnabled();
  const form = useForm<UserFormFields>({ mode: "onChange" });
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [user, setUser] = useState<UIUserRepresentation>();
  const [bruteForced, setBruteForced] = useState<BruteForced>();
  const [isUnmanagedAttributesEnabled, setUnmanagedAttributesEnabled] =
    useState<boolean>();
  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();
  const [refreshCount, setRefreshCount] = useState(0);
  const refresh = () => setRefreshCount((count) => count + 1);
  const lightweightUser = isLightweightUser(user?.id);
  const [upConfig, setUpConfig] = useState<UserProfileConfig>();

  const toTab = (tab: UserTab) =>
    toUser({
      realm: realmName,
      id: user?.id || "",
      tab,
    });

  const useTab = (tab: UserTab) => useRoutableTab(toTab(tab));

  const settingsTab = useTab("settings");
  const attributesTab = useTab("attributes");
  const credentialsTab = useTab("credentials");
  const roleMappingTab = useTab("role-mapping");
  const groupsTab = useTab("groups");
  const consentsTab = useTab("consents");
  const identityProviderLinksTab = useTab("identity-provider-links");
  const sessionsTab = useTab("sessions");

  useFetch(
    async () =>
      Promise.all([
        adminClient.realms.findOne({ realm: realmName }),
        adminClient.users.findOne({
          id: id!,
          userProfileMetadata: true,
        }) as UIUserRepresentation | undefined,
        adminClient.attackDetection.findOne({ id: id! }),
        getUnmanagedAttributes(id!),
        adminClient.users.getProfile({ realm: realmName }),
      ]),
    ([realm, userData, attackDetection, unmanagedAttributes, upConfig]) => {
      if (!userData || !realm || !attackDetection) {
        throw new Error(t("notFound"));
      }

      const isUserProfileEnabled =
        isFeatureEnabled(Feature.DeclarativeUserProfile) &&
        realm.attributes?.userProfileEnabled === "true";

      const { userProfileMetadata, ...user } = userData;
      setUserProfileMetadata(
        isUserProfileEnabled ? userProfileMetadata : undefined,
      );

      if (isUserProfileEnabled) {
        user.unmanagedAttributes = unmanagedAttributes;
        user.attributes = filterManagedAttributes(
          user.attributes,
          unmanagedAttributes,
        );
      }

      if (
        upConfig.unmanagedAttributePolicy !== undefined ||
        !isUserProfileEnabled
      ) {
        setUnmanagedAttributesEnabled(true);
      }

      setRealm(realm);
      setUser(user);
      setUpConfig(upConfig);

      const isBruteForceProtected = realm.bruteForceProtected;
      const isLocked = isBruteForceProtected && attackDetection.disabled;

      setBruteForced({ isBruteForceProtected, isLocked });

      form.reset(toUserFormFields(user, isUserProfileEnabled));
    },
    [refreshCount],
  );

  const save = async (data: UserFormFields) => {
    try {
      await adminClient.users.update(
        { id: user!.id! },
        toUserRepresentation(data),
      );
      addAlert(t("userSaved"), AlertVariant.success);
      refresh();
    } catch (error) {
      if (isUserProfileError(error)) {
        setUserProfileServerError(error, form.setError, ((key, param) =>
          t(key as string, param as any)) as TFunction);
      } else {
        addError("userCreateError", error);
      }
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteConfirm",
    messageKey: "deleteConfirmCurrentUser",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        if (lightweightUser) {
          await adminClient.users.logout({ id: user!.id! });
        } else {
          await adminClient.users.del({ id: user!.id! });
        }
        addAlert(t("userDeletedSuccess"), AlertVariant.success);
        navigate(toUsers({ realm: realmName }));
      } catch (error) {
        addError("userDeletedError", error);
      }
    },
  });

  const [toggleImpersonateDialog, ImpersonateConfirm] = useConfirmDialog({
    titleKey: "impersonateConfirm",
    messageKey: "impersonateConfirmDialog",
    continueButtonLabel: "impersonate",
    onConfirm: async () => {
      try {
        const data = await adminClient.users.impersonation(
          { id: user!.id! },
          { user: user!.id!, realm: realmName },
        );
        if (data.sameRealm) {
          window.location = data.redirect;
        } else {
          window.open(data.redirect, "_blank");
        }
      } catch (error) {
        addError("impersonateError", error);
      }
    },
  });

  if (!realm || !user || !bruteForced) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ImpersonateConfirm />
      <DeleteConfirm />
      <ViewHeader
        titleKey={user.username!}
        className="kc-username-view-header"
        divider={false}
        badges={
          lightweightUser
            ? [
                {
                  text: (
                    <Tooltip content={t("transientUserTooltip")}>
                      <Label
                        data-testid="user-details-label-transient-user"
                        icon={<InfoCircleIcon />}
                      >
                        {t("transientUser")}
                      </Label>
                    </Tooltip>
                  ),
                },
              ]
            : []
        }
        dropdownItems={[
          <DropdownItem
            key="impersonate"
            isDisabled={!user.access?.impersonate}
            onClick={() => toggleImpersonateDialog()}
          >
            {t("impersonate")}
          </DropdownItem>,
          <DropdownItem
            key="delete"
            isDisabled={!user.access?.manage}
            onClick={() => toggleDeleteDialog()}
          >
            {t("delete")}
          </DropdownItem>,
        ]}
        onToggle={(value) =>
          save({
            ...toUserFormFields(user, !!userProfileMetadata),
            enabled: value,
          })
        }
        isEnabled={user.enabled}
      />

      <PageSection variant="light" className="pf-u-p-0">
        <UserProfileProvider>
          <FormProvider {...form}>
            <RoutableTabs
              isBox
              mountOnEnter
              defaultLocation={toTab("settings")}
            >
              <Tab
                data-testid="user-details-tab"
                title={<TabTitleText>{t("details")}</TabTitleText>}
                {...settingsTab}
              >
                <PageSection variant="light">
                  <UserForm
                    form={form}
                    realm={realm}
                    user={user}
                    bruteForce={bruteForced}
                    userProfileMetadata={userProfileMetadata}
                    save={save}
                  />
                </PageSection>
              </Tab>
              {isUnmanagedAttributesEnabled && (
                <Tab
                  data-testid="attributes"
                  title={<TabTitleText>{t("attributes")}</TabTitleText>}
                  {...attributesTab}
                >
                  <UserAttributes
                    user={user}
                    save={save}
                    upConfig={upConfig}
                    isUserProfileEnabled={!!userProfileMetadata}
                  />
                </Tab>
              )}
              <Tab
                data-testid="credentials"
                isHidden={!user.access?.view}
                title={<TabTitleText>{t("credentials")}</TabTitleText>}
                {...credentialsTab}
              >
                <UserCredentials user={user} />
              </Tab>
              <Tab
                data-testid="role-mapping-tab"
                isHidden={!user.access?.mapRoles}
                title={<TabTitleText>{t("roleMapping")}</TabTitleText>}
                {...roleMappingTab}
              >
                <UserRoleMapping id={user.id!} name={user.username!} />
              </Tab>
              {hasAccess("query-groups") && (
                <Tab
                  data-testid="user-groups-tab"
                  title={<TabTitleText>{t("groups")}</TabTitleText>}
                  {...groupsTab}
                >
                  <UserGroups user={user} />
                </Tab>
              )}
              <Tab
                data-testid="user-consents-tab"
                title={<TabTitleText>{t("consents")}</TabTitleText>}
                {...consentsTab}
              >
                <UserConsents />
              </Tab>
              {hasAccess("view-identity-providers") && (
                <Tab
                  data-testid="identity-provider-links-tab"
                  title={
                    <TabTitleText>{t("identityProviderLinks")}</TabTitleText>
                  }
                  {...identityProviderLinksTab}
                >
                  <UserIdentityProviderLinks userId={user.id!} />
                </Tab>
              )}
              <Tab
                data-testid="user-sessions-tab"
                title={<TabTitleText>{t("sessions")}</TabTitleText>}
                {...sessionsTab}
              >
                <UserSessions />
              </Tab>
            </RoutableTabs>
          </FormProvider>
        </UserProfileProvider>
      </PageSection>
    </>
  );
}
