import React from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation from "keycloak-admin/lib/defs/authenticationFlowRepresentation";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { NameDescription } from "./NameDescription";
import { FlowType } from "./FlowType";

export const CreateFlow = () => {
  const { t } = useTranslation("authentication");
  const history = useHistory();
  const { realm } = useRealm();
  const form = useForm<AuthenticationFlowRepresentation>({
    defaultValues: { builtIn: false, topLevel: true },
  });
  const { handleSubmit, register } = form;

  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const save = async (flow: AuthenticationFlowRepresentation) => {
    try {
      await adminClient.authenticationManagement.createFlow(flow);
      addAlert(t("flowCreatedSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("flowCreateError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader
        titleKey="authentication:createFlow"
        subKey="authentication-help:createFlow"
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            role="manage-authorization"
            onSubmit={handleSubmit(save)}
          >
            <input name="builtIn" type="hidden" ref={register} />
            <input name="topLevel" type="hidden" ref={register} />
            <NameDescription />
            <FlowType />
            <ActionGroup>
              <Button data-testid="create" type="submit">
                {t("common:create")}
              </Button>
              <Button
                data-testid="cancel"
                variant="link"
                onClick={() => history.push(`/${realm}/authentication`)}
              >
                {t("common:cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
};
