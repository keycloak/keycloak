import type {
  UserProfileConfig,
  UserProfileMetadata,
} from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  isUserProfileError,
  setUserProfileServerError,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  Label,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
  Tooltip,
} from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { TFunction } from "i18next";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeyValueType } from "../components/key-value-form/key-value-convert";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { UserProfileProvider } from "../realm-settings/user-profile/UserProfileContext";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { useParams } from "../utils/useParams";
import { Organizations } from "./Organizations";
import { UserAttributes } from "./UserAttributes";
import { UserConsents } from "./UserConsents";
import { UserCredentials } from "./UserCredentials";
import { BruteForced, UserForm } from "./UserForm";
import { UserGroups } from "./UserGroups";
import { UserIdentityProviderLinks } from "./UserIdentityProviderLinks";
import { UserRoleMapping } from "./UserRoleMapping";
import { UserSessions } from "./UserSessions";
import { UserEvents } from "../events/UserEvents";
import {
  UIUserRepresentation,
  UserFormFields,
  filterManagedAttributes,
  toUserFormFields,
  toUserRepresentation,
} from "./form-state";
import { UserParams, UserTab, toUser } from "./routes/User";
import { toUsers } from "./routes/Users";
import { isLightweightUser } from "./utils";

import "./user-section.css";
import { AdminEvents } from "../events/AdminEvents";

