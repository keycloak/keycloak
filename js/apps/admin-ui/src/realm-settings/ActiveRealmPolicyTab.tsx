import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  FormGroup,
  FormSelect,
  FormSelectOption,
  Label,
  TextInput,
  Title,
  Switch,
} from "@patternfly/react-core";
import { CubesIcon } from "@patternfly/react-icons";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useNavigate } from "react-router-dom";
import { useRealm } from "../context/realm-context/RealmContext";
import { toChangeRequests } from "../tide-change-requests/routes/ChangeRequests";
import CodeEditor from "../components/form/CodeEditor";

// ── Types ──────────────────────────────────────────────────────────────

interface TemplateParameter {
  name: string;
  type: "string" | "number" | "boolean" | "select";
  helpText: string;
  required: boolean;
  defaultValue?: string;
  options?: string[];
}

interface PolicyTemplate {
  id: string;
  name: string;
  description?: string;
  contractCode: string;
  modelId?: string;
  parameters?: TemplateParameter[];
}

interface RealmPolicyState {
  status: "none" | "pending" | "active" | "delete_pending";
  id?: string;
  templateId?: string;
  templateName?: string;
  changesetRequestId?: string;
  changesetStatus?: string;
  requestModel?: string;
  policyData?: string;
  timestamp?: number;
}

// ── Component ──────────────────────────────────────────────────────────

