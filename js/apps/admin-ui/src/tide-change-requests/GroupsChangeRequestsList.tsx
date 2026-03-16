/** TIDECLOAK IMPLEMENTATION */
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
  ButtonVariant,
  ExpandableSection
} from "@patternfly/react-core";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { useAccess } from '../context/access/Access';
import { useAdminClient } from "../admin-client";
import "../events/events.css";
import { useEnvironment, useAlerts } from '@keycloak/keycloak-ui-shared';
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { findTideComponent } from '../identity-providers/utils/SignSettingsUtil';
import { useRealm } from '../context/realm-context/RealmContext';
import { useWhoAmI } from '../context/whoami/WhoAmI';
import { groupRequestsByDraftId, type BundledRequest } from './utils/bundleUtils';
import { ActivityPanel } from "./ActivityPanel";
import { base64ToBytes, bytesToBase64 } from "./utils/blockchain/tideSerialization";
import { expandRowAndScrollTo } from "./utils/expandAndScroll";


type ChangeRequestProps = {
  updateCounter: (count: number) => void;
  onActionComplete?: () => void;
};

const getEffectiveStatus = (bundle: BundledRequest): string => {
  const statuses = [...new Set(bundle.requests.map((r: any) =>
    r.status === "ACTIVE" ? r.deleteStatus || r.status : r.status
  ))];
  return statuses.length === 1 ? statuses[0] : "MIXED";
};

