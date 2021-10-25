import React, { useState } from "react";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { BruteForced, UserForm } from "./UserForm";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useHistory, useParams } from "react-router-dom";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { UserGroups } from "./UserGroups";
import { UserConsents } from "./UserConsents";
import { useRealm } from "../context/realm-context/RealmContext";
import { UserIdentityProviderLinks } from "./UserIdentityProviderLinks";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { toUser } from "./routes/User";
import { toUsers } from "./routes/Users";
import { UserRoleMapping } from "./UserRoleMapping";
import { UserAttributes } from "./UserAttributes";

export const UsersTabs = () => {
  const { t } = useTranslation("users");
  const { addAlert, addError } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

  const adminClient = useAdminClient();
  const userForm = useForm<UserRepresentation>({ mode: "onChange" });
  const { id } = useParams<{ id: string }>();
  const [user, setUser] = useState<UserRepresentation>();
  const [bruteForced, setBruteForced] = useState<BruteForced>();
  const [addedGroups, setAddedGroups] = useState<GroupRepresentation[]>([]);

  useFetch(
    async () => {
      if (id) {
        const user = await adminClient.users.findOne({ id });
        if (!user) {
          throw new Error(t("common:notFound"));
        }

        const isBruteForceProtected = (await adminClient.realms.findOne({
          realm,
        }))!.bruteForceProtected;
        const bruteForce = await adminClient.attackDetection.findOne({
          id: user.id!,
        });
        const isLocked: boolean =
          isBruteForceProtected && bruteForce && bruteForce.disabled;
        return { user, bruteForced: { isBruteForceProtected, isLocked } };
      }
      return { user: undefined };
    },
    ({ user, bruteForced }) => {
      setUser(user);
      setBruteForced(bruteForced);
      user && setupForm(user);
    },
    []
  );

  const setupForm = (user: UserRepresentation) => {
    userForm.reset(user);
  };

  const updateGroups = (groups: GroupRepresentation[]) => {
    setAddedGroups(groups);
  };

  const save = async (user: UserRepresentation) => {
    try {
      if (id) {
        await adminClient.users.update({ id }, user);
        addAlert(t("userSaved"), AlertVariant.success);
      } else {
        const createdUser = await adminClient.users.create(user);

        addedGroups.forEach(async (group) => {
          await adminClient.users.addToGroup({
            id: createdUser.id!,
            groupId: group.id!,
          });
        });

        addAlert(t("userCreated"), AlertVariant.success);
        history.push(toUser({ id: createdUser.id, realm, tab: "settings" }));
      }
    } catch (error) {
      addError("userCreateError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "users:deleteConfirm",
    messageKey: "users:deleteConfirmCurrentUser",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.users.del({ id });
        addAlert(t("userDeletedSuccess"), AlertVariant.success);
        history.push(toUsers({ realm }));
      } catch (error) {
        addError("users:userDeletedError", error);
      }
    },
  });

  const [toggleImpersonateDialog, ImpersonateConfirm] = useConfirmDialog({
    titleKey: "users:impersonateConfirm",
    messageKey: "users:impersonateConfirmDialog",
    continueButtonLabel: "users:impersonate",
    onConfirm: async () => {
      try {
        const data = await adminClient.users.impersonation(
          { id },
          { user: id, realm }
        );
        if (data.sameRealm) {
          window.location = data.redirect;
        } else {
          window.open(data.redirect, "_blank");
        }
      } catch (error) {
        addError("users:impersonateError", error);
      }
    },
  });

  return (
    <>
      <ImpersonateConfirm />
      <DeleteConfirm />
      <ViewHeader
        titleKey={user?.username || t("createUser")}
        divider={!id}
        dropdownItems={[
          <DropdownItem
            key="impersonate"
            onClick={() => toggleImpersonateDialog()}
          >
            {t("impersonate")}
          </DropdownItem>,
          <DropdownItem key="delete" onClick={() => toggleDeleteDialog()}>
            {t("common:delete")}
          </DropdownItem>,
        ]}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...userForm}>
          {id && user && (
            <KeycloakTabs isBox>
              <Tab
                eventKey="settings"
                data-testid="user-details-tab"
                title={<TabTitleText>{t("common:details")}</TabTitleText>}
              >
                <PageSection variant="light">
                  {bruteForced && (
                    <UserForm
                      onGroupsUpdate={updateGroups}
                      save={save}
                      user={user}
                      bruteForce={bruteForced}
                    />
                  )}
                </PageSection>
              </Tab>
              <Tab
                eventKey="attributes"
                data-testid="attributes"
                title={<TabTitleText>{t("common:attributes")}</TabTitleText>}
              >
                <UserAttributes user={user} />
              </Tab>
              <Tab
                eventKey="groups"
                data-testid="user-groups-tab"
                title={<TabTitleText>{t("common:groups")}</TabTitleText>}
              >
                <UserGroups user={user} />
              </Tab>
              <Tab
                eventKey="consents"
                data-testid="user-consents-tab"
                title={<TabTitleText>{t("consents")}</TabTitleText>}
              >
                <UserConsents />
              </Tab>
              <Tab
                eventKey="role-mapping"
                data-testid="role-mapping-tab"
                title={<TabTitleText>{t("roleMapping")}</TabTitleText>}
              >
                <UserRoleMapping id={id} name={user.username!} />
              </Tab>
              <Tab
                eventKey="identity-provider-links"
                data-testid="identity-provider-links-tab"
                title={
                  <TabTitleText>{t("identityProviderLinks")}</TabTitleText>
                }
              >
                <UserIdentityProviderLinks />
              </Tab>
            </KeycloakTabs>
          )}
          {!id && (
            <PageSection variant="light">
              <UserForm onGroupsUpdate={updateGroups} save={save} />
            </PageSection>
          )}
        </FormProvider>
      </PageSection>
    </>
  );
};