export default function EditUser() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { hasAccess } = useAccess();
  const { id } = useParams<UserParams>();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  // Validation of form fields is performed on server, thus we need to clear all errors before submit
  const clearAllErrorsBeforeSubmit = async (values: UserFormFields) => ({
    values,
    errors: {},
  });
  const form = useForm<UserFormFields>({
    mode: "onChange",
    resolver: clearAllErrorsBeforeSubmit,
  });
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

  const [realmHasOrganizations, setRealmHasOrganizations] = useState(false);
  const isFeatureEnabled = useIsFeatureEnabled();
  const showOrganizations =
    isFeatureEnabled(Feature.Organizations) && realm?.organizationsEnabled;

  const toTab = (tab: UserTab) =>
    toUser({
      realm: realmName,
      id: user?.id || "",
      tab,
    });

  const [activeEventsTab, setActiveEventsTab] = useState("userEvents");

  const settingsTab = useRoutableTab(toTab("settings"));
  const attributesTab = useRoutableTab(toTab("attributes"));
  const credentialsTab = useRoutableTab(toTab("credentials"));
  const roleMappingTab = useRoutableTab(toTab("role-mapping"));
  const groupsTab = useRoutableTab(toTab("groups"));
  const organizationsTab = useRoutableTab(toTab("organizations"));
  const consentsTab = useRoutableTab(toTab("consents"));
  const identityProviderLinksTab = useRoutableTab(
    toTab("identity-provider-links"),
  );
  const sessionsTab = useRoutableTab(toTab("sessions"));
  const eventsTab = useRoutableTab(toTab("events"));

  useFetch(
    async () =>
      Promise.all([
        adminClient.users.findOne({
          id: id!,
          userProfileMetadata: true,
        }) as UIUserRepresentation | undefined,
        adminClient.attackDetection.findOne({ id: id! }),
        adminClient.users.getUnmanagedAttributes({ id: id! }),
        adminClient.users.getProfile({ realm: realmName }),
        showOrganizations
          ? adminClient.organizations.find({ first: 0, max: 1 })
          : [],
      ]),
    ([
      userData,
      attackDetection,
      unmanagedAttributes,
      upConfig,
      organizations,
    ]) => {
      if (!userData || !realm || !attackDetection) {
        throw new Error(t("notFound"));
      }

      const { userProfileMetadata, ...user } = userData;
      setUserProfileMetadata(userProfileMetadata);
      user.unmanagedAttributes = unmanagedAttributes;
      user.attributes = filterManagedAttributes(
        user.attributes,
        unmanagedAttributes,
      );

      if (upConfig.unmanagedAttributePolicy !== undefined) {
        setUnmanagedAttributesEnabled(true);
      }

      setUser(user);
      setUpConfig(upConfig);

      const isBruteForceProtected = realm.bruteForceProtected;
      const isLocked = isBruteForceProtected && attackDetection.disabled;

      setBruteForced({ isBruteForceProtected, isLocked });
      setRealmHasOrganizations(organizations.length === 1);

      form.reset(toUserFormFields(user));
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
        if (
          isUnmanagedAttributesEnabled &&
          Array.isArray(data.unmanagedAttributes)
        ) {
          const unmanagedAttributeErrors: object[] = new Array(
            data.unmanagedAttributes.length,
          );
          let someUnmanagedAttributeError = false;
          setUserProfileServerError<UserFormFields>(
            error,
            (field, params) => {
              if (field.startsWith("attributes.")) {
                const attributeName = field.substring("attributes.".length);
                (data.unmanagedAttributes as KeyValueType[]).forEach(
                  (attr, index) => {
                    if (attr.key === attributeName) {
                      unmanagedAttributeErrors[index] = params;
                      someUnmanagedAttributeError = true;
                    }
                  },
                );
              } else {
                form.setError(field, params);
              }
            },
            ((key, param) => t(key as string, param as any)) as TFunction,
          );
          if (someUnmanagedAttributeError) {
            form.setError(
              "unmanagedAttributes",
              unmanagedAttributeErrors as any,
            );
          }
        } else {
          setUserProfileServerError<UserFormFields>(error, form.setError, ((
            key,
            param,
          ) => t(key as string, param as any)) as TFunction);
        }
        addError("userNotSaved", "");
      } else {
        addError("userCreateError", error);
      }
    }
  };

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "disableConfirmUserTitle",
    messageKey: "disableConfirmUser",
    continueButtonLabel: "disable",
    onConfirm: async () => {
      await save({
        ...toUserFormFields(user!),
        enabled: false,
      });
    },
  });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteConfirmUsers",
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

  if (!user || !bruteForced) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ImpersonateConfirm />
      <DeleteConfirm />
      <DisableConfirm />
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
        onToggle={async (value) => {
          if (!value) {
            toggleDisableDialog();
          } else {
            await save({
              ...toUserFormFields(user),
              enabled: value,
            });
          }
        }}
        isEnabled={user.enabled}
      />

      <PageSection variant="light" className="pf-v5-u-p-0">
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
                    realm={realm!}
                    user={user}
                    bruteForce={bruteForced}
                    userProfileMetadata={userProfileMetadata}
                    refresh={refresh}
                    save={save}
                  />
                </PageSection>
              </Tab>
              {isUnmanagedAttributesEnabled && (
                <Tab
                  data-testid="attributesTab"
                  title={<TabTitleText>{t("attributes")}</TabTitleText>}
                  {...attributesTab}
                >
                  <UserAttributes user={user} save={save} upConfig={upConfig} />
                </Tab>
              )}
              <Tab
                data-testid="credentials"
                isHidden={!user.access?.view}
                title={<TabTitleText>{t("credentials")}</TabTitleText>}
                {...credentialsTab}
              >
                <UserCredentials user={user} setUser={setUser} />
              </Tab>
              <Tab
                data-testid="role-mapping-tab"
                isHidden={!user.access?.view}
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
              {showOrganizations && realmHasOrganizations && (
                <Tab
                  data-testid="user-organizations-tab"
                  title={<TabTitleText>{t("organizations")}</TabTitleText>}
                  {...organizationsTab}
                >
                  <Organizations user={user} />
                </Tab>
              )}
              <Tab
                data-testid="user-consents-tab"
                title={<TabTitleText>{t("consents")}</TabTitleText>}
                {...consentsTab}
              >
                <UserConsents />
              </Tab>
              <Tab
                data-testid="identity-provider-links-tab"
                title={
                  <TabTitleText>{t("identityProviderLinks")}</TabTitleText>
                }
                {...identityProviderLinksTab}
              >
                <UserIdentityProviderLinks userId={user.id!} />
              </Tab>
              <Tab
                data-testid="user-sessions-tab"
                title={<TabTitleText>{t("sessions")}</TabTitleText>}
                {...sessionsTab}
              >
                <UserSessions />
              </Tab>
              {hasAccess("view-events") && (
                <Tab
                  data-testid="events-tab"
                  title={<TabTitleText>{t("events")}</TabTitleText>}
                  {...eventsTab}
                >
                  <Tabs
                    activeKey={activeEventsTab}
                    onSelect={(_, key) => setActiveEventsTab(key as string)}
                  >
                    <Tab
                      eventKey="userEvents"
                      title={<TabTitleText>{t("userEvents")}</TabTitleText>}
                    >
                      <UserEvents user={user.id} />
                    </Tab>
                    <Tab
                      eventKey="adminEvents"
                      title={<TabTitleText>{t("adminEvents")}</TabTitleText>}
                    >
                      <AdminEvents resourcePath={`users/${user.id}`} />
                    </Tab>
                  </Tabs>
                </Tab>
              )}
            </RoutableTabs>
          </FormProvider>
        </UserProfileProvider>
      </PageSection>
    </>
  );
}
