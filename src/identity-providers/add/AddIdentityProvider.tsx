import React from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";

import type { BreadcrumbData } from "use-react-router-breadcrumbs";
import type IdentityProviderRepresentation from "keycloak-admin/lib/defs/identityProviderRepresentation";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { toUpperCase } from "../../util";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { GeneralSettings } from "./GeneralSettings";

export const IdentityProviderCrumb = ({ match, location }: BreadcrumbData) => {
  const { t } = useTranslation();
  const {
    params: { id },
  } = match as unknown as {
    params: { [id: string]: string };
  };
  return (
    <>
      {t(
        `identity-providers:${
          location.pathname.endsWith("settings")
            ? "provider"
            : "addIdentityProvider"
        }`,
        {
          provider: toUpperCase(id),
        }
      )}
    </>
  );
};

export const AddIdentityProvider = () => {
  const { t } = useTranslation("identity-providers");
  const { id } = useParams<{ id: string }>();
  const form = useForm<IdentityProviderRepresentation>();
  const {
    handleSubmit,
    formState: { isDirty },
  } = form;

  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

  const save = async (provider: IdentityProviderRepresentation) => {
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId: id,
        alias: id,
      });
      addAlert(t("createSuccess"), AlertVariant.success);
      history.push(`/${realm}/identity-providers/${id}/settings`);
    } catch (error) {
      addAlert(
        t("createError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={t("addIdentityProvider", { provider: toUpperCase(id) })}
      />
      <PageSection variant="light">
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          <FormProvider {...form}>
            <GeneralSettings id={id} />
          </FormProvider>
          <ActionGroup>
            <Button
              isDisabled={!isDirty}
              variant="primary"
              type="submit"
              data-testid="createProvider"
            >
              {t("common:add")}
            </Button>
            <Button
              variant="link"
              data-testid="cancel"
              onClick={() => history.push(`/${realm}/identity-providers`)}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
