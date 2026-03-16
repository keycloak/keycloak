/** TIDECLOAK IMPLEMENTATION */
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ToolbarItem,
  EmptyState,
  EmptyStateBody,
  Title,
  TextContent,
  Text,
  Label,
  ButtonVariant,
  AlertVariant,
  ExpandableSection,
} from "@patternfly/react-core";
import {
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
} from "@patternfly/react-table";
import { useAdminClient } from "../admin-client";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../context/access/Access";
import { useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { base64ToBytes, bytesToBase64 } from "./utils/blockchain/tideSerialization";
import { expandRowAndScrollTo } from "./utils/expandAndScroll";
import { ActivityPanel } from "./ActivityPanel";

interface PolicyChangeRequestsListProps {
  updateCounter: (count: number) => void;
  onActionComplete?: () => void;
}

interface PolicyBundle {
  draftRecordId: string;
  requests: PolicyRequest[];
  status: string;
  requestedBy: string;
  requestedByUserId: string;
  approvalCount: number;
  rejectionCount: number;
  approvedBy: string[];
  deniedBy: string[];
  commentCount: number;
  count: number;
}

interface PolicyRequest {
  draftRecordId: string;
  changeSetType: string;
  actionType: string;
  action: string;
  requestType: string;
  status: string;
  deleteStatus?: string;
  templateName?: string;
  templateId?: string;
  timestamp?: number;
}

export const PolicyChangeRequestsList = ({
  updateCounter,
  onActionComplete,
}: PolicyChangeRequestsListProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert } = useAlerts();
  const { whoAmI } = useWhoAmI();
  const [selectedRow, setSelectedRow] = useState<PolicyBundle[]>([]);
  const [key, setKey] = useState<number>(0);
  const [approveRecord, setApproveRecord] = useState<boolean>(false);
  const [commitRecord, setCommitRecord] = useState<boolean>(false);

  const { approveTideRequests } = useEnvironment();

  const refresh = () => {
    setSelectedRow([]);
    setKey((prev: number) => prev + 1);
    onActionComplete?.();
  };

  const loader = async (): Promise<PolicyBundle[]> => {
    try {
      const policy: any = await adminClient.tideUsersExt.getRealmPolicy();

      if (
        !policy ||
        (policy.status !== "pending" && policy.status !== "delete_pending")
      ) {
        updateCounter(0);
        return [];
      }

      // Use changeset approval status from backend (DRAFT/PENDING/APPROVED/DENIED)
      const changesetStatus = policy.changesetStatus || "PENDING";
      const isDelete = policy.status === "delete_pending";

      const bundle: PolicyBundle = {
        draftRecordId: policy.changesetRequestId,
        requests: [
          {
            draftRecordId: policy.changesetRequestId,
            changeSetType: "POLICY",
            actionType: isDelete ? "DELETE" : "CREATE",
            action: isDelete ? "Delete Realm Policy" : "Create Realm Policy",
            requestType: "REALM_POLICY",
            status: changesetStatus,
            templateName: policy.templateName || policy.templateId,
            templateId: policy.templateId,
            timestamp: policy.timestamp,
          },
        ],
        status: changesetStatus,
        requestedBy: policy.requestedByUsername || "",
        requestedByUserId: policy.requestedBy || "",
        approvalCount: policy.approvalCount ?? 0,
        rejectionCount: policy.rejectionCount ?? 0,
        approvedBy: policy.approvedBy ?? [],
        deniedBy: policy.deniedBy ?? [],
        commentCount: policy.commentCount ?? 0,
        count: 1,
      };

      updateCounter(1);
      return [bundle];
    } catch (error) {
      console.error("Failed to load policy change requests:", error);
      updateCounter(0);
      return [];
    }
  };

  useEffect(() => {
    if (!selectedRow || !selectedRow[0]) {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    const firstBundle = selectedRow[0];
    const allRequests = firstBundle.requests;
    if (!allRequests || !allRequests[0]) {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    const { status, deleteStatus } = allRequests[0];

    if (status === "DENIED" || deleteStatus === "DENIED") {
      setApproveRecord(false);
      setCommitRecord(false);
      return;
    }

    if (
      status === "PENDING" ||
      status === "DRAFT" ||
      (status === "ACTIVE" && (deleteStatus === "DRAFT" || deleteStatus === "PENDING"))
    ) {
      setApproveRecord(true);
      setCommitRecord(false);
      return;
    }

    if (status === "APPROVED" || deleteStatus === "APPROVED") {
      setCommitRecord(true);
      setApproveRecord(false);
      return;
    }

    setApproveRecord(false);
    setCommitRecord(false);
  }, [selectedRow]);

  const handleApprove = async () => {
    try {
      if (!selectedRow[0]) return;

      const bundle = selectedRow[0];
      const request = bundle.requests[0];

      const changeRequests = [
        {
          changeSetId: request.draftRecordId,
          changeSetType: request.changeSetType,
          actionType: request.actionType,
        },
      ];

      const respObj: any = await adminClient.tideUsersExt.approveDraftChangeSet(
        { changeSets: changeRequests }
      );

      if (!respObj || respObj.length === 0) {
        refresh();
        return;
      }

      try {
        const firstRespObj = respObj[0];
        if (
          firstRespObj.requiresApprovalPopup === true ||
          firstRespObj.requiresApprovalPopup === "true"
        ) {
          const changereqs = respObj.map((resp: any) => ({
            id: resp.changesetId,
            request: base64ToBytes(resp.changeSetDraftRequests),
          }));
          const reviewResponses = await approveTideRequests(changereqs);

          for (const reviewResp of reviewResponses) {
            if (reviewResp.approved) {
              const msg = reviewResp.approved.request;
              const formData = new FormData();
              formData.append("changeSetId", reviewResp.id);
              formData.append("actionType", changeRequests[0].actionType);
              formData.append("changeSetType", changeRequests[0].changeSetType);
              formData.append("requests", bytesToBase64(msg));

              await adminClient.tideAdmin.addReview(formData);
            } else if (reviewResp.denied) {
              const formData = new FormData();
              formData.append("changeSetId", reviewResp.id);
              formData.append("actionType", changeRequests[0].actionType);
              formData.append("changeSetType", changeRequests[0].changeSetType);

              await adminClient.tideAdmin.addRejection(formData);
            }
          }
        }
      } catch (error: any) {
        addAlert(
          error.responseData || "Failed to review policy change request",
          AlertVariant.danger
        );
      } finally {
        refresh();
      }
    } catch (error: any) {
      addAlert(
        error.responseData || "Failed to approve policy request",
        AlertVariant.danger
      );
    }
  };

  const handleCommit = async () => {
    try {
      if (!selectedRow[0]) return;
      const bundle = selectedRow[0];
      const request = bundle.requests[0];

      if (request.actionType === "DELETE") {
        await adminClient.tideUsersExt.commitDeleteRealmPolicy();
        addAlert("Realm policy deleted successfully", AlertVariant.success);
      } else {
        await adminClient.tideUsersExt.commitRealmPolicy();
        addAlert("Realm policy committed and active", AlertVariant.success);
      }
      refresh();
    } catch (error: any) {
      addAlert(
        error.responseData || "Failed to commit realm policy change",
        AlertVariant.danger
      );
    }
  };

  const [toggleCancelDialog, CancelConfirm] = useConfirmDialog({
    titleKey: "Cancel Policy Change Request",
    children: (
      <>Are you sure you want to cancel this policy change request?</>
    ),
    continueButtonLabel: "cancel",
    cancelButtonLabel: "back",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.tideUsersExt.deleteRealmPolicy();
        addAlert("Policy change request cancelled", AlertVariant.success);
        refresh();
      } catch (error) {
        addAlert(
          "Error cancelling policy change request",
          AlertVariant.danger
        );
      }
    },
  });

  const bundleStatusLabel = (bundle: PolicyBundle) => {
    const status = bundle.status;
    const color =
      status === "PENDING"
        ? "orange"
        : status === "APPROVED"
        ? "blue"
        : status === "DENIED"
        ? "red"
        : "grey";
    return (
      <Label
        color={color as any}
        className="keycloak-admin--role-mapping__client-name"
      >
        {status}
      </Label>
    );
  };

  const statusLabel = (row: PolicyRequest) => {
    const status = row.status;
    const color =
      status === "PENDING"
        ? "orange"
        : status === "APPROVED"
        ? "blue"
        : status === "DENIED"
        ? "red"
        : "grey";
    return (
      <Label
        color={color as any}
        className="keycloak-admin--role-mapping__client-name"
      >
        {status}
      </Label>
    );
  };

  const DetailCell = (bundle: PolicyBundle) => (
    <>
      <ExpandableSection toggleText="Change Requests" isIndented>
        <Table
          aria-label="Policy details"
          variant="compact"
          borders={false}
          isStriped
        >
          <Thead>
            <Tr>
              <Th width={20}>Action</Th>
              <Th width={20}>Template</Th>
              <Th width={15}>Change Set Type</Th>
              <Th width={15}>Status</Th>
              <Th width={30}>Created</Th>
            </Tr>
          </Thead>
          <Tbody>
            {bundle.requests.map((request: PolicyRequest, index: number) => (
              <Tr key={index}>
                <Td dataLabel="Action">{request.action}</Td>
                <Td dataLabel="Template">
                  {request.templateName || request.templateId || "—"}
                </Td>
                <Td dataLabel="Change Set Type">{request.changeSetType}</Td>
                <Td dataLabel="Status">{statusLabel(request)}</Td>
                <Td dataLabel="Created">
                  {request.timestamp
                    ? new Date(request.timestamp).toLocaleString()
                    : "—"}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </ExpandableSection>
      <ActivityPanel changesetRequestId={bundle.draftRecordId} />
    </>
  );

  const columns = [
    {
      name: "Summary",
      displayKey: "Summary",
      cellRenderer: (bundle: PolicyBundle) => {
        const request = bundle.requests[0];
        return (
          <div>
            <div className="pf-v5-u-font-weight-bold">{request.action}</div>
            <div className="pf-v5-u-color-200">
              Template: {request.templateName || request.templateId || "—"}
            </div>
          </div>
        );
      },
    },
    {
      name: "Status",
      displayKey: "Status",
      cellRenderer: (bundle: PolicyBundle) => bundleStatusLabel(bundle),
    },
    {
      name: "Reviews",
      displayKey: "Reviews",
      cellRenderer: (bundle: PolicyBundle) => (
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
      ),
    },
    {
      name: "Comments",
      displayKey: "Comments",
      cellRenderer: (bundle: PolicyBundle) => (
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
      ),
    },
  ];

  const ToolbarItemsComponent = () => {
    const { hasAccess } = useAccess();
    const isManager = hasAccess("manage-clients");
    if (!isManager) return <span />;

    return (
      <>
        <ToolbarItem>
          <Button
            variant="primary"
            isDisabled={!approveRecord}
            onClick={handleApprove}
          >
            {t("Review Draft")}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button
            variant="secondary"
            isDisabled={!commitRecord}
            onClick={handleCommit}
          >
            {t("Commit Draft")}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button
            variant="secondary"
            isDanger
            isDisabled={!selectedRow.length || !selectedRow.every(b => !b.requestedByUserId || b.requestedByUserId === whoAmI.userId)}
            onClick={() => toggleCancelDialog()}
          >
            {t("Cancel Draft")}
          </Button>
        </ToolbarItem>
        <CancelConfirm />
      </>
    );
  };

  return (
    <>
      <div className="keycloak__events_table">
        <KeycloakDataTable
          key={key}
          toolbarItem={<ToolbarItemsComponent />}
          isRadio
          loader={loader}
          ariaLabelKey="Policy Change Requests"
          detailColumns={[
            {
              name: "details",
              cellRenderer: DetailCell,
            },
          ]}
          columns={columns}
          isPaginated
          onSelect={(value: PolicyBundle[]) => setSelectedRow([...value])}
          emptyState={
            <EmptyState variant="lg">
              <Title headingLevel="h4" size="lg">
                No policy change requests
              </Title>
              <EmptyStateBody>
                <TextContent>
                  <Text>
                    No pending policy change requests found. Create a realm
                    policy from the Realm Settings → Realm Policy tab.
                  </Text>
                </TextContent>
              </EmptyStateBody>
            </EmptyState>
          }
        />
      </div>

      <CancelConfirm />
    </>
  );
};
