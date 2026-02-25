import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  FormSelect,
  FormSelectOption,
  Modal,
  ModalVariant,
  Switch,
  TextArea,
  TextInput,
  HelperText,
  HelperTextItem,
  ToolbarItem,
} from "@patternfly/react-core";
import { PlusCircleIcon, TrashIcon } from "@patternfly/react-icons";
import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
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
  id?: string;
  name: string;
  description?: string;
  contractCode: string;
  parameters?: TemplateParameter[];
  createdBy?: string;
}

// ── Default C# stub ────────────────────────────────────────────────────

const DEFAULT_CS_CODE = `using Ork.Forseti.Sdk;
using System;
using System.Collections.Generic;
using System.Text;

/// <summary>
/// Forseti contract — start here.
/// </summary>
public class Contract : IAccessPolicy
{
    [PolicyParam(Required = true, Description = "Role required for SSH access.")]
    public string Role { get; set; }

    [PolicyParam(Required = true, Description = "Resource identifier for role checks.")]
    public string Resource { get; set; }

    public PolicyDecision ValidateData(DataContext ctx)
    {
        throw new NotImplementedException("Implement validation in ValidateData().");
    }

    public PolicyDecision ValidateApprovers(ApproversContext ctx)
    {
        throw new NotImplementedException("Implement validation in ValidateApprovers().");
    }

    public PolicyDecision ValidateExecutor(ExecutorContext ctx)
    {
        throw new NotImplementedException("Implement validation in ValidateExecutor().");
    }
}`;

const defaultFormData: PolicyTemplate = {
  name: "",
  description: "",
  contractCode: DEFAULT_CS_CODE,
  parameters: [],
};

// ── Component ──────────────────────────────────────────────────────────