export const ActiveRealmPolicyTab = () => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const [realmPolicy, setRealmPolicy] = useState<RealmPolicyState>({
    status: "none",
  });
  const [templates, setTemplates] = useState<PolicyTemplate[]>([]);
  const [key, setKey] = useState(0);

  // Form state
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [paramValues, setParamValues] = useState<Record<string, string>>({});
  const [previewCode, setPreviewCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const refresh = () => setKey((k) => k + 1);

  // Fetch realm policy status
  useFetch(
    () => adminClient.tideUsersExt.getRealmPolicy(),
    (data) =>
      setRealmPolicy((data || { status: "none" }) as RealmPolicyState),
    [key],
  );

  // Fetch templates for the selector
  useFetch(
    () => adminClient.tideUsersExt.listPolicyTemplates(),
    (data) => setTemplates((data || []) as PolicyTemplate[]),
    [key],
  );

  // ── Template selection ────────────────────────────────────────────

  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId);

  const handleTemplateSelect = (_event: unknown, value: string) => {
    setSelectedTemplateId(value);
    const template = templates.find((t) => t.id === value);
    if (template) {
      const defaults: Record<string, string> = {};
      (template.parameters || []).forEach((p) => {
        defaults[p.name] = p.defaultValue || "";
      });
      setParamValues(defaults);
      setPreviewCode(substituteParams(template.contractCode, defaults));
    } else {
      setParamValues({});
      setPreviewCode("");
    }
  };

  const handleParamChange = (paramName: string, value: string) => {
    const updated = { ...paramValues, [paramName]: value };
    setParamValues(updated);
    if (selectedTemplate) {
      setPreviewCode(substituteParams(selectedTemplate.contractCode, updated));
    }
  };

  const substituteParams = (
    code: string,
    params: Record<string, string>,
  ): string => {
    let result = code;
    for (const [key, val] of Object.entries(params)) {
      result = result.replace(
        new RegExp(`\\{\\{${key}\\}\\}`, "g"),
        val || `{{${key}}}`,
      );
    }
    return result;
  };

  // ── Create pending policy ─────────────────────────────────────────

  const handleCreatePending = async () => {
    if (!selectedTemplateId) {
      addAlert("Please select a template", AlertVariant.danger);
      return;
    }

    const template = templates.find((t) => t.id === selectedTemplateId);
    if (template) {
      const missing = (template.parameters || [])
        .filter((p) => p.required && !paramValues[p.name]?.trim())
        .map((p) => p.name);
      if (missing.length > 0) {
        addAlert(
          `Missing required parameters: ${missing.join(", ")}`,
          AlertVariant.danger,
        );
        return;
      }
    }

    setIsSubmitting(true);
    try {
      const result = await adminClient.tideUsersExt.createPendingRealmPolicy({
        templateId: selectedTemplateId,
        contractCode: previewCode,
        paramValues,
      });
      addAlert(
        `Policy request created for "${result.templateName}". Ready to commit.`,
        AlertVariant.success,
      );
      setSelectedTemplateId("");
      setParamValues({});
      setPreviewCode("");
      refresh();
    } catch (error) {
      addError("Failed to create pending policy", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Delete dialog ─────────────────────────────────────────────────

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "Request Realm Policy Deletion",
    messageKey:
      "This will create a deletion request that requires admin approval. Continue?",
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.tideUsersExt.requestDeleteRealmPolicy();
        addAlert(
          "Deletion request created. Approve it in Change Requests.",
          AlertVariant.success,
        );
        refresh();
      } catch (error) {
        addError("Failed to request policy deletion", error);
      }
    },
  });

  const [toggleCancelDeleteDialog, CancelDeleteConfirm] = useConfirmDialog({
    titleKey: "Cancel Deletion Request",
    messageKey:
      "Are you sure you want to cancel the pending deletion request? The policy will remain active.",
    continueButtonLabel: t("cancel"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.tideUsersExt.deleteRealmPolicy();
        addAlert("Deletion request cancelled", AlertVariant.success);
        refresh();
      } catch (error) {
        addError("Failed to cancel deletion request", error);
      }
    },
  });

  const [toggleCancelCreateDialog, CancelCreateConfirm] = useConfirmDialog({
    titleKey: "Cancel Policy Request",
    messageKey:
      "Are you sure you want to cancel this policy creation request?",
    continueButtonLabel: t("cancel"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.tideUsersExt.deleteRealmPolicy();
        addAlert("Policy request cancelled", AlertVariant.success);
        refresh();
      } catch (error) {
        addError("Failed to cancel policy request", error);
      }
    },
  });

  // ── Render ────────────────────────────────────────────────────────

  return (
    <>
      <DeleteConfirm />
      <CancelDeleteConfirm />
      <CancelCreateConfirm />
      <div
        style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}
      >
        {/* ── Status Card ──────────────────────────────────────────── */}
        <Card>
          <CardTitle>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <span>Realm Policy</span>
              {realmPolicy.status === "active" && (
                <Label color="green">Active</Label>
              )}
              {realmPolicy.status === "pending" && (
                <Label color="orange">Pending Approval</Label>
              )}
              {realmPolicy.status === "delete_pending" && (
                <Label color="orange">Deletion Pending</Label>
              )}
            </div>
          </CardTitle>
          <CardBody>
            {/* ── No policy ──────────────────────────────────────── */}
            {realmPolicy.status === "none" && (
              <EmptyState>
                <EmptyStateIcon icon={CubesIcon} />
                <Title headingLevel="h4" size="lg">
                  No realm policy
                </Title>
                <EmptyStateBody>
                  This realm does not have a policy. Select a template below to
                  create one.
                </EmptyStateBody>
              </EmptyState>
            )}

            {/* ── Pending policy ─────────────────────────────────── */}
            {realmPolicy.status === "pending" && (
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "1rem",
                }}
              >
                <DescriptionList isHorizontal>
                  <DescriptionListGroup>
                    <DescriptionListTerm>Template</DescriptionListTerm>
                    <DescriptionListDescription>
                      {realmPolicy.templateName || realmPolicy.templateId}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                  <DescriptionListGroup>
                    <DescriptionListTerm>Status</DescriptionListTerm>
                    <DescriptionListDescription>
                      Pending approval — review and approve in Change Requests
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                </DescriptionList>

                <div style={{ display: "flex", gap: "0.5rem" }}>
                  <Button
                    variant={ButtonVariant.primary}
                    onClick={() =>
                      navigate(
                        toChangeRequests({ realm, tab: "policies" }).pathname!
                      )
                    }
                  >
                    Go to Change Requests
                  </Button>
                  <Button
                    variant={ButtonVariant.danger}
                    onClick={toggleCancelCreateDialog}
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            )}

            {/* ── Delete Pending policy ──────────────────────────── */}
            {realmPolicy.status === "delete_pending" && (
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "1rem",
                }}
              >
                <DescriptionList isHorizontal>
                  <DescriptionListGroup>
                    <DescriptionListTerm>Template</DescriptionListTerm>
                    <DescriptionListDescription>
                      {realmPolicy.templateName || realmPolicy.templateId}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                  <DescriptionListGroup>
                    <DescriptionListTerm>Status</DescriptionListTerm>
                    <DescriptionListDescription>
                      Deletion pending approval — review and approve in Change
                      Requests
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                </DescriptionList>

                <div style={{ display: "flex", gap: "0.5rem" }}>
                  <Button
                    variant={ButtonVariant.primary}
                    onClick={() =>
                      navigate(
                        toChangeRequests({ realm, tab: "policies" }).pathname!,
                      )
                    }
                  >
                    Go to Change Requests
                  </Button>
                  <Button
                    variant={ButtonVariant.secondary}
                    onClick={toggleCancelDeleteDialog}
                  >
                    Cancel Deletion Request
                  </Button>
                </div>
              </div>
            )}

            {/* ── Active policy ──────────────────────────────────── */}
            {realmPolicy.status === "active" && (
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "1rem",
                }}
              >
                <DescriptionList isHorizontal>
                  <DescriptionListGroup>
                    <DescriptionListTerm>Template</DescriptionListTerm>
                    <DescriptionListDescription>
                      {realmPolicy.templateName || realmPolicy.templateId}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                  <DescriptionListGroup>
                    <DescriptionListTerm>Committed</DescriptionListTerm>
                    <DescriptionListDescription>
                      {realmPolicy.timestamp
                        ? new Date(realmPolicy.timestamp).toLocaleString()
                        : "—"}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                  {realmPolicy.policyData && (
                    <DescriptionListGroup>
                      <DescriptionListTerm>Policy Data</DescriptionListTerm>
                      <DescriptionListDescription>
                        <span
                          style={{
                            fontFamily: "monospace",
                            fontSize: "0.75rem",
                            wordBreak: "break-all",
                          }}
                        >
                          {realmPolicy.policyData.substring(0, 80)}...
                        </span>
                      </DescriptionListDescription>
                    </DescriptionListGroup>
                  )}
                </DescriptionList>

                <div>
                  <Button
                    variant={ButtonVariant.danger}
                    onClick={toggleDeleteDialog}
                  >
                    Remove Policy
                  </Button>
                </div>
              </div>
            )}
          </CardBody>
        </Card>

        {/* ── Create New Policy ─────────────────────────────────────── */}
        {realmPolicy.status === "none" && (
          <Card>
            <CardTitle>Create Policy from Template</CardTitle>
            <CardBody>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "1rem",
                }}
              >
                <FormGroup
                  label="Select Template"
                  isRequired
                  fieldId="template-select"
                >
                  {templates.length === 0 ? (
                    <div
                      style={{
                        color: "var(--pf-v5-global--Color--200, #6a6e73)",
                        fontStyle: "italic",
                      }}
                    >
                      No templates available. Create a template in the Policy
                      Templates tab first.
                    </div>
                  ) : (
                    <FormSelect
                      id="template-select"
                      value={selectedTemplateId}
                      onChange={handleTemplateSelect}
                    >
                      <FormSelectOption
                        value=""
                        label="— Select a template —"
                        isDisabled
                      />
                      {templates.map((tmpl) => (
                        <FormSelectOption
                          key={tmpl.id}
                          value={tmpl.id}
                          label={tmpl.name}
                        />
                      ))}
                    </FormSelect>
                  )}
                </FormGroup>

                {selectedTemplate && (
                  <>
                    {selectedTemplate.description && (
                      <div
                        style={{
                          color: "var(--pf-v5-global--Color--200, #6a6e73)",
                          fontSize: "0.875rem",
                        }}
                      >
                        {selectedTemplate.description}
                      </div>
                    )}

                    {/* ── Parameter inputs ────────────────────────── */}
                    {(selectedTemplate.parameters || []).length > 0 && (
                      <FormGroup
                        label="Template Parameters"
                        fieldId="params-section"
                      >
                        <div
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "0.75rem",
                          }}
                        >
                          {(selectedTemplate.parameters || []).map((param) => (
                            <FormGroup
                              key={param.name}
                              label={
                                <>
                                  {param.name}
                                  {param.required && (
                                    <span style={{ color: "red" }}> *</span>
                                  )}
                                </>
                              }
                              fieldId={`param-${param.name}`}
                            >
                              {param.helpText && (
                                <div
                                  style={{
                                    fontSize: "0.8rem",
                                    color:
                                      "var(--pf-v5-global--Color--200, #6a6e73)",
                                    marginBottom: "0.25rem",
                                  }}
                                >
                                  {param.helpText}
                                </div>
                              )}
                              {param.type === "boolean" ? (
                                <Switch
                                  id={`param-${param.name}`}
                                  isChecked={
                                    paramValues[param.name] === "true"
                                  }
                                  onChange={(_event, checked) =>
                                    handleParamChange(
                                      param.name,
                                      String(checked),
                                    )
                                  }
                                  label="Yes"
                                  labelOff="No"
                                />
                              ) : param.type === "select" && param.options ? (
                                <FormSelect
                                  id={`param-${param.name}`}
                                  value={paramValues[param.name] || ""}
                                  onChange={(_event, value) =>
                                    handleParamChange(param.name, value)
                                  }
                                >
                                  <FormSelectOption
                                    value=""
                                    label="— Select —"
                                    isDisabled
                                  />
                                  {param.options.map((opt) => (
                                    <FormSelectOption
                                      key={opt}
                                      value={opt}
                                      label={opt}
                                    />
                                  ))}
                                </FormSelect>
                              ) : (
                                <TextInput
                                  id={`param-${param.name}`}
                                  type={
                                    param.type === "number" ? "number" : "text"
                                  }
                                  value={paramValues[param.name] || ""}
                                  onChange={(_event, value) =>
                                    handleParamChange(param.name, value)
                                  }
                                  placeholder={
                                    param.defaultValue
                                      ? `Default: ${param.defaultValue}`
                                      : undefined
                                  }
                                />
                              )}
                            </FormGroup>
                          ))}
                        </div>
                      </FormGroup>
                    )}

                    {/* ── Code preview ────────────────────────────── */}
                    <FormGroup
                      label="Contract Code Preview"
                      fieldId="code-preview"
                    >
                      <CodeEditor
                        id="code-preview"
                        language="csharp"
                        value={previewCode}
                        onChange={(value) => setPreviewCode(value)}
                        height={300}
                      />
                    </FormGroup>

                    <div>
                      <Button
                        variant={ButtonVariant.primary}
                        onClick={handleCreatePending}
                        isDisabled={isSubmitting}
                      >
                        {isSubmitting
                          ? "Creating..."
                          : "Create Policy Request"}
                      </Button>
                    </div>
                  </>
                )}
              </div>
            </CardBody>
          </Card>
        )}
      </div>
    </>
  );
};
