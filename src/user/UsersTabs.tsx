import React from "react";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { ViewHeader } from "../components/view-header/ViewHeader";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { UserForm } from "./UserForm";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import { useHistory, useRouteMatch } from "react-router-dom";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
  const { addAlert } = useAlerts();
  const { url } = useRouteMatch();
  const history = useHistory();

  const adminClient = useAdminClient();
  const form = useForm<UserRepresentation>({ mode: "onChange" });

  const save = async (user: UserRepresentation) => {
    try {
      await adminClient.users.create({
        username: user!.username,
        email: user!.email,
        emailVerified: user!.emailVerified,
        firstName: user!.firstName,
        lastName: user!.lastName,
        enabled: user!.enabled,
        requiredActions: user!.requiredActions,
      });
      addAlert(t("users:userCreated"), AlertVariant.success);
      history.push(url.substr(0, url.lastIndexOf("/")));
    } catch (error) {
      addAlert(
        t("users:userCreateError", {
          error: error.response.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={t("users:createUser")}
        subKey=""
        dividerComponent="div"
      />
      <PageSection variant="light">
        <UserForm form={form} save={save} />
      </PageSection>
    </>
  );
};
