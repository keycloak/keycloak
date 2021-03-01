import React, { useEffect } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { PageSection } from "@patternfly/react-core";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { boolFormatter, emptyFormatter } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";

export const UsersInRoleTab = () => {
  const { t } = useTranslation("roles");

  const { id } = useParams<{ id: string }>();

  const adminClient = useAdminClient();

  const loader = async () => {
    const role = await adminClient.roles.findOneById({ id: id });
    const usersWithRole = await adminClient.roles.findUsersWithRole({
      name: role.name!,
    });
    return usersWithRole;
  };

  useEffect(() => {
    loader();
  }, []);

  return (
    <>
      <PageSection data-testid="users-page" variant="light">
        <KeycloakDataTable
          loader={loader}
          ariaLabelKey="roles:roleList"
          searchPlaceholderKey=""
          emptyState={
            <ListEmptyState
              hasIcon={true}
              message={t("noDirectUsers")}
              instructions={t("noUsersEmptyStateDescription")}
            />
          }
          columns={[
            {
              name: "username",
              displayKey: "roles:userName",
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "email",
              displayKey: "roles:email",
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "lastName",
              displayKey: "roles:lastName",
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "firstName",
              displayKey: "roles:firstName",
              cellFormatters: [boolFormatter(), emptyFormatter()],
            },
          ]}
        />
      </PageSection>
    </>
  );
};
