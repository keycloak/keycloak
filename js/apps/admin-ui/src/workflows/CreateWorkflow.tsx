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
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { FormAccess } from "../components/form/FormAccess";
import { toWorkflows } from "./routes/Workflows";
import CodeEditor from "../components/form/CodeEditor";

type AttributeForm = {
  workflowJSON?: string;
  [key: string]: any;
};

export default function CreateWorkflow() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const { control, handleSubmit } = form;
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [workflowJSON, setWorkflowJSON] = useState("");

  const onSubmit: SubmitHandler<AttributeForm> = async () => {
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

  return (
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
              <HelpItem helpText={t("workflowJsonHelp")} fieldLabelId="code" />
            }
            fieldId="code"
            isRequired
          >
            <Controller
              name="workflowJSON"
              defaultValue=""
              control={control}
              render={({ field }) => (
                <CodeEditor
                  id="workflowJSON"
                  data-testid="workflowJSON"
                  value={field.value}
                  onChange={(value) => setWorkflowJSON(value ?? "")}
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
              {t("save")}
            </FormSubmitButton>
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
  );
}
