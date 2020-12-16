import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Label,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { InfoCircleIcon, WarningTriangleIcon } from "@patternfly/react-icons";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";

import { useAdminClient } from "../context/auth/AdminClient";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAlerts } from "../components/alert/Alerts";
import { RealmContext } from "../context/realm-context/RealmContext";

type BruteUser = UserRepresentation & {
  brute?: Record<string, object>;
};

export const UsersSection = () => {
  const { t } = useTranslation("users");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { realm: realmName } = useContext(RealmContext);

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };
    if (search) {
      params.search = search;
    }

    const users = await adminClient.users.find({ ...params });
    const realm = await adminClient.realms.findOne({ realm: realmName });
    if (realm?.bruteForceProtected) {
      const brutes = await Promise.all(
        users.map((user: BruteUser) =>
          adminClient.attackDetection.findOne({
            id: user.id!,
          })
        )
      );
      for (let index = 0; index < users.length; index++) {
        const user: BruteUser = users[index];
        user.brute = brutes[index];
      }
    }

    return users;
  };

  const deleteUser = async (user: UserRepresentation) => {
    try {
      await adminClient.users.del({ id: user.id! });
      refresh();
      addAlert(t("userDeletedSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(t("userDeletedError", { error }), AlertVariant.danger);
    }
  };

  const StatusRow = (user: BruteUser) => {
    return (
      <>
        {!user.enabled && (
          <Label key={user.id} color="red" icon={<InfoCircleIcon />}>
            {t("disabled")}
          </Label>
        )}
        {user.brute?.disabled && (
          <Label key={user.id} color="orange" icon={<WarningTriangleIcon />}>
            {t("temporaryDisabled")}
          </Label>
        )}
      </>
    );
  };

  return (
    <>
      <ViewHeader titleKey="users:title" subKey="users:userExplain" />
      <PageSection variant="light">
        <KeycloakDataTable
          key={key}
          loader={loader}
          isPaginated
          ariaLabelKey="users:title"
          searchPlaceholderKey="users:searchForUser"
          toolbarItem={
            <>
              <ToolbarItem>
                <Button>{t("addUser")}</Button>
              </ToolbarItem>
              <ToolbarItem>
                <Button variant={ButtonVariant.plain}>{t("deleteUser")}</Button>
              </ToolbarItem>
            </>
          }
          actions={[
            {
              title: t("common:delete"),
              onRowClick: (user) => {
                deleteUser(user);
              },
            },
          ]}
          columns={[
            {
              name: "username",
              displayKey: "users:username",
            },
            {
              name: "email",
              displayKey: "users:email",
            },
            {
              name: "lastName",
              displayKey: "users:lastName",
            },
            {
              name: "firstName",
              displayKey: "users:firstName",
            },
            {
              name: "status",
              displayKey: "users:status",
              cellRenderer: StatusRow,
            },
          ]}
        />
      </PageSection>
    </>
  );
};
