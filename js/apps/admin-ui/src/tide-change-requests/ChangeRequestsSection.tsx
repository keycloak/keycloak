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
import { RealmSettingsChangeRequestsList } from './RealmSettingsChangeRequestsList';
import { SettingsChangeRequestsList } from './SettingsChangeRequestsList';
import { groupRequestsByDraftId } from './utils/bundleUtils';
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { useAccess } from '../context/access/Access';
import DraftChangeSetRequest from "@keycloak/keycloak-admin-client/lib/defs/DraftChangeSetRequest"
import { useEnvironment, useAlerts } from '@keycloak/keycloak-ui-shared';
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { findTideComponent } from '../identity-providers/utils/SignSettingsUtil';
import { ApprovalEnclave} from "heimdall-tide";

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
  const [realmSettingsRequestCount, setRealmSettingsRequestCount] = useState(0);
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
            const orkURL = new URL(respObj.uri);
            const heimdall = new ApprovalEnclave({
              homeOrkOrigin: orkURL.origin,
              voucherURL: "",
              signed_client_origin: "",
              vendorId: ""
            }).init([keycloak.tokenParsed!['vuid']], respObj.uri);
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
            heimdall.close();
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
      name: 'Summary',
      displayKey: 'Summary',
      cellRenderer: (bundle: any) => {
        if (bundle.requests.length === 1) {
          const request = bundle.requests[0];
          return (
            <div>
              <div className="pf-v5-u-font-weight-bold">
                {request.action} {request.requestType}
              </div>
              <div className="pf-v5-u-color-200">
                {request.role ? `Role: ${request.role}` : ''} {request.clientId ? `• Client: ${request.clientId}` : ''}
              </div>
            </div>
          );
        } else {
          const actions = [...new Set(bundle.requests.map((r: any) => r.action))];
          const types = [...new Set(bundle.requests.map((r: any) => r.requestType))];
          return (
            <div>
              <div className="pf-v5-u-font-weight-bold">
                {bundle.requests.length} changes: {actions.join(', ')}
              </div>
              <div className="pf-v5-u-color-200">
                {types.join(', ')}
              </div>
            </div>
          );
        }
      }
    },
    {
      name: 'Requested By',
      displayKey: 'Requested By',
      cellRenderer: (bundle: any) => bundle.requestedBy
    },
    {
      name: 'Status',
      displayKey: 'Status',
      cellRenderer: (bundle: any) => bundleStatusLabel(bundle)
    },
  ];

  const bundleStatusLabel = (bundle: any) => {
    const statuses = [...new Set(bundle.requests.map((r: any) => r.status === "ACTIVE" ? r.deleteStatus || r.status : r.status))];
    
    if (statuses.length === 1) {
      const status = statuses[0] as string;
      return (
        <Label 
          color={status === 'PENDING' ? 'orange' : status === 'APPROVED' ? 'blue' : status === 'DENIED' ? 'red' : 'grey'}
          className="keycloak-admin--role-mapping__client-name"
        >
          {status}
        </Label>
      );
    } else {
      return (
        <Label color="purple" className="keycloak-admin--role-mapping__client-name">
          MIXED
        </Label>
      );
    }
  };

  const statusLabel = (row: any) => {
    const status = row.status === "ACTIVE" ? row.deleteStatus || row.status : row.status;
    return (
      <Label 
        color={status === 'PENDING' ? 'orange' : status === 'APPROVED' ? 'blue' : status === 'DENIED' ? 'red' : 'grey'}
        className="keycloak-admin--role-mapping__client-name"
      >
        {status}
      </Label>
    );
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

  const DetailCell = (bundle: any) => (
    <Table
      aria-label="Bundle details"
      variant={'compact'}
      borders={false}
      isStriped
    >
      <Thead>
        <Tr>
          <Th width={15}>Action</Th>
          <Th width={15}>Role</Th>
          <Th width={15}>Client ID</Th>
          <Th width={15}>Type</Th>
          <Th width={10}>Status</Th>
          <Th width={15}>Affected User</Th>
          <Th width={15}>Affected Client</Th>
          <Th width={30}>Access Draft</Th>
        </Tr>
      </Thead>
      <Tbody>
        {bundle.requests.map((request: any, index: number) => 
          request.userRecord.map((userRecord: any, userIndex: number) => (
            <Tr key={`${index}-${userIndex}`}>
              <Td dataLabel="Action">{request.action}</Td>
              <Td dataLabel="Role">{request.role}</Td>
              <Td dataLabel="Client ID">{request.clientId}</Td>
              <Td dataLabel="Type">{request.requestType}</Td>
              <Td dataLabel="Status">{statusLabel(request)}</Td>
              <Td dataLabel="Affected User">{userRecord.username}</Td>
              <Td dataLabel="Affected Client">{userRecord.clientId}</Td>
              <Td dataLabel={columnNames.accessDraft}>
                <ClipboardCopy isCode isReadOnly hoverTip="Copy" clickTip="Copied" variant={ClipboardCopyVariant.expansion}>
                  {parseAndFormatJson(userRecord.accessDraft)}
                </ClipboardCopy>
              </Td>
            
            </Tr>
          ))
        )}
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
  const updateRealmSettingsCounter = (counter: number) => {
    if (counter != realmSettingsRequestCount) {
      setRealmSettingsRequestCount(counter);
    }
  }

  const loadUserRequests = async () => {
    try {
      const userRequest = await adminClient.tideUsersExt.getRequestedChangesForUsers();
      // Update counter with bundle count, not individual request count
      const bundledRequests = groupRequestsByDraftId(userRequest);
      setUserRequestCount(bundledRequests.length);
      return bundledRequests;
    } catch (error) {
      return [];
    }
  };

  const loader = async () => {
    return loadUserRequests();
  };

  const useTab = (tab: ChangeRequestsTab) => {
    return useRoutableTab(toChangeRequests({ realm, tab }))
  };

  const userRequestsTab = useTab("users");
  const roleRequestsTab = useTab("roles");
  const clientRequestsTab = useTab("clients");
  const settingsRequestsTab = useTab("settings");

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


  const updateSettingsCounter = (counter: number) => {
    if (counter !== realmSettingsRequestCount) {
      setRealmSettingsRequestCount(counter);
    }
  };

  return (
    <>
      <ViewHeader
        titleKey="Change Requests"
        subKey="Change requests are change requests that require approval from administrators"
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
            title={
              <>
                <TabTitleText>Users</TabTitleText>
                {userRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm">
                    {userRequestCount}
                  </Label>
                )}
              </>
            }
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
                    enabled: (bundle) => bundle.requests.length > 0,
                    cellRenderer: DetailCell,
                  },
                ]}
                columns={columns}
                isPaginated
                onSelect={(value: any[]) => {
                  // Flatten the selected bundles into individual requests for the toolbar
                  const flattenedRequests = value.flatMap(bundle => bundle.requests);
                  setSelectedRow(flattenedRequests);
                }}
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
            title={
              <>
                <TabTitleText>Roles</TabTitleText>
                {roleRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm">
                    {roleRequestCount}
                  </Label>
                )}
              </>
            }
            {...roleRequestsTab}
          >
            <RolesChangeRequestsList updateCounter={updateRoleCounter} />
          </Tab>
          <Tab
            title={
              <>
                <TabTitleText>Clients</TabTitleText>
                {clientRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm">
                    {clientRequestCount}
                  </Label>
                )}
              </>
            }
            {...clientRequestsTab}
          >
            <ClientChangeRequestsList updateCounter={updateClientCounter} />
          </Tab>
          <Tab
            title={
              <>
                <TabTitleText>Settings</TabTitleText>
                {realmSettingsRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm"> {/* TIDECLOAK IMPLEMENTATION */}
                    {realmSettingsRequestCount}
                  </Label>
                )}
              </>
            }
            {...settingsRequestsTab}
          >
            <SettingsChangeRequestsList updateCounter={updateSettingsCounter} />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
