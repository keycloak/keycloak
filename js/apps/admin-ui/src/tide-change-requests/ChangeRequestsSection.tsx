import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  TextContent,
  Text,
  EmptyState,
  PageSection,
  Tab,
  TabTitleText,
  ClipboardCopy,
  ClipboardCopyVariant,
  Label,
  Button,
  ToolbarItem,
  AlertVariant,
  ButtonVariant
} from "@patternfly/react-core";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import RoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/RoleChangeRequest"
import RequestChangesUserRecord from "@keycloak/keycloak-admin-client/lib/defs/RequestChangesUserRecord"
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../admin-client";
import "../events/events.css";
import helpUrls from '../help-urls';
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ChangeRequestsTab, toChangeRequests } from './routes/ChangeRequests';
import { useRealm } from "../context/realm-context/RealmContext";
import { RolesChangeRequestsList } from "./RolesChangeRequestsList"
import { ClientChangeRequestsList } from './ClientChangeRequestsList';
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { useAccess } from '../context/access/Access';
import DraftChangeSetRequest from "@keycloak/keycloak-admin-client/lib/defs/DraftChangeSetRequest"
import { useEnvironment, useAlerts } from '@keycloak/keycloak-ui-shared';
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { findTideComponent } from '../identity-providers/utils/SignSettingsUtil';
import { importHeimdall } from './HeimdallHelper';

export interface changeSetApprovalRequest {
  message: string,
  uri: string,
  changeSetRequests: string,
  requiresApprovalPopup: string,
  expiry: string
}

