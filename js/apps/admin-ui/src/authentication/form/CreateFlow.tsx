import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toAuthentication } from "../routes/Authentication";
import { toFlow } from "../routes/Flow";
import { FlowType } from "./FlowType";
import { NameDescription } from "./NameDescription";

export default function CreateFlow() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { addAlert } = useAlerts();
  const form = useForm<AuthenticationFlowRepresentation>();
  const { handleSubmit } = form;

  const onSubmit = async (formValues: AuthenticationFlowRepresentation) => {
    const flow = { ...formValues, builtIn: false, topLevel: true };

    try {
      const { id } =
        await adminClient.authenticationManagement.createFlow(flow);
      addAlert(t("flowCreatedSuccess"), AlertVariant.success);
      navigate(
        toFlow({
          realm,
          id: id!,
          usedBy: "notInUse",
        }),
      );
    } catch (error: any) {
      addAlert(
        t("flowCreateError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger,
      );
    }
  };

  return (
    <>
      <ViewHeader titleKey="createFlow" subKey="authenticationCreateFlowHelp" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            role="manage-authorization"
            onSubmit={handleSubmit(onSubmit)}
          >
            <NameDescription />
            <FlowType />
            <ActionGroup>
              <Button data-testid="create" type="submit">
                {t("create")}
              </Button>
              <Button
                data-testid="cancel"
                variant="link"
                component={(props) => (
                  <Link {...props} to={toAuthentication({ realm })}></Link>
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
