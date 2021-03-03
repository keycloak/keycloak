import React from "react";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { ViewHeader } from "../components/view-header/ViewHeader";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { UserForm } from "./UserForm";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
  const form = useForm<UserRepresentation>({ mode: "onChange" });
  const { addAlert } = useAlerts();
  const adminClient = useAdminClient();

  const save = async (user: UserRepresentation) => {
    try {
      await adminClient.users.create({ username: user!.username });

      addAlert(t("users:userCreated"), AlertVariant.success);
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
