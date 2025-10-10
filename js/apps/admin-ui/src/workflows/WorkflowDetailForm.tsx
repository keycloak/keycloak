import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { AlertVariant } from "@patternfly/react-core";
import { useState } from "react";
import {
  Controller,
  FormProvider,
  SubmitHandler,
  useForm,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import {
  HelpItem,
  FormSubmitButton,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { FormAccess } from "../components/form/FormAccess";
import { toWorkflows } from "./routes/Workflows";
import CodeEditor from "../components/form/CodeEditor";
import { useParams } from "../utils/useParams";
import {
  WorkflowDetailParams,
  toWorkflowDetail,
} from "./routes/WorkflowDetail";
import { ViewHeader } from "../components/view-header/ViewHeader";

type AttributeForm = {
  workflowJSON?: string;
  [key: string]: unknown;
};

export default function WorkflowDetailForm() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const { control, handleSubmit } = form;
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { mode, id } = useParams<WorkflowDetailParams>();
  const [workflowJSON, setWorkflowJSON] = useState("");
  const [enabled, setEnabled] = useState(true);

  useFetch(
    async () => {
      if (mode === "create") {
        return undefined;
      }
      return adminClient.workflows.findOne({
        id: id!,
      });
    },
    (workflow) => {
      if (!workflow) {
        return;
      }

      if (mode === "copy") {
        delete workflow.id;
        workflow.name = `${workflow.name} -- ${t("copy")}`;
      }

      setWorkflowJSON(JSON.stringify(workflow, null, 2));
      setEnabled(workflow?.enabled ?? true);
    },
    [mode, id],
  );

  const onSubmit: SubmitHandler<AttributeForm> = async () => {
    if (mode === "view") {
      navigate(toWorkflowDetail({ realm, mode: "copy", id: id! }));
      return;
    }

    try {
      const json = JSON.parse(workflowJSON);
      if (!json.name) {
        throw new Error(t("workflowNameRequired"));
      }

      const payload = {
        realm,
        ...json,
      };
      await adminClient.workflows.create(payload);

      addAlert(t("workflowCreated"), AlertVariant.success);
      navigate(toWorkflows({ realm }));
    } catch (error) {
      addError("workflowCreateError", error);
    }
  };

  const toggleEnabled = async () => {
    const json = JSON.parse(workflowJSON);
    json.enabled = !enabled;

    try {
      const payload = {
        realm,
        ...json,
      };
      await adminClient.workflows.update({ id: json.id }, payload);

      setWorkflowJSON(JSON.stringify(json, null, 2));
      setEnabled(!enabled);
      addAlert(
        enabled ? t("workflowDisabled") : t("workflowEnabled"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("workflowCreateError", error);
    }
  };

  const titlekeyMap: Record<WorkflowDetailParams["mode"], string> = {
    copy: "copyWorkflow",
    create: "createWorkflow",
    view: "viewWorkflow",
  };

  const subkeyMap: Record<WorkflowDetailParams["mode"], string> = {
    copy: "copyWorkflowDetails",
    create: "createWorkflowDetails",
    view: "viewWorkflowDetails",
  };

  return (
    <>
      <ViewHeader
        titleKey={titlekeyMap[mode]}
        subKey={subkeyMap[mode]}
        isEnabled={enabled}
        onToggle={mode === "view" ? toggleEnabled : undefined}
      />

      <FormProvider {...form}>
        <PageSection variant="light">
          <FormAccess
            isHorizontal
            onSubmit={handleSubmit(onSubmit)}
            role={"manage-realm"}
            className="pf-v5-u-mt-lg"
            fineGrainedAccess={true} // TODO: Set this properly
          >
            <FormGroup
              label={t("workflowJSON")}
              labelIcon={
                <HelpItem
                  helpText={t("workflowJsonHelp")}
                  fieldLabelId="code"
                />
              }
              fieldId="code"
              isRequired
            >
              <Controller
                name="workflowJSON"
                defaultValue=""
                control={control}
                render={() => (
                  <CodeEditor
                    id="workflowJSON"
                    data-testid="workflowJSON"
                    readOnly={mode === "view"}
                    value={workflowJSON}
                    onChange={(value) => setWorkflowJSON(value ?? "")}
                    language="json"
                    height={600}
                  />
                )}
              />
            </FormGroup>
            <ActionGroup>
              {mode !== "view" && (
                <FormSubmitButton
                  formState={form.formState}
                  data-testid="save"
                  allowInvalid
                  allowNonDirty
                >
                  {t("save")}
                </FormSubmitButton>
              )}
              {mode === "view" && (
                <FormSubmitButton
                  formState={form.formState}
                  data-testid="copy"
                  allowInvalid
                  allowNonDirty
                >
                  {t("copy")}
                </FormSubmitButton>
              )}
              <Button
                data-testid="cancel"
                variant="link"
                component={(props) => (
                  <Link {...props} to={toWorkflows({ realm })} />
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </PageSection>
      </FormProvider>
    </>
  );
}
