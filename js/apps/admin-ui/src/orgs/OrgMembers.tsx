import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  Action,
  KeycloakDataTable,
} from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useRealm } from "../context/realm-context/RealmContext";
import { AddMember } from "./AddMember";
import type { OrgRepresentation } from "./routes";
import useOrgFetcher from "./useOrgFetcher";
import { Button, ToolbarItem } from "@patternfly/react-core";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { Link } from "react-router-dom";
import { toUser } from "../user/routes/User";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { AssignRoleToMemberModal } from "./modals/AssignRoleToMemberInOrgModal";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";

type OrgMembersTypeProps = {
  org: OrgRepresentation;
};

type MembersOf = UserRepresentation & {
  membership: GroupRepresentation[];
};

export default function OrgMembers({ org }: OrgMembersTypeProps) {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const { getOrgMembers, removeMemberFromOrg, getRolesForOrg } =
    useOrgFetcher(realm);
  const [assignRoleModalOpen, setAssignRoleModalOpen] = useState<
    UserRepresentation | boolean
  >(false);

  const [orgRoles, setOrgRoles] = useState<RoleRepresentation[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      const data = await getRolesForOrg(org.id);
      setOrgRoles(data);
    };

    fetchData();
  }, []);

  const loader = async (): Promise<MembersOf[]> => {
    return await getOrgMembers(org.id);
  };

  const UserDetailLink = (user: MembersOf) => (
    <Link key={user.id} to={toUser({ realm, id: user.id!, tab: "settings" })}>
      {user.username}
    </Link>
  );

  const [addMembersVisibility, setAddMembersVisibility] = useState(false);
  const toggleAddMembersVisibility = () =>
    setAddMembersVisibility(!addMembersVisibility);

  return (
    <>
      {addMembersVisibility && (
        <AddMember
          refresh={refresh}
          orgId={org.id}
          onClose={toggleAddMembersVisibility}
        />
      )}
      {assignRoleModalOpen && (
        <AssignRoleToMemberModal
          orgId={org.id}
          user={assignRoleModalOpen as UserRepresentation}
          handleModalToggle={() => setAssignRoleModalOpen(false)}
          refresh={refresh}
          orgRoles={orgRoles}
        />
      )}
      <KeycloakDataTable
        data-testid="members-org-table"
        key={key}
        loader={loader}
        ariaLabelKey="members"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="addMember"
              variant="primary"
              onClick={toggleAddMembersVisibility}
            >
              Add Member
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: "Assign Role",
            onRowClick: async (user: UserRepresentation): Promise<boolean> => {
              setAssignRoleModalOpen(user);
              return Promise.resolve(true);
            },
          } as Action<UserRepresentation>,
          {
            title: "Remove from Org",
            onRowClick: async (user: UserRepresentation): Promise<boolean> => {
              await removeMemberFromOrg(org.id, user.id!);
              refresh();
              return Promise.resolve(true);
            },
          } as Action<UserRepresentation>,
        ]}
        columns={[
          {
            name: "username",
            displayKey: "Name",
            cellRenderer: UserDetailLink,
          },
          {
            name: "email",
            displayKey: "Email",
          },
          {
            name: "firstName",
            displayKey: "First Name",
          },
          {
            name: "lastName",
            displayKey: "Last Name",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noUsersFound")}
            instructions={t("emptyInstructions")}
            primaryActionText={t("addMember")}
            onPrimaryAction={toggleAddMembersVisibility}
          />
        }
      />
    </>
  );
}
