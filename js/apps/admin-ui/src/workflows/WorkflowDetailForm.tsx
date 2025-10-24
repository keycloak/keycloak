import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { AlertVariant } from "@patternfly/react-core";
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
import WorkflowRepresentation from "libs/keycloak-admin-client/lib/defs/workflowRepresentation";

type AttributeForm = {
  workflowJSON: string;
};

export default function WorkflowDetailForm() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { mode, id } = useParams<WorkflowDetailParams>();
  const form = useForm<AttributeForm>({
    mode: "onChange",
    defaultValues: {
      workflowJSON: "",
    },
  });
  const { control, handleSubmit, setValue } = form;

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

      const workflowToSet = { ...workflow };
      if (mode === "copy") {
        delete workflowToSet.id;
        workflowToSet.name = `${workflow.name} -- ${t("copy")}`;
      }

      setValue("workflowJSON", JSON.stringify(workflowToSet, null, 2));
    },
    [mode, id, setValue, t],
  );

  const validateWorkflowJSON = (jsonStr: string): WorkflowRepresentation => {
    const json = JSON.parse(jsonStr);
    if (!json.name) {
      throw new Error(t("workflowNameRequired"));
    }
    return json;
  };

  const onUpdate: SubmitHandler<AttributeForm> = async (data) => {
    try {
      const json = validateWorkflowJSON(data.workflowJSON);
      await adminClient.workflows.update({ id: json.id! }, json);
      addAlert(t("workflowUpdated"), AlertVariant.success);
    } catch (error) {
      addError("workflowUpdateError", error);
    }
  };

  const onCreate: SubmitHandler<AttributeForm> = async (data) => {
    try {
      const json = validateWorkflowJSON(data.workflowJSON);
      await adminClient.workflows.create(json);
      addAlert(t("workflowCreated"), AlertVariant.success);
      navigate(toWorkflows({ realm }));
    } catch (error) {
      addError("workflowCreateError", error);
    }
  };

  const titlekeyMap: Record<WorkflowDetailParams["mode"], string> = {
    copy: "copyWorkflow",
    create: "createWorkflow",
    update: "updateWorkflow",
  };

  const subkeyMap: Record<WorkflowDetailParams["mode"], string> = {
    copy: "copyWorkflowDetails",
    create: "createWorkflowDetails",
    update: "updateWorkflowDetails",
  };

  return (
    <>
      <ViewHeader titleKey={titlekeyMap[mode]} subKey={subkeyMap[mode]} />

      <FormProvider {...form}>
        <PageSection variant="light">
          <FormAccess
            isHorizontal
            onSubmit={
              mode === "update"
                ? handleSubmit(onUpdate)
                : handleSubmit(onCreate)
            }
            role={"manage-realm"}
            className="pf-v5-u-mt-lg"
            fineGrainedAccess={true}
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
                control={control}
                render={({ field }) => (
                  <CodeEditor
                    id="workflowJSON"
                    data-testid="workflowJSON"
                    value={field.value}
                    onChange={field.onChange}
                    language="json"
                    height={600}
                  />
                )}
              />
            </FormGroup>
            <ActionGroup>
              <FormSubmitButton
                formState={form.formState}
                data-testid="save"
                allowInvalid
                allowNonDirty
              >
                {mode === "update" ? t("save") : t("create")}
              </FormSubmitButton>
              {mode === "update" && (
                <Button
                  data-testid="copy"
                  variant="link"
                  component={(props) => (
                    <Link
                      {...props}
                      to={toWorkflowDetail({ realm, mode: "copy", id: id! })}
                    />
                  )}
                >
                  {t("copy")}
                </Button>
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
