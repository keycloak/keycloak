/** TIDECLOAK IMPLEMENTATION */
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
  ButtonVariant,
  ExpandableSection
} from "@patternfly/react-core";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import type { BundledRequest } from './utils/bundleUtils';
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
import { SettingsChangeRequestsList } from './SettingsChangeRequestsList';
import { PolicyChangeRequestsList } from './PolicyChangeRequestsList';
import { GroupsChangeRequestsList } from './GroupsChangeRequestsList';
import { groupRequestsByDraftId } from './utils/bundleUtils';
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { useAccess } from '../context/access/Access';
import { useEnvironment, useAlerts } from '@keycloak/keycloak-ui-shared';
import { useWhoAmI } from '../context/whoami/WhoAmI';
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { findTideComponent } from '../identity-providers/utils/SignSettingsUtil';
import { base64ToBytes, bytesToBase64 } from "./utils/blockchain/tideSerialization";
import { ActivityPanel } from "./ActivityPanel";
import { expandRowAndScrollTo } from "./utils/expandAndScroll";

export interface changeSetApprovalRequest {
  message: string,
  uri: string,
  changeSetRequests: string,
  requiresApprovalPopup: string,
  expiry: string
}

export default function ChangeRequestsSection() {
  const { adminClient } = useAdminClient();
  const { keycloak, approveTideRequests, } = useEnvironment();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const [key, setKey] = useState<number>(0);
  const refresh = () => {
    setSelectedRow([])
    setKey((prev: number) => prev + 1);
    fetchAllCounts();
  };


  const [selectedRow, setSelectedRow] = useState<BundledRequest[]>([]);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [userRequestCount, setUserRequestCount] = useState(0);
  const [roleRequestCount, setRoleRequestCount] = useState(0);
  const [clientRequestCount, setClientRequestCount] = useState(0);
  const [realmSettingsRequestCount, setRealmSettingsRequestCount] = useState(0);
  const [groupRequestCount, setGroupRequestCount] = useState(0);
  const [policyRequestCount, setPolicyRequestCount] = useState(0);
  const [isTideEnabled, setIsTideEnabled] = useState<boolean>(true)


  const fetchAllCounts = async () => {
    try {
      const [counts, settings, licensing, policy] = await Promise.all([
        adminClient.tideUsersExt.getChangeSetCounts().catch(() => null),
        adminClient.tideUsersExt.getRequestedChangesForRagnarokSettings().catch(() => []),
        adminClient.tideUsersExt.getRequestedChangesForRealmLicensing().catch(() => []),
        adminClient.tideUsersExt.getRealmPolicy().catch(() => null),
      ]);

      if (counts) {
        setUserRequestCount(counts.users);
        setRoleRequestCount(counts.roles);
        setClientRequestCount(counts.clients);
        setGroupRequestCount(counts.groups);
      }

      const mergedSettings = [
        ...(Array.isArray(settings) ? settings : []),
        ...(Array.isArray(licensing) ? licensing : []),
      ];
      setRealmSettingsRequestCount(groupRequestsByDraftId(mergedSettings as any[]).length);
      const p = policy as any;
      setPolicyRequestCount(
        p && (p.status === "pending" || p.status === "delete_pending") ? 1 : 0
      );
    } catch {}
  };

  useEffect(() => {
    fetchAllCounts();
    const interval = setInterval(fetchAllCounts, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const checkTide = async () => {
      const isTideKeyEnabled = await findTideComponent(adminClient, realm) === undefined ? false : true
      setIsTideEnabled(isTideKeyEnabled)
    }
    checkTide();

  }, [adminClient, realm])

  const getEffectiveStatus = (bundle: BundledRequest): string => {
    const statuses = [...new Set(bundle.requests.map((r: any) =>
      r.status === "ACTIVE" ? r.deleteStatus || r.status : r.status
    ))];
    return statuses.length === 1 ? statuses[0] : "MIXED";
  };

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
      cellRenderer: (bundle: any) => {
        if (bundle.requests.length === 1) {
          const request = bundle.requests[0];
          return (
            <div>
              <div className="pf-v5-u-font-weight-bold">
                {request.action}
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
      cellRenderer: (bundle: BundledRequest) => (
        <span>{bundle.requestedBy}</span>
      )
    },
    {
      name: 'Status',
      displayKey: 'Status',
      cellRenderer: (bundle: any) => bundleStatusLabel(bundle)
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
              <Th width={10}>Role</Th>
              <Th width={10}>Client ID</Th>
              <Th width={10}>Type</Th>
              <Th width={10}>Status</Th>
              <Th width={15} modifier="wrap">Affected User</Th>
              <Th width={15} modifier="wrap">Affected Client</Th>
              <Th width={40}>Access Draft</Th>
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
      </ExpandableSection>
      <ActivityPanel changesetRequestId={bundle.draftRecordId} />
    </>
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
  const updateGroupCounter = (counter: number) => {
    if (counter != groupRequestCount) {
      setGroupRequestCount(counter);
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
  const groupRequestsTab = useTab("groups");
  const clientRequestsTab = useTab("clients");
  const settingsRequestsTab = useTab("settings");
  const policiesTab = useTab("policies");

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


  const updateSettingsCounter = (counter: number) => {
    if (counter !== realmSettingsRequestCount) {
      setRealmSettingsRequestCount(counter);
    }
  };
  const updatePolicyCounter = (counter: number) => {
    if (counter !== policyRequestCount) {
      setPolicyRequestCount(counter);
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
            <RolesChangeRequestsList updateCounter={updateRoleCounter} onActionComplete={fetchAllCounts} />
          </Tab>
          <Tab
            title={
              <>
                <TabTitleText>Groups</TabTitleText>
                {groupRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm">
                    {groupRequestCount}
                  </Label>
                )}
              </>
            }
            {...groupRequestsTab}
          >
            <GroupsChangeRequestsList updateCounter={updateGroupCounter} onActionComplete={fetchAllCounts} />
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
            <ClientChangeRequestsList updateCounter={updateClientCounter} onActionComplete={fetchAllCounts} />
          </Tab>
          <Tab
            title={
              <>
                <TabTitleText>Settings</TabTitleText>
                {realmSettingsRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm">
                    {realmSettingsRequestCount}
                  </Label>
                )}
              </>
            }
            {...settingsRequestsTab}
          >
            <SettingsChangeRequestsList updateCounter={updateSettingsCounter} onActionComplete={fetchAllCounts} />
          </Tab>
          <Tab
            title={
              <>
                <TabTitleText>Policies</TabTitleText>
                {policyRequestCount > 0 && (
                  <Label className="keycloak-admin--role-mapping__client-name pf-v5-u-ml-sm">
                    {policyRequestCount}
                  </Label>
                )}
              </>
            }
            {...policiesTab}
          >
            <PolicyChangeRequestsList updateCounter={updatePolicyCounter} onActionComplete={fetchAllCounts} />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