export const GroupsChangeRequestsList = ({ updateCounter, onActionComplete }: ChangeRequestProps) => {
  const { keycloak, approveTideRequests, } = useEnvironment();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { whoAmI } = useWhoAmI();

  const { t } = useTranslation();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setSelectedRow([])
    setKey((prev: number) => prev + 1);
    onActionComplete?.();
  };
  const [selectedRow, setSelectedRow] = useState<BundledRequest[]>([]);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const { addAlert, addError } = useAlerts();
  const [isTideEnabled, setIsTideEnabled] = useState<boolean>(true);


  useEffect(() => {
    const checkTide = async () => {
      const isTideKeyEnabled = await findTideComponent(adminClient, realm) === undefined ? false : true
      setIsTideEnabled(isTideKeyEnabled)
    }
    checkTide();
  }, [adminClient, realm])

  const hasSelection = selectedRow.length > 0;

  const canApprove = hasSelection && selectedRow.every(b => {
    const s = getEffectiveStatus(b);
    return s === "PENDING" || s === "DRAFT" || s === "MIXED";
  });

  const canCommit = hasSelection && selectedRow.every(b => {
    const s = getEffectiveStatus(b);
    return s === "APPROVED";
  });

  const canCancel = hasSelection && selectedRow.every(b => {
    const s = getEffectiveStatus(b);
    return s !== "ACTIVE" && (!b.requestedByUserId || b.requestedByUserId === whoAmI.userId);
  });

  const ToolbarItemsComponent = () => {
    const { t } = useTranslation();
    const { hasAccess } = useAccess();
    const isManager = hasAccess("manage-clients");

    if (!isManager) return <span />;

    return (
      <>
        <ToolbarItem>
          <Button
            variant="primary"
            isDisabled={!canApprove || isProcessing}
            isLoading={isProcessing}
            onClick={() => handleApproveButtonClick(selectedRow)}
          >
            {isTideEnabled ? t("Review Draft") : t("Approve Draft")}
            {selectedRow.length > 1 ? ` (${selectedRow.length})` : ''}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button
            variant="secondary"
            isDisabled={!canCommit || isProcessing}
            isLoading={isProcessing}
            onClick={() => handleCommitButtonClick(selectedRow)}
          >
            {t("Commit Draft")}
            {selectedRow.length > 1 ? ` (${selectedRow.length})` : ''}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button
            variant="secondary"
            isDanger
            isDisabled={!canCancel || isProcessing}
            onClick={() => toggleCancelDialog()}
          >
            {t("Cancel Draft")}
          </Button>
        </ToolbarItem>
        <CancelConfirm />
      </>
    );
  };

  const handleApproveButtonClick = async (selectedBundles: BundledRequest[]) => {
    setIsProcessing(true);
    try {
      const allRequests = selectedBundles.flatMap(bundle => bundle.requests);

      const changeRequests = allRequests.map(x => ({
        changeSetId: x.draftRecordId,
        changeSetType: x.changeSetType,
        actionType: x.actionType,
      }));

      if (!isTideEnabled) {
        for (const change of changeRequests) {
          await adminClient.tideUsersExt.approveDraftChangeSet({ changeSets: [change] });
        }
        addAlert(t("Change requests approved successfully"), AlertVariant.success);
        refresh();
        return;
      }

      const respObj: any = await adminClient.tideUsersExt.approveDraftChangeSet({
        changeSets: changeRequests,
      });

      if (respObj.length > 0) {
        try {
          const firstRespObj = respObj[0];
          if (firstRespObj.requiresApprovalPopup === true || firstRespObj.requiresApprovalPopup === "true") {
            const respMetaMap: Record<string, { actionType: string; changeSetType: string }> = {};
            const changereqs = respObj.map((resp: any) => {
              respMetaMap[resp.changesetId] = {
                actionType: resp.actionType || allRequests[0].actionType,
                changeSetType: resp.changeSetType || allRequests[0].changeSetType,
              };
              return {
                id: resp.changesetId,
                request: base64ToBytes(resp.changeSetDraftRequests),
              };
            });
            const reviewResponses = await approveTideRequests(changereqs);

            for (const reviewResp of reviewResponses) {
              if (reviewResp.approved) {
                const meta = respMetaMap[reviewResp.id] || { actionType: allRequests[0].actionType, changeSetType: allRequests[0].changeSetType };
                const msg = reviewResp.approved.request;
                const formData = new FormData();
                formData.append("changeSetId", reviewResp.id);
                formData.append("actionType", meta.actionType);
                formData.append("changeSetType", meta.changeSetType);
                formData.append("requests", bytesToBase64(msg));

                await adminClient.tideAdmin.addReview(formData);
              } else if (reviewResp.denied) {
                const meta = respMetaMap[reviewResp.id] || { actionType: allRequests[0].actionType, changeSetType: allRequests[0].changeSetType };
                const formData = new FormData();
                formData.append("changeSetId", reviewResp.id);
                formData.append("actionType", meta.actionType);
                formData.append("changeSetType", meta.changeSetType);

                await adminClient.tideAdmin.addRejection(formData);
              }
            }
            addAlert(t("Change requests reviewed successfully"), AlertVariant.success);
          } else {
            addAlert(t("Change requests approved successfully"), AlertVariant.success);
          }
        } catch (error: any) {
          addAlert(error.responseData, AlertVariant.danger);
        } finally {
          refresh();
        }
      }
    } catch (error: any) {
      addAlert(error.responseData, AlertVariant.danger);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCommitButtonClick = async (selectedBundles: BundledRequest[]) => {
    setIsProcessing(true);
    try {
      const allRequests = selectedBundles.flatMap(bundle => bundle.requests);
      const changeRequests = allRequests.map(x => ({
        changeSetId: x.draftRecordId,
        changeSetType: x.changeSetType,
        actionType: x.actionType,
      }));

      await adminClient.tideUsersExt.commitDraftChangeSet({ changeSets: changeRequests });
      addAlert(t("Change requests committed successfully"), AlertVariant.success);
      refresh();
    } catch (error: any) {
      addAlert(error.responseData, AlertVariant.danger);
    } finally {
      setIsProcessing(false);
    }
  };

  const columns = [
    {
      name: 'Summary',
      displayKey: 'Summary',
      cellRenderer: (bundle: BundledRequest) => {
        if (bundle.requests.length === 1) {
          const request = bundle.requests[0] as any;
          return (
            <div>
              <div className="pf-v5-u-font-weight-bold">
                {request.action}
              </div>
              <div className="pf-v5-u-color-200">
                {request.groupName ? `Group: ${request.groupName}` : ''}
                {request.roleName ? ` • Role: ${request.roleName}` : ''}
                {request.userName ? ` • User: ${request.userName}` : ''}
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
      cellRenderer: (bundle: BundledRequest) => (
        <span>{bundle.requestedBy}</span>
      )
    },
    {
      name: 'Status',
      displayKey: 'Status',
      cellRenderer: (bundle: BundledRequest) => bundleStatusLabel(bundle)
    },
    {
      name: 'Reviews',
      displayKey: 'Reviews',
      cellRenderer: (bundle: BundledRequest) => (
        <div
          className="pf-v5-u-display-flex pf-v5-u-align-items-center"
          style={{ gap: '6px', flexWrap: 'wrap', cursor: 'pointer' }}
          onClick={(e) => expandRowAndScrollTo(e, 'activity-reviews', bundle.draftRecordId)}
          role="button"
          tabIndex={0}
        >
          {bundle.approvalCount > 0 && (
            <Label color="green" isCompact>
              {bundle.approvalCount} approved
            </Label>
          )}
          {bundle.rejectionCount > 0 && (
            <Label color="red" isCompact>
              {bundle.rejectionCount} denied
            </Label>
          )}
          {bundle.approvalCount === 0 && bundle.rejectionCount === 0 && (
            <span className="pf-v5-u-color-200 pf-v5-u-font-size-sm">No reviews</span>
          )}
        </div>
      )
    },
    {
      name: 'Comments',
      displayKey: 'Comments',
      cellRenderer: (bundle: BundledRequest) => (
        <span
          style={{ cursor: 'pointer' }}
          onClick={(e) => expandRowAndScrollTo(e, 'activity-comments', bundle.draftRecordId)}
          role="button"
          tabIndex={0}
        >
          {bundle.commentCount > 0 ? (
            <Label color="blue" isCompact>{bundle.commentCount}</Label>
          ) : (
            <span className="pf-v5-u-color-200 pf-v5-u-font-size-sm">0</span>
          )}
        </span>
      )
    },
  ];

  const bundleStatusLabel = (bundle: BundledRequest) => {
    const status = getEffectiveStatus(bundle);
    const color = status === 'PENDING' ? 'orange'
      : status === 'APPROVED' ? 'blue'
      : status === 'DENIED' ? 'red'
      : status === 'MIXED' ? 'purple'
      : 'grey';
    return (
      <Label color={color} className="keycloak-admin--role-mapping__client-name">
        {status}
      </Label>
    );
  };

  const parseAndFormatJson = (str: string) => {
    try {
      const jsonObject = JSON.parse(str);
      return JSON.stringify(jsonObject, null, 2);
    } catch (e) {
      return 'Invalid JSON';
    }
  };

  const DetailCell = (bundle: BundledRequest) => (
    <>
      <ExpandableSection toggleText="Change Requests" isIndented>
        <Table
          aria-label="Bundle details"
          variant={'compact'}
          borders={false}
          isStriped
        >
          <Thead>
            <Tr>
              <Th width={10}>Action</Th>
              <Th width={10}>Group</Th>
              <Th width={10}>Role</Th>
              <Th width={10}>User</Th>
              <Th width={10}>Type</Th>
              <Th width={10}>Status</Th>
              <Th width={15} modifier="wrap">Affected User</Th>
              <Th width={15} modifier="wrap">Affected Client</Th>
              <Th width={30}>Access Draft</Th>
            </Tr>
          </Thead>
          <Tbody>
            {bundle.requests.map((request: any, index: number) =>
              request.userRecord.map((userRecord: any, userIndex: number) => (
                <Tr key={`${index}-${userIndex}`}>
                  <Td dataLabel="Action">{request.action}</Td>
                  <Td dataLabel="Group">{request.groupName || '-'}</Td>
                  <Td dataLabel="Role">{request.roleName || '-'}</Td>
                  <Td dataLabel="User">{request.userName || '-'}</Td>
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
                  <Td dataLabel="Access Draft">
                    <ClipboardCopy isCode isReadOnly hoverTip="Copy" clickTip="Copied" variant={ClipboardCopyVariant.expansion}>
                      {parseAndFormatJson(userRecord.accessDraft)}
                    </ClipboardCopy>
                  </Td>
                </Tr>
              ))
            )}
          </Tbody>
        </Table>
      </ExpandableSection>
      <ActivityPanel changesetRequestId={bundle.draftRecordId} />
    </>
  );

  const loader = async () => {
    try {
      const requests = await adminClient.tideUsersExt.getRequestedChangesForGroups();
      const bundledRequests = groupRequestsByDraftId(requests);
      updateCounter(bundledRequests.length);
      return bundledRequests;
    } catch (error) {
      console.error("Failed to load group requests:", error);
      updateCounter(0);
      return [];
    }
  };

  const [toggleCancelDialog, CancelConfirm] = useConfirmDialog({
    titleKey: "Cancel Change Request",
    children: (
      <>
        {selectedRow.length > 1
          ? `Are you sure you want to cancel these ${selectedRow.length} change requests?`
          : "Are you sure you want to cancel this change request?"}
      </>
    ),
    continueButtonLabel: "cancel",
    continueButtonVariant: ButtonVariant.danger,
    cancelButtonLabel: "back",

    onConfirm: async () => {
      try {
        const allRequests = selectedRow.flatMap(bundle => bundle.requests);
        const changeSetArray = allRequests.map((row) => ({
          changeSetId: row.draftRecordId,
          changeSetType: row.changeSetType,
          actionType: row.actionType
        }));

        await adminClient.tideUsersExt.cancelDraftChangeSet({ changeSets: changeSetArray });
        addAlert(t("Change request cancelled"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("Error cancelling change request", error);
      }
    },
  });

  return (
    <>
      <div className="keycloak__events_table">
        <KeycloakDataTable
          key={key}
          toolbarItem={<ToolbarItemsComponent />}
          loader={loader}
          ariaLabelKey="Group Change Requests"
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
                <Text>No group change requests found.</Text>
              </TextContent>
            </EmptyState>
          }
        />
      </div>
    </>
  );
};
