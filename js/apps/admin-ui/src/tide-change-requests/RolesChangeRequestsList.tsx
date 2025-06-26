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
import RequestChangesUserRecord from "@keycloak/keycloak-admin-client/lib/defs/RequestChangesUserRecord"
import CompositeRoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/CompositeRoleChangeRequest"
import RoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/RoleChangeRequest"
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { useAccess } from '../context/access/Access';
import { useAdminClient } from "../admin-client";
import "../events/events.css";
import { useEnvironment, useAlerts } from '@keycloak/keycloak-ui-shared';
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { findTideComponent } from '../identity-providers/utils/SignSettingsUtil';
import { useRealm } from '../context/realm-context/RealmContext';
import { importHeimdall } from './HeimdallHelper';


type ChangeRequestProps = {
  updateCounter: (count: number) => void;
};

export const RolesChangeRequestsList = ({ updateCounter }: ChangeRequestProps) => {
  const { keycloak } = useEnvironment();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const { t } = useTranslation();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setSelectedRow([])
    setKey((prev: number) => prev + 1);
  };
  const [selectedRow, setSelectedRow] = useState<CompositeRoleChangeRequest[] | RoleChangeRequest[]>([]);
  const [commitRecord, setCommitRecord] = useState<boolean>(false);
  const [approveRecord, setApproveRecord] = useState<boolean>(false);
  const { addAlert, addError } = useAlerts();
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

      if(!isTideEnabled){
        changeRequests.forEach(async (change) => {
          await adminClient.tideUsersExt.approveDraftChangeSet({changeSets: [change]});
          refresh();
        })
      } else{
        const response: string[] = await adminClient.tideUsersExt.approveDraftChangeSet({changeSets: changeRequests});
  
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

      await adminClient.tideUsersExt.commitDraftChangeSet({changeSets: changeRequests});
      refresh();
      return;
    } catch (error: any) {
      addAlert(error.responseData, AlertVariant.danger);
    }
  };

  function isCompositeRoleChangeRequest(row: RoleChangeRequest | CompositeRoleChangeRequest): row is CompositeRoleChangeRequest {
    return 'compositeRole' in row;
  }

  const columns = [
    {
      name: t('Action'),
      displayKey: 'Action',
      cellRenderer: (row: RoleChangeRequest | CompositeRoleChangeRequest) => row.action
    },
    {
      name: t('Role'),
      displayKey: 'Role',
      cellRenderer: (row: RoleChangeRequest | CompositeRoleChangeRequest) => row.role
    },
    {
      name: 'Composite Role',
      displayKey: 'Composite Role',
      cellRenderer: (row: RoleChangeRequest | CompositeRoleChangeRequest) => isCompositeRoleChangeRequest(row) ? row.compositeRole || '' : '',
      shouldDisplay: (row: RoleChangeRequest | CompositeRoleChangeRequest) => isCompositeRoleChangeRequest(row),
    },
    {
      name: t('Client ID'),
      displayKey: 'Client ID',
      cellRenderer: (row: RoleChangeRequest | CompositeRoleChangeRequest) => row.clientId
    },
    {
      name: t('Type'),
      displayKey: 'Type',
      cellRenderer: (row: RoleChangeRequest | CompositeRoleChangeRequest) => row.requestType
    },
    {
      name: t('Status'),
      displayKey: 'Status',
      cellRenderer: (row: RoleChangeRequest | CompositeRoleChangeRequest) => statusLabel(row)
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

  const DetailCell = (row: RoleChangeRequest | CompositeRoleChangeRequest) => (
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

  const loader = async () => {
    try {
      const roleRequest = await adminClient.tideUsersExt.getRequestedChangesForRoles();
      updateCounter(roleRequest.length)
      return roleRequest
    } catch (error) {
      return [];
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
    continueButtonVariant: ButtonVariant.danger,
    cancelButtonLabel: "back",

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
      <KeycloakDataTable
        toolbarItem={<ToolbarItemsComponent />}
        isSearching={false}
        key={key}
        isRadio={isTideEnabled}
        loader={loader}
        ariaLabelKey="roleChangeRequestsList"
        detailColumns={[
          {
            name: "details",
            enabled: (row) => row.userRecord.length > 0,
            cellRenderer: DetailCell,
          },
        ]}
        columns={columns}
        isPaginated
        onSelect={(value: RoleChangeRequest[] | CompositeRoleChangeRequest[]) => setSelectedRow([...value])}
        emptyState={
          <EmptyState variant="lg">
            <TextContent>
              <Text>No requested changes found.</Text>
            </TextContent>
          </EmptyState>
        }
      />
    </>

  );
};
