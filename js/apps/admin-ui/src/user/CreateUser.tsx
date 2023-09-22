import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { UserProfileProvider } from "../realm-settings/user-profile/UserProfileContext";
import { UserForm } from "./UserForm";
import {
  isUserProfileError,
  userProfileErrorToString,
} from "./UserProfileFields";
import { UserFormFields, toUserRepresentation } from "./form-state";
import { toUser } from "./routes/User";

import "./user-section.css";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useFetch } from "../utils/useFetch";

export default function CreateUser() {
  const { t } = useTranslation("users");
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const userForm = useForm<UserFormFields>({ mode: "onChange" });
  const [addedGroups, setAddedGroups] = useState<GroupRepresentation[]>([]);

  const [realmRepresentation, setRealmRepresentation] =
    useState<RealmRepresentation>();

  useFetch(
    () => adminClient.realms.findOne({ realm }),
    (result) => setRealmRepresentation(result),
    [],
  );

  const save = async (data: UserFormFields) => {
    try {
      const createdUser = await adminClient.users.create({
        ...toUserRepresentation(data),
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
              <UserForm
                realm={realmRepresentation}
                onGroupsUpdate={setAddedGroups}
                save={save}
              />
            </PageSection>
          </FormProvider>
        </UserProfileProvider>
      </PageSection>
    </>
  );
}
