import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { UserProfileProvider } from "../realm-settings/user-profile/UserProfileContext";
import { toUser } from "./routes/User";
import { UserForm } from "./UserForm";
import {
  isUserProfileError,
  userProfileErrorToString,
} from "./UserProfileFields";

import "./user-section.css";

export default function CreateUser() {
  const { t } = useTranslation("users");
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const userForm = useForm<UserRepresentation>({ mode: "onChange" });
  const [addedGroups, setAddedGroups] = useState<GroupRepresentation[]>([]);

  const save = async (formUser: UserRepresentation) => {
    try {
      const createdUser = await adminClient.users.create({
        ...formUser,
        username: formUser.username?.trim(),
        groups: addedGroups.map((group) => group.path!),
        enabled: true,
      });

      addAlert(t("userCreated"), AlertVariant.success);
      navigate(toUser({ id: createdUser.id, realm, tab: "settings" }));
    } catch (error) {
      if (isUserProfileError(error)) {
        addError(userProfileErrorToString(error), error);
      } else {
        addError("users:userCreateError", error);
      }
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={t("createUser")}
        className="kc-username-view-header"
      />
      <PageSection variant="light" className="pf-u-p-0">
        <UserProfileProvider>
          <FormProvider {...userForm}>
            <PageSection variant="light">
              <UserForm onGroupsUpdate={setAddedGroups} save={save} />
            </PageSection>
          </FormProvider>
        </UserProfileProvider>
      </PageSection>
    </>
  );
}
