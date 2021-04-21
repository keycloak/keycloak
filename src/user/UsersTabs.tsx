import React, { useEffect, useState } from "react";
import {
  AlertVariant,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { ViewHeader } from "../components/view-header/ViewHeader";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { UserForm } from "./UserForm";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import { useHistory, useParams, useRouteMatch } from "react-router-dom";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { UserGroups } from "./UserGroups";
import { UserConsents } from "./UserConsents";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
  const { addAlert } = useAlerts();
  const { url } = useRouteMatch();
  const history = useHistory();

  const adminClient = useAdminClient();
  const userForm = useForm<UserRepresentation>({ mode: "onChange" });
  const { id } = useParams<{ id: string }>();
  const [user, setUser] = useState("");
  const [addedGroups, setAddedGroups] = useState<GroupRepresentation[]>([]);

  useEffect(() => {
    const update = async () => {
      if (id) {
        const fetchedUser = await adminClient.users.findOne({ id });
        setUser(fetchedUser.username!);
      }
    };
    setTimeout(update, 100);
  }, []);

  const updateGroups = (groups: GroupRepresentation[]) => {
    setAddedGroups(groups);
  };

  const save = async (user: UserRepresentation) => {
    try {
      if (id) {
        await adminClient.users.update({ id: user.id! }, user);
        addAlert(t("users:userSaved"), AlertVariant.success);
      } else {
        const getNewUserId = await adminClient.users.create(user);

        addedGroups.forEach(async (group) => {
          await adminClient.users.addToGroup({
            id: getNewUserId.id!,
            groupId: group.id!,
          });
        });

        addAlert(t("users:userCreated"), AlertVariant.success);
        history.push(url.substr(0, url.lastIndexOf("/")));
      }
    } catch (error) {
      addAlert(
        t("users:userCreateError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader titleKey={user! || t("users:createUser")} />
      <PageSection variant="light">
        {id && (
          <KeycloakTabs isBox>
            <Tab
              eventKey="settings"
              data-testid="user-details-tab"
              title={<TabTitleText>{t("details")}</TabTitleText>}
            >
              <UserForm
                onGroupsUpdate={updateGroups}
                form={userForm}
                save={save}
                editMode={true}
              />
            </Tab>
            <Tab
              eventKey="groups"
              data-testid="user-groups-tab"
              title={<TabTitleText>{t("groups")}</TabTitleText>}
            >
              <UserGroups />
            </Tab>
            <Tab
              eventKey="consents"
              data-testid="user-consents-tab"
              title={<TabTitleText>{t("users:consents")}</TabTitleText>}
            >
              <UserConsents />
            </Tab>
          </KeycloakTabs>
        )}
        {!id && (
          <UserForm
            onGroupsUpdate={updateGroups}
            form={userForm}
            save={save}
            editMode={false}
          />
        )}
      </PageSection>
    </>
  );
};
