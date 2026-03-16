/** TIDECLOAK IMPLEMENTATION */
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ToolbarItem,
  EmptyState,
  EmptyStateBody,
  Title,
  Spinner,
  TextContent,
  Text,
  Label,
  ButtonVariant,
  AlertVariant,
  Modal,
  ModalVariant,
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
import { CogIcon } from "@patternfly/react-icons";
import { useAdminClient } from "../admin-client";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../context/access/Access";
import { useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { groupRequestsByDraftId, BundledRequest } from "./utils/bundleUtils";
import { useCurrentUser } from "../utils/useCurrentUser";
import { base64ToBytes, bytesToBase64 } from "./utils/blockchain/tideSerialization";
import { expandRowAndScrollTo } from "./utils/expandAndScroll";
import { ActivityPanel } from "./ActivityPanel";

interface SettingsChangeRequestsListProps {
  updateCounter: (count: number) => void;
  onActionComplete?: () => void;
}

type ChangeSetItem = {
  draftRecordId: string;
  changeSetType: string;
  actionType: string;
  [k: string]: any;
};

export const SettingsChangeRequestsList = ({
  updateCounter,
  onActionComplete,
}: SettingsChangeRequestsListProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert } = useAlerts();
  const { whoAmI } = useWhoAmI();
  const currentUser = useCurrentUser();
  const [selectedRow, setSelectedRow] = useState<BundledRequest[]>([]);
  const [key, setKey] = useState<number>(0);
  const [approveRecord, setApproveRecord] = useState<boolean>(false);
  const [commitRecord, setCommitRecord] = useState<boolean>(false);
  const [showEmailConfirmModal, setShowEmailConfirmModal] = useState<boolean>(false);
  const [userCount, setUserCount] = useState<number>(0);
  const [isEmailing, setIsEmailing] = useState<boolean>(false);

  const { approveTideRequests } = useEnvironment();
  const { realmRepresentation } = useRealm();

  const refresh = () => {
    setSelectedRow([]);
    setKey((prev: number) => prev + 1);
    onActionComplete?.();
  };

  // Loader merges Ragnarok + Licensing requests
  const loader = async (): Promise<BundledRequest[]> => {
    try {
      const requests = await adminClient.tideUsersExt.getRequestedChangesForRagnarokSettings();
      const licensingRequests =
        await adminClient.tideUsersExt.getRequestedChangesForRealmLicensing();

      const merged: any[] = [
        ...(Array.isArray(requests) ? requests : []),
        ...(Array.isArray(licensingRequests) ? licensingRequests : []),
      ];

      const bundled = groupRequestsByDraftId(merged);
      updateCounter(bundled.length);
      return bundled;
    } catch (error) {
      console.error("Failed to load settings/licensing requests:", error);
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
    const allRequests = firstBundle.requests as ChangeSetItem[];
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
      (status === "ACTIVE" &&
        (deleteStatus === "DRAFT" || deleteStatus === "PENDING"))
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

  // --- helpers: partition bundle requests into licensing vs ragnarok groups ---
  function partitionByType(reqs: ChangeSetItem[]) {
    const licensing: ChangeSetItem[] = [];
    const ragnarok: ChangeSetItem[] = [];
    for (const r of reqs) {
      const t = (r.changeSetType || "").toUpperCase();
      if (t === "REALM_LICENSING") licensing.push(r);
      else if (t === "RAGNAROK") ragnarok.push(r);
      else {
        throw new Error(`Unknown changeSetType '${r.changeSetType}' in request`);
      }
    }
    return { licensing, ragnarok };
  }

  async function runApprovalFlowForGroup(group: ChangeSetItem[]) {
    if (group.length === 0) return;

    const changeRequests = group.map((x) => ({
      changeSetId: x.draftRecordId,
      changeSetType: x.changeSetType,
      actionType: x.actionType,
    }));

    // New flow: approve via backend, then (if required) go through approveTideRequests
    const respObj: any = await adminClient.tideUsersExt.approveDraftChangeSet({
      changeSets: changeRequests,
    });

    if (!respObj || respObj.length === 0) {
      return;
    }

    try {
      
      const firstRespObj = respObj[0];
      if (
        firstRespObj.requiresApprovalPopup === true ||
        firstRespObj.requiresApprovalPopup === "true"
      ) {
        // Collect all change requests to send to the Tide approval UI
        const changereqs = respObj.map((resp: any) => {
          return {
            id: resp.changesetId,
            request: base64ToBytes(resp.changeSetDraftRequests),
          };
        });
        const reviewResponses = await approveTideRequests(changereqs);

        // Process each review response
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
      addAlert(error.responseData || "Failed to review settings change request", AlertVariant.danger);
    }
  }

  const handleApprove = async () => {
    try {
      if (!selectedRow[0]) return;

      const bundle = selectedRow[0];
      const allRequests = bundle.requests as ChangeSetItem[];

      // Partition mixed bundles and run flows sequentially (keeps backend semantics by type)
      const { licensing, ragnarok } = partitionByType(allRequests);

      await runApprovalFlowForGroup(licensing);
      await runApprovalFlowForGroup(ragnarok);

      refresh();
    } catch (error: any) {
      addAlert(error.responseData || "Failed to approve request", AlertVariant.danger);
    }
  };

  const handleCommit = async () => {
    try {
      const allRequests = selectedRow.flatMap((bundle) => bundle.requests) as ChangeSetItem[];
      const hasRagnarokRequest = allRequests.some(
        (req) => (req.changeSetType || "").toLowerCase() === "ragnarok"
      );

      if (hasRagnarokRequest) {
        const users = await adminClient.users.find();
        setUserCount(users.length);
        setShowEmailConfirmModal(true);
        return;
      }

      const changeRequests = allRequests.map((x) => ({
        changeSetId: x.draftRecordId,
        changeSetType: x.changeSetType,
        actionType: x.actionType,
      }));

      await adminClient.tideUsersExt.commitDraftChangeSet({
        changeSets: changeRequests,
      });
      addAlert(t("Settings change request committed"), AlertVariant.success);
      refresh();
    } catch (error: any) {
      addAlert(error.responseData || "Failed to commit request", AlertVariant.danger);
    }
  };

  const handleSendEmails = async () => {
    setIsEmailing(true);
    try {
      const users = await adminClient.users.find();
      setUserCount(users.length);

      await Promise.all(
        users.map((user: any) =>
          adminClient.users.executeActionsEmail({
            id: user.id!,
            actions: ["UPDATE_PASSWORD"],
            lifespan: 43200,
          })
        )
      );

      const allRequests = selectedRow.flatMap((bundle) => bundle.requests) as ChangeSetItem[];
      const changeRequests = allRequests.map((x) => ({
        changeSetId: x.draftRecordId,
        changeSetType: x.changeSetType,
        actionType: x.actionType,
      }));

      await adminClient.tideUsersExt.commitDraftChangeSet({
        changeSets: changeRequests,
      });
      addAlert(t(`Settings committed and emails sent to ${userCount} users`), AlertVariant.success);
      setShowEmailConfirmModal(false);
      refresh();
    } catch (error: any) {
      addAlert(error.responseData || "Failed to send emails", AlertVariant.danger);
      setShowEmailConfirmModal(false);
    } finally {
      setIsEmailing(false);
    }
  };

  const handleSkipEmails = async () => {
    setIsEmailing(true);
    try {
      const allRequests = selectedRow.flatMap((bundle) => bundle.requests) as ChangeSetItem[];
      const changeRequests = allRequests.map((x) => ({
        changeSetId: x.draftRecordId,
        changeSetType: x.changeSetType,
        actionType: x.actionType,
      }));

      await adminClient.tideUsersExt.commitDraftChangeSet({
        changeSets: changeRequests,
      });
      addAlert(t("Settings change request committed"), AlertVariant.success);
      setShowEmailConfirmModal(false);
      refresh();
    } catch (error: any) {
      addAlert(error.responseData || "Failed to commit request", AlertVariant.danger);
      setShowEmailConfirmModal(false);
    } finally {
      setIsEmailing(false);
    }
  };

  const [toggleCancelDialog, CancelConfirm] = useConfirmDialog({
    titleKey: "Cancel Settings Change Request",
    children: <>Are you sure you want to cancel this settings change request?</>,
    continueButtonLabel: "cancel",
    cancelButtonLabel: "back",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        const allRequests = selectedRow.flatMap((bundle) => bundle.requests) as ChangeSetItem[];
        const changeSetArray = allRequests.map((row) => ({
          changeSetId: row.draftRecordId,
          changeSetType: row.changeSetType || "RAGNAROK",
          actionType: row.actionType,
        }));

        await adminClient.tideUsersExt.cancelDraftChangeSet({
          changeSets: changeSetArray,
        });
        addAlert(t("Settings change request cancelled"), AlertVariant.success);
        refresh();
      } catch (error) {
        addAlert("Error cancelling settings change request", AlertVariant.danger);
      }
    },
  });

  const typeLabel = (bundle: BundledRequest) => {
    const types = [
      ...new Set(
        bundle.requests
          .map((r: any) => (r.changeSetType || "").toUpperCase())
          .filter(Boolean)
      ),
    ];
    if (types.length === 1) {
      const t0 = types[0];
      const color =
        t0 === "RAGNAROK" ? "purple" : t0 === "LICENSING" ? "blue" : "grey";
      return (
        <Label color={color as any} className="keycloak-admin--role-mapping__client-name">
          {t0}
        </Label>
      );
    }
    return (
      <Label color="gold" className="keycloak-admin--role-mapping__client-name">
        MIXED
      </Label>
    );
  };

  const bundleStatusLabel = (bundle: BundledRequest) => {
    const statuses = [
      ...new Set(
        bundle.requests.map((r: any) =>
          r.status === "ACTIVE" ? r.deleteStatus || r.status : r.status
        )
      ),
    ];
    if (statuses.length === 1) {
      const status = statuses[0] as string;
      const color =
        status === "PENDING"
          ? "orange"
          : status === "APPROVED"
          ? "blue"
          : status === "DENIED"
          ? "red"
          : "grey";
      return (
        <Label color={color as any} className="keycloak-admin--role-mapping__client-name">
          {status}
        </Label>
      );
    }
    return (
      <Label color="purple" className="keycloak-admin--role-mapping__client-name">
        MIXED
      </Label>
    );
  };

  const statusLabel = (row: any) => {
    const status = row.status === "ACTIVE" ? row.deleteStatus || row.status : row.status;
    const color =
      status === "PENDING"
        ? "orange"
        : status === "APPROVED"
        ? "blue"
        : status === "DENIED"
        ? "red"
        : "grey";
    return (
      <Label color={color as any} className="keycloak-admin--role-mapping__client-name">
        {status}
      </Label>
    );
  };

  const DetailCell = (bundle: BundledRequest) => (
    <>
      <ExpandableSection toggleText="Change Requests" isIndented>
        <Table aria-label="Bundle details" variant="compact" borders={false} isStriped>
          <Thead>
            <Tr>
              <Th width={10}>Action</Th>
              <Th width={20} modifier="wrap">
                Request Type
              </Th>
              <Th width={20} modifier="wrap">
                Change Set Type
              </Th>
              <Th width={10} modifier="wrap">
                Action Type
              </Th>
              <Th width={10}>Status</Th>
              <Th width={10}>Realm ID</Th>
            </Tr>
          </Thead>
          <Tbody>
            {bundle.requests.map((request: any, index: number) => (
              <Tr key={index}>
                <Td dataLabel="Action">{request.action}</Td>
                <Td dataLabel="Request Type">{request.requestType}</Td>
                <Td dataLabel="Change Set Type">{request.changeSetType}</Td>
                <Td dataLabel="Action Type">{request.actionType}</Td>
                <Td dataLabel="Status">{statusLabel(request)}</Td>
                <Td dataLabel="Realm ID">{request.realmId}</Td>
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
      cellRenderer: (bundle: BundledRequest) => {
        if (bundle.requests.length === 1) {
          const request = bundle.requests[0];
          return (
            <div>
              <div className="pf-v5-u-font-weight-bold">{request.action}</div>
              <div className="pf-v5-u-color-200">
                {request.requestType} • {request.changeSetType}
              </div>
            </div>
          );
        } else {
          const actions = [...new Set(bundle.requests.map((r: any) => r.action))];
          const types = [...new Set(bundle.requests.map((r: any) => r.requestType))];
          return (
            <div>
              <div className="pf-v5-u-font-weight-bold">
                {bundle.requests.length} changes: {actions.join(", ")}
              </div>
              <div className="pf-v5-u-color-200">{types.join(", ")}</div>
            </div>
          );
        }
      },
    },
    {
      name: "Type",
      displayKey: "Type",
      cellRenderer: (bundle: BundledRequest) => typeLabel(bundle),
    },
    {
      name: "Status",
      displayKey: "Status",
      cellRenderer: (bundle: BundledRequest) => bundleStatusLabel(bundle),
    },
    {
      name: "Reviews",
      displayKey: "Reviews",
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
      ),
    },
    {
      name: "Comments",
      displayKey: "Comments",
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
          <Button variant="primary" isDisabled={!approveRecord} onClick={handleApprove}>
            {t("Review Draft")}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button variant="secondary" isDisabled={!commitRecord} onClick={handleCommit}>
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
          ariaLabelKey="Settings Change Requests"
          detailColumns={[
            {
              name: "details",
              cellRenderer: DetailCell,
            },
          ]}
          columns={columns}
          isPaginated
          onSelect={(value: BundledRequest[]) => setSelectedRow([...value])}
          emptyState={
            <EmptyState variant="lg">
              <Title headingLevel="h4" size="lg">
                {t("noSettingsChangeRequests")}
              </Title>
              <EmptyStateBody>
                <TextContent>
                  <Text>No settings or licensing change requests found.</Text>
                </TextContent>
              </EmptyStateBody>
            </EmptyState>
          }
        />
      </div>

      <CancelConfirm />

      <Modal
        variant={ModalVariant.small}
        title="Send Password Reset Emails"
        isOpen={showEmailConfirmModal}
        onClose={() => setShowEmailConfirmModal(false)}
        actions={
          !realmRepresentation?.smtpServer ||
          Object.keys(realmRepresentation.smtpServer).length === 0
            ? [
                <Button
                  key="send"
                  variant="primary"
                  onClick={handleSkipEmails}
                  isDisabled={isEmailing}
                >
                  {isEmailing ? <Spinner size="sm" /> : t("Continue with offboarding")}
                </Button>,
                <Button
                  key="close"
                  variant="primary"
                  onClick={() => setShowEmailConfirmModal(false)}
                  isDisabled={isEmailing}
                >
                  {t("Close")}
                </Button>,
              ]
            : [
                <Button
                  key="send"
                  variant="primary"
                  onClick={handleSendEmails}
                  isDisabled={isEmailing}
                >
                  {isEmailing ? (
                    <Spinner size="sm" />
                  ) : (
                    `Send Emails to ${userCount} Users and Offboard`
                  )}
                </Button>,
                <Button
                  key="skip"
                  variant="secondary"
                  onClick={handleSkipEmails}
                  isDisabled={isEmailing}
                >
                  {isEmailing ? <Spinner size="sm" /> : t("Skip Email Notification")}
                </Button>,
              ]
        }
      >
        <TextContent>
          {!realmRepresentation?.smtpServer ||
          Object.keys(realmRepresentation.smtpServer).length === 0 ? (
            <>
              <Text>
                A Ragnarok (offboarding) request is ready to be committed, which will affect{" "}
                <strong>{userCount}</strong> user/s in the realm.
              </Text>
              <Text className="pf-v5-u-mt-md pf-v5-u-color-danger">
                <strong>No SMTP server is configured for this realm.</strong> You will need to
                manually email user/s to reset their passwords.
                <br />
                <br />
                <strong>
                  ENSURE YOU HAVE SET A PASSWORD FOR YOUR OWN ADMIN ACCOUNT BEFORE CONTINUING.
                </strong>
              </Text>
            </>
          ) : (
            <>
              <Text>
                A Ragnarok (offboarding) request has been committed. Would you like to send password
                reset emails to all {userCount} user/s in the realm?
              </Text>
              <Text className="pf-v5-u-mt-md pf-v5-u-color-200">
                This will require all users to reset their passwords within 12 hours.
                <br />
                <br />
                <strong>
                  ENSURE YOU HAVE SET A PASSWORD FOR YOUR OWN ADMIN ACCOUNT BEFORE CONTINUING.
                </strong>
              </Text>
            </>
          )}
        </TextContent>
      </Modal>
    </>
  );
};
