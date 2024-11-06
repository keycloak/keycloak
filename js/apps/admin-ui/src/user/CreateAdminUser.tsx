import { UserProfileMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  isUserProfileError,
  KeycloakSpinner,
  setUserProfileServerError,
  useAlerts,
  useFetch,
  UserProfileFields,
} from "@keycloak/keycloak-ui-shared";
import { PageSection } from "@patternfly/react-core";
import { TFunction } from "i18next";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";
import { FormAccess } from "../components/form/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { toUserRepresentation, UserFormFields } from "./form-state";
import { toUser } from "./routes/User";
import { toUsers } from "./routes/Users";
import { ResetPasswordForm } from "./user-credentials/ResetPasswordForm";

type AdminUserFields = UserFormFields & {
  password: string;
  passwordConfirmation: string;
};

export default function CreateAdminUser() {
  const { t } = useTranslation();
  const form = useForm<AdminUserFields>({ mode: "onChange" });
  const { handleSubmit } = form;

  const { whoAmI } = useWhoAmI();
  const currentLocale = whoAmI.getLocale();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();

  useFetch(
    () => adminClient.users.getProfileMetadata({ realm: realmName }),
    (userProfileMetadata) => {
      if (!userProfileMetadata) {
        throw new Error(t("notFound"));
      }

      form.setValue("attributes.locale", realm?.defaultLocale || "");
      setUserProfileMetadata(userProfileMetadata);
    },
    [],
  );

  const save = async (data: AdminUserFields) => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { password, passwordConfirmation, ...user } = data;
      const createdUser = await adminClient.users.create({
        ...toUserRepresentation(user),
        enabled: true,
      });
      await adminClient.users.resetPassword({
        id: createdUser.id!,
        credential: {
          type: "password",
          value: password,
        },
      });
      const role = await adminClient.roles.findOneByName({ name: "admin" });
      await adminClient.users.addRealmRoleMappings({
        id: createdUser.id,
        roles: [{ name: role!.name!, id: role!.id! }],
      });

      addAlert(t("userCreated"));
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
      <ViewHeader titleKey={t("createUser")} />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="query-users"
        >
          <FormProvider {...form}>
            <UserProfileFields
              form={form as any}
              userProfileMetadata={userProfileMetadata}
              supportedLocales={realm.supportedLocales || []}
              currentLocale={currentLocale}
              t={t}
            />
            <ResetPasswordForm />
          </FormProvider>
          <FixedButtonsGroup
            name="admin-user-creation"
            saveText={t("create")}
            reset={() => navigate(toUsers({ realm: realm.realm! }))}
            resetText={t("cancel")}
            isSubmit
          />
        </FormAccess>
      </PageSection>
    </>
  );
}
