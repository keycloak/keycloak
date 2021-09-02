import React, { useState } from "react";
import {
  AlertVariant,
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
import { toUser } from "./routes/User";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
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
        const isBruteForceProtected = (
          await adminClient.realms.findOne({ realm })
        ).bruteForceProtected;
        const isLocked: boolean =
          isBruteForceProtected &&
          (await adminClient.attackDetection.findOne({ id: user.id! }))
            ?.disabled;
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
        addAlert(t("users:userSaved"), AlertVariant.success);
      } else {
        const createdUser = await adminClient.users.create(user);

        addedGroups.forEach(async (group) => {
          await adminClient.users.addToGroup({
            id: createdUser.id!,
            groupId: group.id!,
          });
        });

        addAlert(t("users:userCreated"), AlertVariant.success);
        history.push(toUser({ id: createdUser.id, realm, tab: "settings" }));
      }
    } catch (error) {
      addError("users:userCreateError", error);
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={user?.username || t("users:createUser")}
        divider={!id}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...userForm}>
          {id && user && (
            <KeycloakTabs isBox>
              <Tab
                eventKey="settings"
                data-testid="user-details-tab"
                title={<TabTitleText>{t("details")}</TabTitleText>}
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
                eventKey="groups"
                data-testid="user-groups-tab"
                title={<TabTitleText>{t("groups")}</TabTitleText>}
              >
                <UserGroups user={user} />
              </Tab>
              <Tab
                eventKey="consents"
                data-testid="user-consents-tab"
                title={<TabTitleText>{t("users:consents")}</TabTitleText>}
              >
                <UserConsents />
              </Tab>
              <Tab
                eventKey="identity-provider-links"
                data-testid="identity-provider-links-tab"
                title={
                  <TabTitleText>
                    {t("users:identityProviderLinks")}
                  </TabTitleText>
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
