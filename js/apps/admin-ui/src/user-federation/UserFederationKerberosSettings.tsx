import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import { useParams } from "../utils/useParams";
import { KerberosSettingsRequired } from "./kerberos/KerberosSettingsRequired";
import { toUserFederation } from "./routes/UserFederation";
import { Header } from "./shared/Header";
import { SettingsCache } from "./shared/SettingsCache";

export default function UserFederationKerberosSettings() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<ComponentRepresentation>({ mode: "onChange" });
  const navigate = useNavigate();
  const { realm } = useRealm();

  const { id } = useParams<{ id?: string }>();

  const { addAlert, addError } = useAlerts();

  useFetch(
    async () => {
      if (id) {
        return adminClient.components.findOne({ id });
      }
    },
    (fetchedComponent) => {
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      } else if (id) {
        throw new Error(t("notFound"));
      }
    },
    [],
  );

  const setupForm = (component: ComponentRepresentation) => {
    form.reset({ ...component });
  };

  const save = async (component: ComponentRepresentation) => {
    try {
      if (!id) {
        await adminClient.components.create(component);
        navigate(`/${realm}/user-federation`);
      } else {
        await adminClient.components.update({ id }, component);
      }
      setupForm(component as ComponentRepresentation);
      addAlert(
        t(!id ? "createUserProviderSuccess" : "userProviderSaveSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError(
        !id ? "createUserProviderError" : "userProviderSaveError",
        error,
      );
    }
  };

  return (
    <FormProvider {...form}>
      <Header provider="Kerberos" save={() => form.handleSubmit(save)()} />
      <PageSection variant="light">
        <KerberosSettingsRequired form={form} showSectionHeading />
      </PageSection>
      <PageSection variant="light" isFilled>
        <SettingsCache form={form} showSectionHeading />
        <Form onSubmit={form.handleSubmit(save)}>
          <ActionGroup>
            <Button
              isDisabled={!form.formState.isDirty}
              variant="primary"
              type="submit"
              data-testid="kerberos-save"
            >
              {t("save")}
            </Button>
            <Button
              variant="link"
              onClick={() => navigate(toUserFederation({ realm }))}
              data-testid="kerberos-cancel"
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </FormProvider>
  );
}
