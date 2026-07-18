import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import { Button, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { emptyFormatter } from "../util";

type UserRow = UserRepresentation & { onSelect: () => void };

const UsernameLink = ({ username, onSelect }: UserRow) => (
  <Button
    data-testid={`user-detail-link-${username}`}
    variant="link"
    isInline
    onClick={onSelect}
  >
    {username}
  </Button>
);

export const SecondTab = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const [selectedUserId, setSelectedUserId] = useState<string>();

  const loader = async (first?: number, max?: number, search?: string) => {
    const users = await adminClient.users.find({
      first,
      max,
      search,
      briefRepresentation: true,
    });

    return users.map((user) => ({
      ...user,
      onSelect: () => setSelectedUserId(user.id),
    }));
  };

  console.log(selectedUserId);

  return (
    <PageSection data-testid="users-table-tab" variant="light">
      <KeycloakDataTable
        isPaginated
        loader={loader}
        ariaLabelKey="usersTableTab"
        searchPlaceholderKey="searchForUser"
        emptyState={
          <ListEmptyState
            message={t("noUsersFound")}
            instructions={t("emptyInstructions")}
          />
        }
        columns={[
          {
            name: "username",
            displayKey: "username",
            cellRenderer: UsernameLink,
          },
          {
            name: "email",
            displayKey: "email",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellFormatters: [emptyFormatter()],
          },
        ]}
      />
    </PageSection>
  );
};
