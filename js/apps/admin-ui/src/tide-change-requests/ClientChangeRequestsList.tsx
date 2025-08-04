import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  TextContent,
  Text,
  EmptyState,
  ClipboardCopy,
  ClipboardCopyVariant,
  Label,
  Button,
  ToolbarItem,
  AlertVariant,
  ButtonVariant
} from "@patternfly/react-core";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import RequestedChanges from "@keycloak/keycloak-admin-client/lib/defs/RequestedChanges"
import RequestChangesUserRecord from "@keycloak/keycloak-admin-client/lib/defs/RequestChangesUserRecord"
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { useAccess } from '../context/access/Access';
import { useAdminClient } from "../admin-client";
import "../events/events.css";
import { useEnvironment, useAlerts } from '@keycloak/keycloak-ui-shared';
import { GenerateDefaultUserContextModal } from './GenerateDefaultUserContextModal';
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from '../context/realm-context/RealmContext';
import { findTideComponent } from '../identity-providers/utils/SignSettingsUtil';
import { ApprovalEnclave} from "heimdall-tide";
import { groupRequestsByDraftId, type BundledRequest } from './utils/bundleUtils';

type ChangeRequestProps = {
  updateCounter: (count: number) => void;
};

export const ClientChangeRequestsList = ({ updateCounter }: ChangeRequestProps) => {
  const { keycloak } = useEnvironment();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setSelectedRow([])
    setKey((prev: number) => prev + 1);
  };
  const [selectedRow, setSelectedRow] = useState<BundledRequest[]>([]);
  const [commitRecord, setCommitRecord] = useState<boolean>(false);
  const [approveRecord, setApproveRecord] = useState<boolean>(false);
  const [showModal, setShowModal] = useState(false);
  const { addAlert, addError } = useAlerts();
  const [isTideEnabled, setIsTideEnabled] = useState<boolean>(true);


  useEffect(() => {
    const checkTide = async () => {
      const isTideKeyEnabled = await findTideComponent(adminClient, realm) === undefined ? false : true
      setIsTideEnabled(isTideKeyEnabled)
    }
    checkTide();

  }, [adminClient, realm])

  const generateClientDefaultUserContext = async (rows: ClientRepresentation[]) => {
    try {
      const clients: string[] = rows.map(r => r.clientId!)
      await adminClient.tideUsersExt.generateDefaultUserContext({ clients });
      refresh();
    } catch (err: any) {
      addAlert(err.responseData, AlertVariant.danger);

    }
  }

  useEffect(() => {
    if (!selectedRow || !selectedRow[0]) {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    const bundle = selectedRow[0];
    const { status } = bundle;

    // Disable both buttons if status is DENIED
    if (status === "DENIED") {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    // Enable Approve button if the bundle is PENDING or DRAFT or MIXED
    if (status === "PENDING" || status === "DRAFT" || status === "MIXED") {
      setApproveRecord(true);
      setCommitRecord(false);
      return;
    }

    // Enable Commit button if status is APPROVED
    if (status === "APPROVED") {
      setCommitRecord(true);
      setApproveRecord(false);
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
        <ToolbarItem>
          <Button variant="secondary" onClick={() => setShowModal(true)}>
            {t("Generate Default User Context")}
          </Button>
        </ToolbarItem>
        <CancelConfirm />
      </>
    );
  };

  const handleApproveButtonClick = async (selectedBundles: BundledRequest[]) => {
    try {
      const allRequests = selectedBundles.flatMap(bundle => bundle.requests);
      const changeRequests = allRequests.map(x => {
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
          const respObj = JSON.parse(response[0])
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
                formData.append("changeSetId", allRequests[0].draftRecordId)
                formData.append("actionType", allRequests[0].actionType);
                formData.append("changeSetType", allRequests[0].changeSetType);
                await adminClient.tideAdmin.addRejection(formData)
              }

              else {

                const authzAuthn = await heimdall.getAuthorizerAuthentication();
                const formData = new FormData();
                formData.append("changeSetId", allRequests[0].draftRecordId)
                formData.append("actionType", allRequests[0].actionType);
                formData.append("changeSetType", allRequests[0].changeSetType);
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

  const handleCommitButtonClick = async (selectedBundles: BundledRequest[]) => {
    try {
      const allRequests = selectedBundles.flatMap(bundle => bundle.requests);
      const changeRequests = allRequests.map(x => {
        return {
          changeSetId: x.draftRecordId,
          changeSetType: x.changeSetType,
          actionType: x.actionType,
        }
      })

      await adminClient.tideUsersExt.commitDraftChangeSet({ changeSets: changeRequests });
      refresh();
      return;
    } catch (error: any) {
      addAlert(error.responseData, AlertVariant.danger);
    }
  };


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
        const allRequests = selectedRow.flatMap(bundle => bundle.requests);
        const changeSetArray = allRequests.map((row) => {
          return {
            changeSetId: row.draftRecordId,
            changeSetType: row.changeSetType,
            actionType: row.actionType
          }
        })

        await adminClient.tideUsersExt.cancelDraftChangeSet({ changeSets: changeSetArray });
        addAlert(t("Change request cancelled"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("Error cancelling change request", error);
      }
    },
  });

  const columns = [
    {
      name: 'Summary',
      displayKey: 'Summary',
      cellRenderer: (bundle: BundledRequest) => {
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
      cellRenderer: (bundle: BundledRequest) => bundle.requestedBy
    },
    {
      name: 'Status',
      displayKey: 'Status',
      cellRenderer: (bundle: BundledRequest) => bundleStatusLabel(bundle)
    },
  ];

  const bundleStatusLabel = (bundle: BundledRequest) => {
    const statuses = [...new Set(bundle.requests.map((r: any) => r.status === "ACTIVE" ? r.deleteStatus || r.status : r.status))];
    
    if (statuses.length === 1) {
      const status = statuses[0];
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

  const DetailCell = (bundle: BundledRequest) => (
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
              <Td dataLabel="Status">
                <Label 
                  color={request.status === 'APPROVED' ? 'blue' : request.status === 'PENDING' ? 'orange' : request.status === 'DENIED' ? 'red' : 'grey'}
                >
                  {request.status === "ACTIVE" ? request.deleteStatus || request.status : request.status}
                </Label>
              </Td>
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

  const loader = async () => {
    try {
      const requests = await adminClient.tideUsersExt.getRequestedChangesForClients();
      const bundledRequests = groupRequestsByDraftId(requests);
      updateCounter(bundledRequests.length);
      return bundledRequests;
    } catch (error) {
      console.error("Failed to load client requests:", error);
      updateCounter(0);
      return [];
    }
  };

  return (
    <>
      {showModal && (
        <GenerateDefaultUserContextModal
          onSubmit={generateClientDefaultUserContext}
          onClose={() => setShowModal(false)}
        />
      )}
      <div className="keycloak__events_table">
        <KeycloakDataTable
          key={key}
          toolbarItem={<ToolbarItemsComponent />}
          isRadio={isTideEnabled}
          loader={loader}
          ariaLabelKey="Client Change Requests"
          detailColumns={[
            {
              name: "details",
              enabled: (bundle) => bundle.requests.length > 0,
              cellRenderer: DetailCell,
            },
          ]}
          columns={columns}
          isPaginated
          onSelect={(value: BundledRequest[]) => setSelectedRow([...value])}
          emptyState={
            <EmptyState variant="lg">
              <TextContent>
                <Text>No requested changes found.</Text>
              </TextContent>
            </EmptyState>
          }
        />
      </div>
    </>
  );
};
