import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type { UserProfileMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  isUserProfileError,
  setUserProfileServerError,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { TFunction } from "i18next";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { UserForm } from "./UserForm";
import { UserFormFields, toUserRepresentation } from "./form-state";
import { toUser } from "./routes/User";

import "./user-section.css";

export default function CreateUser() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const form = useForm<UserFormFields>({ mode: "onChange" });
  const [addedGroups, setAddedGroups] = useState<GroupRepresentation[]>([]);
  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();

  useFetch(
    () => adminClient.users.getProfileMetadata({ realm: realmName }),
    (userProfileMetadata) => {
      if (!userProfileMetadata) {
        throw new Error(t("notFound"));
      }

      setUserProfileMetadata(userProfileMetadata);
    },
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
      navigate(
        toUser({ id: createdUser.id, realm: realmName, tab: "settings" }),
      );
    } catch (error) {
      if (isUserProfileError(error)) {
        setUserProfileServerError(error, form.setError, ((key, param) =>
          t(key as string, param as any)) as TFunction);
      } else {
        addError("userCreateError", error);
      }
    }
  };

  if (!realm || !userProfileMetadata) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader
        titleKey={t("createUser")}
        className="kc-username-view-header"
      />
      <PageSection variant="light">
        <UserForm
          form={form}
          realm={realm}
          userProfileMetadata={userProfileMetadata}
          onGroupsUpdate={setAddedGroups}
          save={save}
        />
      </PageSection>
    </>
  );
}
