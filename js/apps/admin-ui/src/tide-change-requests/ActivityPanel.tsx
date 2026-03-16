/** TIDECLOAK IMPLEMENTATION */
import React, { useState, useEffect } from "react";
import {
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Label,
  TextArea,
  Button,
  Spinner,
  Divider,
  AlertVariant,
} from "@patternfly/react-core";
import { PencilAltIcon, TrashIcon, CheckIcon, TimesIcon } from "@patternfly/react-icons";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useWhoAmI } from "../context/whoami/WhoAmI";

interface ApprovalEntry {
  userId: string;
  username: string;
  isApproval: boolean;
  timestamp: number;
}

interface CommentEntry {
  id: string;
  userId: string;
  username: string;
  comment: string;
  timestamp: number;
}

interface ActivityData {
  requestedBy: string;
  requestedByUsername: string;
  timestamp: number;
  approvals: ApprovalEntry[];
  comments: CommentEntry[];
}

function formatTimestamp(epochSeconds: number): string {
  if (!epochSeconds) return "";
  return new Date(epochSeconds * 1000).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
}

export const ActivityPanel = ({
  changesetRequestId,
}: {
  changesetRequestId: string;
}) => {
  const { adminClient } = useAdminClient();
  const { addAlert } = useAlerts();
  const { whoAmI } = useWhoAmI();
  const [activity, setActivity] = useState<ActivityData | null>(null);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editingText, setEditingText] = useState("");

  const loadActivity = async () => {
    try {
      const data = await adminClient.tideUsersExt.getChangeSetActivity({
        id: changesetRequestId,
      });
      setActivity(data);
    } catch {
      setActivity(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadActivity();
  }, [changesetRequestId]);

  const handleAddComment = async () => {
    if (!newComment.trim()) return;
    setSubmitting(true);
    try {
      await adminClient.tideUsersExt.addChangeSetComment({
        id: changesetRequestId,
        comment: newComment.trim(),
      });
      setNewComment("");
      await loadActivity();
      addAlert("Comment added", AlertVariant.success);
    } catch {
      addAlert("Failed to add comment", AlertVariant.danger);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditComment = async (commentId: string) => {
    if (!editingText.trim()) return;
    setSubmitting(true);
    try {
      await adminClient.tideUsersExt.updateChangeSetComment({
        id: changesetRequestId,
        commentId,
        comment: editingText.trim(),
      });
      setEditingCommentId(null);
      setEditingText("");
      await loadActivity();
      addAlert("Comment updated", AlertVariant.success);
    } catch {
      addAlert("Failed to update comment", AlertVariant.danger);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    setSubmitting(true);
    try {
      await adminClient.tideUsersExt.deleteChangeSetComment({
        id: changesetRequestId,
        commentId,
      });
      await loadActivity();
      addAlert("Comment deleted", AlertVariant.success);
    } catch {
      addAlert("Failed to delete comment", AlertVariant.danger);
    } finally {
      setSubmitting(false);
    }
  };

  const startEditing = (c: CommentEntry) => {
    setEditingCommentId(c.id);
    setEditingText(c.comment);
  };

  const cancelEditing = () => {
    setEditingCommentId(null);
    setEditingText("");
  };

  if (loading) return <Spinner size="md" />;
  if (!activity) return null;

  const isMyComment = (c: CommentEntry) => c.userId === whoAmI.userId;

  return (
    <div className="pf-v5-u-mt-md">
      <Divider className="pf-v5-u-mb-md" />

      {/* Requested By */}
      {activity.requestedByUsername && (
        <DescriptionList isHorizontal className="pf-v5-u-mb-md">
          <DescriptionListGroup>
            <DescriptionListTerm>Requested By</DescriptionListTerm>
            <DescriptionListDescription>
              <strong>{activity.requestedByUsername}</strong>
              {activity.timestamp ? (
                <span className="pf-v5-u-color-200 pf-v5-u-ml-sm">
                  on {formatTimestamp(activity.timestamp)}
                </span>
              ) : null}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      )}

      {/* Approvals / Rejections */}
      <div id={`activity-reviews-${changesetRequestId}`} className="pf-v5-u-mb-md">
        <strong className="pf-v5-u-font-size-sm">Reviews</strong>
        {activity.approvals.length > 0 ? (
          <div className="pf-v5-u-mt-xs">
            {activity.approvals.map((a, i) => (
              <div
                key={i}
                className="pf-v5-u-display-flex pf-v5-u-align-items-center pf-v5-u-mb-xs"
              >
                <Label
                  color={a.isApproval ? "green" : "red"}
                  className="pf-v5-u-mr-sm"
                  isCompact
                >
                  {a.isApproval ? "Approved" : "Denied"}
                </Label>
                <span>
                  {a.username || a.userId}
                  {a.timestamp ? (
                    <span className="pf-v5-u-color-200 pf-v5-u-ml-sm pf-v5-u-font-size-sm">
                      {formatTimestamp(a.timestamp)}
                    </span>
                  ) : null}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <div className="pf-v5-u-color-200 pf-v5-u-mt-xs pf-v5-u-font-size-sm">
            No reviews yet
          </div>
        )}
      </div>

      {/* Comments */}
      <div id={`activity-comments-${changesetRequestId}`} className="pf-v5-u-mb-md">
        <strong className="pf-v5-u-font-size-sm">Comments</strong>
        {activity.comments.length > 0 ? (
          <div className="pf-v5-u-mt-xs">
            {activity.comments.map((c) => (
              <div
                key={c.id}
                className="pf-v5-u-mb-sm pf-v5-u-p-sm"
                style={{
                  background: "var(--pf-v5-global--BackgroundColor--200)",
                  borderRadius: "4px",
                }}
              >
                <div className="pf-v5-u-display-flex pf-v5-u-justify-content-space-between pf-v5-u-mb-xs">
                  <strong className="pf-v5-u-font-size-sm">
                    {c.username}
                  </strong>
                  <div className="pf-v5-u-display-flex pf-v5-u-align-items-center" style={{ gap: "8px" }}>
                    <span className="pf-v5-u-color-200 pf-v5-u-font-size-xs">
                      {formatTimestamp(c.timestamp)}
                    </span>
                    {isMyComment(c) && editingCommentId !== c.id && (
                      <>
                        <Button
                          variant="plain"
                          size="sm"
                          onClick={() => startEditing(c)}
                          isDisabled={submitting}
                          style={{ padding: "2px" }}
                        >
                          <PencilAltIcon />
                        </Button>
                        <Button
                          variant="plain"
                          size="sm"
                          onClick={() => handleDeleteComment(c.id)}
                          isDisabled={submitting}
                          style={{ padding: "2px", color: "var(--pf-v5-global--danger-color--100)" }}
                        >
                          <TrashIcon />
                        </Button>
                      </>
                    )}
                  </div>
                </div>
                {editingCommentId === c.id ? (
                  <div>
                    <TextArea
                      value={editingText}
                      onChange={(_e, val) => setEditingText(val)}
                      rows={2}
                      isDisabled={submitting}
                      autoFocus
                    />
                    <div className="pf-v5-u-mt-xs pf-v5-u-display-flex" style={{ gap: "6px" }}>
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() => handleEditComment(c.id)}
                        isDisabled={!editingText.trim() || submitting}
                        isLoading={submitting}
                        icon={<CheckIcon />}
                      >
                        Save
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={cancelEditing}
                        isDisabled={submitting}
                        icon={<TimesIcon />}
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div>{c.comment}</div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="pf-v5-u-color-200 pf-v5-u-mt-xs pf-v5-u-font-size-sm">
            No comments yet
          </div>
        )}
      </div>

      {/* Add Comment */}
      <div className="pf-v5-u-display-flex pf-v5-u-align-items-flex-end">
        <TextArea
          value={newComment}
          onChange={(_e, val) => setNewComment(val)}
          placeholder="Add a comment..."
          rows={2}
          className="pf-v5-u-mr-sm"
          style={{ flex: 1 }}
          isDisabled={submitting}
        />
        <Button
          variant="secondary"
          onClick={handleAddComment}
          isDisabled={!newComment.trim() || submitting}
          isLoading={submitting}
          size="sm"
        >
          Comment
        </Button>
      </div>
    </div>
  );
};