export const PolicyTemplatesTab = () => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [templates, setTemplates] = useState<PolicyTemplate[]>([]);
  const [key, setKey] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] =
    useState<PolicyTemplate | null>(null);
  const [selectedTemplate, setSelectedTemplate] =
    useState<PolicyTemplate | null>(null);
  const [formData, setFormData] = useState<PolicyTemplate>({
    ...defaultFormData,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useFetch(
    () => adminClient.tideUsersExt.listPolicyTemplates(),
    (data) => setTemplates((data || []) as PolicyTemplate[]),
    [key],
  );

  const refresh = () => setKey((k) => k + 1);
  const loader = async () => templates;

  // ── Modal helpers ──────────────────────────────────────────────────

  const openCreate = () => {
    setEditingTemplate(null);
    setFormData({ ...defaultFormData, parameters: [] });
    setModalOpen(true);
  };

  const openEdit = (template: PolicyTemplate) => {
    setEditingTemplate(template);
    setFormData({
      name: template.name,
      description: template.description || "",
      contractCode: template.contractCode || "",
      parameters: template.parameters || [],
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingTemplate(null);
  };

  // ── Submit ─────────────────────────────────────────────────────────

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      addAlert("Template name is required", AlertVariant.danger);
      return;
    }
    if (!formData.contractCode.trim()) {
      addAlert("Contract code is required", AlertVariant.danger);
      return;
    }

    const entryTypeMatch = formData.contractCode.match(
      /public\s+(?:\w+\s+)*class\s+(\w+)\s*:\s*IAccessPolicy/,
    );
    if (!entryTypeMatch || entryTypeMatch[1] !== "Contract") {
      addAlert(
        "Contract must declare `public class Contract : IAccessPolicy`",
        AlertVariant.danger,
      );
      return;
    }

    setIsSubmitting(true);
    try {
      await adminClient.tideUsersExt.createPolicyTemplate({
        name: formData.name,
        description: formData.description,
        contractCode: formData.contractCode,
        modelId: "default",
        parameters: formData.parameters,
      });

      addAlert(
        editingTemplate ? "Template updated" : "Template created",
        AlertVariant.success,
      );
      closeModal();
      refresh();
    } catch (error) {
      addError("Failed to save template", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Delete dialog ──────────────────────────────────────────────────

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "Delete Policy Template",
    messageKey: `Are you sure you want to delete "${selectedTemplate?.name}"? This action cannot be undone.`,
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      // TODO: call delete endpoint when backend supports it
      addAlert("Template deleted", AlertVariant.success);
      refresh();
    },
  });

  // ── Parameter builder ──────────────────────────────────────────────

  const addParameter = () => {
    setFormData({
      ...formData,
      parameters: [
        ...(formData.parameters || []),
        {
          name: "",
          type: "string",
          helpText: "",
          required: true,
          defaultValue: "",
        },
      ],
    });
  };

  const updateParameter = (
    index: number,
    field: keyof TemplateParameter,
    value: unknown,
  ) => {
    const updated = [...(formData.parameters || [])];
    updated[index] = { ...updated[index], [field]: value };
    setFormData({ ...formData, parameters: updated });
  };

  const removeParameter = (index: number) => {
    setFormData({
      ...formData,
      parameters: (formData.parameters || []).filter((_, i) => i !== index),
    });
  };

  // ── Render ─────────────────────────────────────────────────────────

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={templates.length}
        loader={loader}
        ariaLabelKey="policyTemplates"
        searchPlaceholderKey="Search templates..."
        toolbarItem={
          <ToolbarItem>
            <Button onClick={openCreate}>Create template</Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("edit"),
            onRowClick: (template) => {
              openEdit(template);
            },
          } as Action<PolicyTemplate>,
          {
            title: t("delete"),
            onRowClick: (template) => {
              setSelectedTemplate(template);
              toggleDeleteDialog();
            },
          } as Action<PolicyTemplate>,
        ]}
        columns={[
          {
            name: "name",
            displayKey: "name",
          },
          {
            name: "description",
            displayKey: "description",
          },
          {
            name: "parameters",
            displayKey: "Parameters",
            cellRenderer: (row: PolicyTemplate) => {
              const params = row.parameters || [];
              if (params.length === 0) return <>None</>;
              return <>{params.map((p) => p.name).join(", ")}</>;
            },
          },
        ]}
        emptyState={
          <ListEmptyState
            message="No policy templates"
            instructions="Create a policy template to get started with reusable C# Forseti contracts."
            primaryActionText="Create template"
            onPrimaryAction={openCreate}
          />
        }
      />

      {/* ── Create / Edit modal ─────────────────────────────────────── */}
      <Modal
        variant={ModalVariant.large}
        title={editingTemplate ? "Edit Template" : "Create Template"}
        isOpen={modalOpen}
        onClose={closeModal}
        actions={[
          <Button
            key="submit"
            variant={ButtonVariant.primary}
            onClick={handleSubmit}
            isDisabled={isSubmitting}
          >
            {isSubmitting
              ? "Saving..."
              : editingTemplate
                ? "Save Changes"
                : "Create Template"}
          </Button>,
          <Button
            key="cancel"
            variant={ButtonVariant.link}
            onClick={closeModal}
          >
            {t("cancel")}
          </Button>,
        ]}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <FormGroup label="Template Name" isRequired fieldId="template-name">
            <TextInput
              id="template-name"
              value={formData.name}
              onChange={(_event, value) =>
                setFormData({ ...formData, name: value })
              }
              isRequired
            />
          </FormGroup>

          <FormGroup label="Description" fieldId="template-description">
            <TextArea
              id="template-description"
              value={formData.description}
              onChange={(_event, value) =>
                setFormData({ ...formData, description: value })
              }
              rows={2}
            />
          </FormGroup>

          <FormGroup
            label="Contract Code (C#)"
            fieldId="template-code"
          >
            <HelperText>
              <HelperTextItem>
                Use {"{{PARAM_NAME}}"} placeholders for configurable values. Entry class must be: public class Contract : IAccessPolicy
              </HelperTextItem>
            </HelperText>
            <CodeEditor
              id="template-code"
              language="csharp"
              value={formData.contractCode}
              onChange={(value) =>
                setFormData({ ...formData, contractCode: value })
              }
              height={300}
            />
          </FormGroup>

          {/* ── Parameters section ──────────────────────────────────── */}
          <FormGroup label="Template Parameters" fieldId="template-params">
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.75rem",
              }}
            >
              {(formData.parameters || []).length === 0 && (
                <div
                  style={{
                    border:
                      "1px solid var(--pf-v5-global--BorderColor--100, #d2d2d2)",
                    borderRadius: "4px",
                    padding: "1rem",
                    textAlign: "center",
                    color: "var(--pf-v5-global--Color--200, #6a6e73)",
                  }}
                >
                  No parameters defined. Add parameters to make your template
                  configurable.
                </div>
              )}

              {(formData.parameters || []).map((param, index) => (
                <div
                  key={index}
                  style={{
                    border:
                      "1px solid var(--pf-v5-global--BorderColor--100, #d2d2d2)",
                    borderRadius: "4px",
                    padding: "1rem",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "0.5rem",
                    }}
                  >
                    <span style={{ fontWeight: 600 }}>
                      Parameter {index + 1}
                    </span>
                    <Button
                      variant={ButtonVariant.plain}
                      onClick={() => removeParameter(index)}
                    >
                      <TrashIcon />
                    </Button>
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "0.5rem",
                    }}
                  >
                    <FormGroup
                      label="Name (placeholder)"
                      fieldId={`param-name-${index}`}
                    >
                      <TextInput
                        id={`param-name-${index}`}
                        value={param.name}
                        onChange={(_event, value) =>
                          updateParameter(
                            index,
                            "name",
                            value.toUpperCase().replace(/[^A-Z0-9_]/g, "_"),
                          )
                        }
                        placeholder="e.g., APPROVAL_TYPE"
                        style={{ fontFamily: "monospace" }}
                      />
                    </FormGroup>

                    <FormGroup label="Type" fieldId={`param-type-${index}`}>
                      <FormSelect
                        id={`param-type-${index}`}
                        value={param.type}
                        onChange={(_event, value) =>
                          updateParameter(index, "type", value)
                        }
                      >
                        <FormSelectOption value="string" label="Text" />
                        <FormSelectOption value="number" label="Number" />
                        <FormSelectOption value="boolean" label="Yes/No" />
                        <FormSelectOption
                          value="select"
                          label="Select (Dropdown)"
                        />
                      </FormSelect>
                    </FormGroup>

                    <FormGroup
                      label="Help Text"
                      fieldId={`param-help-${index}`}
                      style={{ gridColumn: "1 / -1" }}
                    >
                      <TextInput
                        id={`param-help-${index}`}
                        value={param.helpText}
                        onChange={(_event, value) =>
                          updateParameter(index, "helpText", value)
                        }
                        placeholder="Explain what this parameter does..."
                      />
                    </FormGroup>

                    <FormGroup
                      label="Default Value"
                      fieldId={`param-default-${index}`}
                    >
                      <TextInput
                        id={`param-default-${index}`}
                        value={param.defaultValue || ""}
                        onChange={(_event, value) =>
                          updateParameter(index, "defaultValue", value)
                        }
                        placeholder="Optional default"
                      />
                    </FormGroup>

                    <FormGroup
                      label="Required"
                      fieldId={`param-required-${index}`}
                    >
                      <Switch
                        id={`param-required-${index}`}
                        isChecked={param.required}
                        onChange={(_event, checked) =>
                          updateParameter(index, "required", checked)
                        }
                        label="Required"
                        labelOff="Optional"
                      />
                    </FormGroup>

                    {param.type === "select" && (
                      <FormGroup
                        label="Options (comma-separated)"
                        fieldId={`param-options-${index}`}
                        style={{ gridColumn: "1 / -1" }}
                      >
                        <TextInput
                          id={`param-options-${index}`}
                          value={param.options?.join(", ") || ""}
                          onChange={(_event, value) =>
                            updateParameter(
                              index,
                              "options",
                              value
                                .split(",")
                                .map((s) => s.trim())
                                .filter(Boolean),
                            )
                          }
                          placeholder="e.g., option1, option2, option3"
                        />
                      </FormGroup>
                    )}
                  </div>
                </div>
              ))}

              <Button
                variant={ButtonVariant.secondary}
                icon={<PlusCircleIcon />}
                onClick={addParameter}
              >
                Add Parameter
              </Button>
            </div>
          </FormGroup>
        </div>
      </Modal>
    </>
  );
};