export default function ChangeRequestsSection() {
  const { adminClient } = useAdminClient();
  const { keycloak } = useEnvironment();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const [key, setKey] = useState<number>(0);
  const refresh = () => {
    setSelectedRow([])
    setKey((prev: number) => prev + 1);
  };


  const [selectedRow, setSelectedRow] = useState<RoleChangeRequest[]>([]);
  const [commitRecord, setCommitRecord] = useState<boolean>(false);
  const [approveRecord, setApproveRecord] = useState<boolean>(false);
  const [userRequestCount, setUserRequestCount] = useState(0);
  const [roleRequestCount, setRoleRequestCount] = useState(0);
  const [clientRequestCount, setClientRequestCount] = useState(0);
  const [isTideEnabled, setIsTideEnabled] = useState<boolean>(true)


  useEffect(() => {
    const checkTide = async () => {
      const isTideKeyEnabled = await findTideComponent(adminClient, realm) === undefined ? false : true
      setIsTideEnabled(isTideKeyEnabled)
    }
    checkTide();

  }, [adminClient, realm])

  useEffect(() => {
    if (!selectedRow || !selectedRow[0]) {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    const { status, deleteStatus } = selectedRow[0];

    // Disable both buttons if status is DENIED
    if (status === "DENIED" || deleteStatus === "DENIED") {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    // Enable Approve button if the record is PENDING or DRAFT
    // Or if the record is ACTIVE and deleteStatus is DRAFT or PENDING
    if (
      status === "PENDING" ||
      status === "DRAFT" ||
      (status === "ACTIVE" && (deleteStatus === "DRAFT" || deleteStatus === "PENDING"))
    ) {
      setApproveRecord(true);
      setCommitRecord(false); // Ensure commit is off when approve is on
      return;
    }

    // Enable Commit button if status or deleteStatus is APPROVED
    if (status === "APPROVED" || deleteStatus === "APPROVED") {
      setCommitRecord(true);
      setApproveRecord(false); // Ensure approve is off when commit is on
      return;
    }

    // Default: Disable both buttons
    setApproveRecord(false);
    setCommitRecord(false);
  }, [selectedRow]);

  const ToolbarItemsComponent = () => {
    const { t } = useTranslation();
    const { hasAccess } = useAccess();
    const isManager = hasAccess("manage-clients");

    if (!isManager) return <span />;

    return (
      <>
        <ToolbarItem>
          <Button variant="primary" isDisabled={!approveRecord} onClick={() => handleApproveButtonClick(selectedRow)}>
            {isTideEnabled ? t("Review Draft") : t("Approve Draft")}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button variant="secondary" isDisabled={!commitRecord} onClick={() => handleCommitButtonClick(selectedRow)}>
            {t("Commit Draft")}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button variant="secondary" isDanger onClick={() => toggleCancelDialog()}>
            {t("Cancel Draft")}
          </Button>
        </ToolbarItem>
        <CancelConfirm />
      </>
    );
  };

  const handleApproveButtonClick = async (selectedRow: RoleChangeRequest[]) => {
    try {
      const changeRequests = selectedRow.map(x => {
        return {
          changeSetId: x.draftRecordId,
          changeSetType: x.changeSetType,
          actionType: x.actionType,
        }
      })
      if (!isTideEnabled) {
        changeRequests.forEach(async (change) => {
          await adminClient.tideUsersExt.approveDraftChangeSet({ changeSets: [change] });
          refresh()
        })
      } else {
        const response: string[] = await adminClient.tideUsersExt.approveDraftChangeSet({ changeSets: changeRequests });

        if (response.length === 1) {
          const respObj = JSON.parse(response[0]);
          if (respObj.requiresApprovalPopup === "true") {
            const module = await importHeimdall();
            if(module === null){
                addAlert("Heimdall module no provided", AlertVariant.danger);
              return
            }
            const heimdall = new module.Heimdall(respObj.uri, [keycloak.tokenParsed!['vuid']])
            await heimdall.openEnclave();
            const authApproval = await heimdall.getAuthorizerApproval(respObj.changeSetRequests, "UserContext:1", respObj.expiry, "base64url");

            if (authApproval.draft === respObj.changeSetRequests) {
              if (authApproval.accepted === false) {
                const formData = new FormData();
                formData.append("changeSetId", selectedRow[0].draftRecordId)
                formData.append("actionType", selectedRow[0].actionType);
                formData.append("changeSetType", selectedRow[0].changeSetType);
                await adminClient.tideAdmin.addRejection(formData)
              }
              else {
                const authzAuthn = await heimdall.getAuthorizerAuthentication();
                const formData = new FormData();
                formData.append("changeSetId", selectedRow[0].draftRecordId)
                formData.append("actionType", selectedRow[0].actionType);
                formData.append("changeSetType", selectedRow[0].changeSetType);
                formData.append("authorizerApproval", authApproval.data);
                formData.append("authorizerAuthentication", authzAuthn);
                await adminClient.tideAdmin.addAuthorization(formData)
              }
            }
            heimdall.closeEnclave();
          }
          refresh();
        }
      }
    } catch (error: any) {
      addAlert(error.responseData, AlertVariant.danger);
    }

  };

  const handleCommitButtonClick = async (selectedRow: RoleChangeRequest[]) => {
    try {
      const changeRequests = selectedRow.map(x => {
        return {
          changeSetId: x.draftRecordId,
          changeSetType: x.changeSetType,
          actionType: x.actionType,
        }
      })
      await adminClient.tideUsersExt.commitDraftChangeSet({ changeSets: changeRequests });
      refresh();
    } catch (error: any) {
      addAlert(error.responseData, AlertVariant.danger);
    }
  };

  const columns = [
    {
      name: 'Action',
      displayKey: 'Action',
      cellRenderer: (row: RoleChangeRequest) => row.action
    },
    {
      name: 'Role',
      displayKey: 'Role',
      cellRenderer: (row: RoleChangeRequest) => row.role
    },
    {
      name: 'Client ID',
      displayKey: 'Client ID',
      cellRenderer: (row: RoleChangeRequest) => row.clientId
    },
    {
      name: 'Type',
      displayKey: 'Type',
      cellRenderer: (row: RoleChangeRequest) => row.requestType
    },
    {
      name: 'Status',
      displayKey: 'Status',
      cellRenderer: (row: RoleChangeRequest) => statusLabel(row)
    },
  ];

  const statusLabel = (row: any) => {
    return (
      <>
        {(row.status === "DRAFT" || row.deleteStatus === "DRAFT") && (
          <Label className="keycloak-admin--role-mapping__client-name">
            {"DRAFT"}
          </Label>
        )}
        {(row.status === "PENDING" || row.deleteStatus === "PENDING") && (
          <Label color="orange" className="keycloak-admin--role-mapping__client-name">
            {"PENDING"}
          </Label>
        )}
        {(row.status === "APPROVED" || row.deleteStatus === "APPROVED") && (
          <Label color="blue" className="keycloak-admin--role-mapping__client-name">
            {"APPROVED"}
          </Label>
        )}
        {(row.status === "DENIED" || row.deleteStatus === "DENIED") && (
          <Label color="red" className="keycloak-admin--role-mapping__client-name">
            {"DENIED"}
          </Label>
        )}
      </>
    )
  }

  const parseAndFormatJson = (str: string) => {
    try {
      // Parse the JSON string
      const jsonObject = JSON.parse(str);
      // Format the JSON object into a readable string with indentation
      return JSON.stringify(jsonObject, null, 2);
    } catch (e) {
      return 'Invalid JSON';
    }
  };

  const columnNames = {
    username: 'Affected User',
    clientId: 'Affected Client',
    accessDraft: 'Access Draft',
  };

  const DetailCell = (row: RoleChangeRequest) => (
    <Table
      aria-label="Simple table"
      variant={'compact'}
      borders={false}
      isStriped
    >
      <Thead>
        <Tr>
          <Th width={20} modifier="wrap">{columnNames.username}</Th>
          <Th width={20} modifier="wrap">{columnNames.clientId}</Th>
          <Th width={40}>{columnNames.accessDraft}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {row.userRecord.map((value: RequestChangesUserRecord) => (
          <Tr key={value.username}>
            <Td dataLabel={columnNames.username}>{value.username}</Td>
            <Td dataLabel={columnNames.clientId}>{value.clientId}</Td>
            <Td dataLabel={columnNames.accessDraft}>
              <ClipboardCopy isCode isReadOnly hoverTip="Copy" clickTip="Copied" variant={ClipboardCopyVariant.expansion}>
                {parseAndFormatJson(value.accessDraft)}
              </ClipboardCopy>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  );

  const updateClientCounter = (counter: number) => {
    if (counter != clientRequestCount) {
      setClientRequestCount(counter);
    }
  }
  const updateRoleCounter = (counter: number) => {
    if (counter != roleRequestCount) {
      setRoleRequestCount(counter);
    }
  }

  const loader = async () => {
    try {
      const userRequest = await adminClient.tideUsersExt.getRequestedChangesForUsers();
      const roleRequest = await adminClient.tideUsersExt.getRequestedChangesForRoles();
      const clientRequest = await adminClient.tideUsersExt.getRequestedChangesForClients();

      setUserRequestCount(userRequest.length)
      setRoleRequestCount(roleRequest.length)
      setClientRequestCount(clientRequest.length)

      return userRequest;
    } catch (error) {
      return [];
    }
  };

  const useTab = (tab: ChangeRequestsTab) => {
    return useRoutableTab(toChangeRequests({ realm, tab }))
  };

  const userRequestsTab = useTab("users");
  const roleRequestsTab = useTab("roles");
  const clientRequestsTab = useTab("clients");

  const [toggleCancelDialog, CancelConfirm] = useConfirmDialog({
    titleKey: "Cancel Change Request",
    children: (
      <>
        {"Are you sure you want to cancel this change request?"}
      </>
    ),
    continueButtonLabel: "cancel",
    cancelButtonLabel: "back",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        const changeSetArray = selectedRow.map((row: { draftRecordId: any; changeSetType: any; actionType: any; }) => {
          return {
            changeSetId: row.draftRecordId,
            changeSetType: row.changeSetType,
            actionType: row.actionType
          }
        })

        await adminClient.tideUsersExt.cancelDraftChangeSet({changeSets: changeSetArray});
        addAlert(t("Change request cancelled"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("Error cancelling change request", error);
      }
    },
  });


  return (
    <>
      <ViewHeader
        titleKey="Change Requests"
        subKey="Change requests are change requests that require approval from adminstrators"
        helpUrl={helpUrls.changeRequests}
        divider={false}
      />
      <PageSection
        data-testid="change-request-page"
        variant="light"
        className="pf-v5-u-p-0"
      >
        <RoutableTabs
          mountOnEnter
          isBox
          defaultLocation={toChangeRequests({ realm, tab: "users" })}
        >
          <Tab
            title={<><TabTitleText>Users</TabTitleText><Label className="keycloak-admin--role-mapping__client-name">
              {userRequestCount}
            </Label></>}
            {...userRequestsTab}
          >
            <div className="keycloak__events_table">
              <KeycloakDataTable
                key={key}
                toolbarItem={<ToolbarItemsComponent />}
                isRadio={isTideEnabled}
                loader={loader}
                ariaLabelKey="Requested Changes"
                detailColumns={[
                  {
                    name: "details",
                    enabled: (row) => row.userRecord.length > 0,
                    cellRenderer: DetailCell,
                  },
                ]}
                columns={columns}
                isPaginated
                onSelect={(value: RoleChangeRequest[]) => setSelectedRow([...value])}
                emptyState={
                  <EmptyState variant="lg">
                    <TextContent>
                      <Text>No requested changes found.</Text>
                    </TextContent>
                  </EmptyState>
                }
              />
            </div>
          </Tab>
          <Tab
            title={<><TabTitleText>Roles</TabTitleText><Label className="keycloak-admin--role-mapping__client-name">
              {roleRequestCount}
            </Label></>}
            {...roleRequestsTab}
          >
            <RolesChangeRequestsList updateCounter={updateRoleCounter} />
          </Tab>
          <Tab
            title={<><TabTitleText>Clients</TabTitleText><Label className="keycloak-admin--role-mapping__client-name">
              {clientRequestCount}
            </Label></>}
            {...clientRequestsTab}
          >
            <ClientChangeRequestsList updateCounter={updateClientCounter} />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
