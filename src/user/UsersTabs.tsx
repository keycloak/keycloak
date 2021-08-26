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
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { UserForm } from "./UserForm";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import { useHistory, useParams } from "react-router-dom";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { UserGroups } from "./UserGroups";
import { UserConsents } from "./UserConsents";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useRealm } from "../context/realm-context/RealmContext";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
  const { addAlert, addError } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

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
        history.push(`/${realm}/users/${createdUser.id}/settings`);
      }
    } catch (error) {
      addError("users:userCreateError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey={user! || t("users:createUser")} divider={!id} />
      <PageSection variant="light" className="pf-u-p-0">
        {id && (
          <KeycloakTabs isBox>
            <Tab
              eventKey="settings"
              data-testid="user-details-tab"
              title={<TabTitleText>{t("details")}</TabTitleText>}
            >
              <PageSection variant="light">
                <UserForm
                  onGroupsUpdate={updateGroups}
                  form={userForm}
                  save={save}
                  editMode={true}
                />
              </PageSection>
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
          <PageSection variant="light">
            <UserForm
              onGroupsUpdate={updateGroups}
              form={userForm}
              save={save}
              editMode={false}
            />
          </PageSection>
        )}
      </PageSection>
    </>
  );
};
