import { useState } from "react";
import {
  Action,
  KeycloakDataTable,
} from "../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../context/realm-context/RealmContext";
import type { OrgRepresentation } from "./routes";
import useOrgFetcher from "./useOrgFetcher";
import { Button, ToolbarItem } from "@patternfly/react-core";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import AddInvitation from "./AddInvitation";
import { useAlerts } from "../components/alert/Alerts";

type OrgInvitationsTypeProps = {
  org: OrgRepresentation;
};

export default function OrgInvitations(props: OrgInvitationsTypeProps) {
  // Table Refresh
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  // Needed State
  const { realm } = useRealm();
  const { getOrgInvitations, deleteOrgInvitation } = useOrgFetcher(realm);
  const { addAlert } = useAlerts();

  const loader = async () => {
    return await getOrgInvitations(props.org.id);
  };

  // Invite User Modal
  const [invitationModalVisibility, setInvitationModalVisibility] =
    useState(false);
  function toggleInvitationModalVisibility() {
    setInvitationModalVisibility(!invitationModalVisibility);
  }

  function DateFormatter(data: any) {
    const date = new Date(data?.createdAt);
    return <div>{date.toLocaleString()}</div>;
  }

  async function removeInvitation(row: any): Promise<boolean> {
    await deleteOrgInvitation(props.org.id, row.id);
    addAlert("Pending invitation removed");
    refresh();
    return true;
  }

  return (
    <>
      {invitationModalVisibility && (
        <AddInvitation
          refresh={refresh}
          org={props.org}
          toggleVisibility={toggleInvitationModalVisibility}
        />
      )}
      <KeycloakDataTable
        data-testid="invitations-org-table"
        key={key}
        loader={loader}
        ariaLabelKey="invitations"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="addInvitation"
              variant="primary"
              onClick={() => setInvitationModalVisibility(true)}
            >
              Invite User
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: "Remove Pending Invitation",
            onRowClick: removeInvitation,
          } as Action<OrgRepresentation>,
        ]}
        columns={[
          {
            name: "email",
            displayKey: "Email",
          },
          {
            name: "createdAt",
            displayKey: "Invited at",
            cellRenderer: DateFormatter,
          },
        ]}
        emptyState={
          <ListEmptyState
            message="No Invitations Found"
            instructions="Please invite a user via email address to see a list of invitations"
            primaryActionText="Invite User"
            onPrimaryAction={() => setInvitationModalVisibility(true)}
          />
        }
      />
    </>
  );
}
