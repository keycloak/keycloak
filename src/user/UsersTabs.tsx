import React from "react";
import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { ViewHeader } from "../components/view-header/ViewHeader";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { UserForm } from "./UserForm";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
  const form = useForm<UserRepresentation>({ mode: "onChange" });

  return (
    <>
      <ViewHeader
        titleKey={t("users:createUser")}
        subKey=""
        dividerComponent="div"
      />
      <PageSection variant="light">
        <UserForm form={form} save={() => {}} />
      </PageSection>
    </>
  );
};
