import React, { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { useRealm } from "../context/realm-context/RealmContext";
import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { AttributeGeneralSettings } from "./user-profile/attribute/AttributeGeneralSettings";
import { AttributePermission } from "./user-profile/attribute/AttributePermission";
import { AttributeValidations } from "./user-profile/attribute/AttributeValidations";
import { toUserProfile } from "./routes/UserProfile";
import "./realm-settings-section.css";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { AttributeAnnotations } from "./user-profile/attribute/AttributeAnnotations";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { UserProfileProvider } from "./user-profile/UserProfileContext";
import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import type { KeyValueType } from "../components/attribute-form/attribute-convert";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";

type UserProfileAttributeType = UserProfileAttribute &
  AttributeRequired &
  Permission;

type AttributeRequired = {
  roles: string[];
  scopeRequired: string[];
  enabledWhen: boolean;
  requiredWhen: boolean;
};

type Permission = {
  view: PermissionView[];
  edit: PermissionEdit[];
};

type PermissionView = [
  {
    adminView: boolean;
    userView: boolean;
  }
];

type PermissionEdit = [
  {
    adminEdit: boolean;
    userEdit: boolean;
  }
];

const CreateAttributeFormContent = ({
  save,
}: {
  save: (profileConfig: UserProfileConfig) => void;
}) => {
  const { t } = useTranslation("realm-settings");
  const form = useFormContext();
  const { realm } = useRealm();

  return (
    <UserProfileProvider>
      <ScrollForm
        sections={[
          t("generalSettings"),
          t("permission"),
          t("validations"),
          t("annotations"),
        ]}
      >
        <AttributeGeneralSettings />
        <AttributePermission />
        <AttributeValidations />
        <AttributeAnnotations />
      </ScrollForm>
      <Form onSubmit={form.handleSubmit(save)}>
        <ActionGroup className="keycloak__form_actions">
          <Button
            variant="primary"
            type="submit"
            data-testid="attribute-create"
          >
            {t("common:create")}
          </Button>
          <Link
            to={toUserProfile({ realm, tab: "attributes" })}
            data-testid="attribute-cancel"
            className="kc-attributeCancel"
          >
            {t("common:cancel")}
          </Link>
        </ActionGroup>
      </Form>
    </UserProfileProvider>
  );
};

export default function NewAttributeSettings() {
  const { realm: realmName } = useRealm();
  const adminClient = useAdminClient();
  const form = useForm<UserProfileConfig>();
  const { t } = useTranslation("realm-settings");
  const history = useHistory();
  const { addAlert, addError } = useAlerts();
  const [config, setConfig] = useState<UserProfileConfig | null>(null);
  const [clientScopes, setClientScopes] =
    useState<ClientScopeRepresentation[]>();

  useFetch(
    () =>
      Promise.all([
        adminClient.users.getProfile({ realm: realmName }),
        adminClient.clientScopes.find(),
      ]),
    ([config, clientScopes]) => {
      setConfig(config);
      setClientScopes(clientScopes);
    },
    []
  );

  const save = async (profileConfig: UserProfileAttributeType) => {
    const scopeNames = clientScopes?.map((clientScope) => clientScope.name);

    const selector = {
      scopes: profileConfig.enabledWhen
        ? scopeNames
        : profileConfig.selector?.scopes,
    };

    const required = {
      roles: profileConfig.roles,
      scopes: profileConfig.requiredWhen
        ? scopeNames
        : profileConfig.scopeRequired,
    };

    const validations = profileConfig.validations;

    const annotations = (profileConfig.annotations! as KeyValueType[]).reduce(
      (obj, item) => Object.assign(obj, { [item.key]: item.value }),
      {}
    );

    const newAttribute = [
      {
        name: profileConfig.name,
        displayName: profileConfig.displayName,
        required,
        validations,
        selector,
        permissions: profileConfig.permissions,
        annotations,
      },
    ];

    const newAttributesList = config?.attributes!.concat(
      newAttribute as UserProfileAttribute
    );

    try {
      await adminClient.users.updateProfile({
        attributes: newAttributesList,
        realm: realmName,
      });

      history.push(toUserProfile({ realm: realmName, tab: "attributes" }));

      addAlert(
        t("realm-settings:createAttributeSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("realm-settings:createAttributeError", error);
    }
  };

  return (
    <FormProvider {...form}>
      <ViewHeader
        titleKey={t("createAttribute")}
        subKey={t("createAttributeSubTitle")}
      />
      <PageSection variant="light">
        <CreateAttributeFormContent save={() => form.handleSubmit(save)()} />
      </PageSection>
    </FormProvider>
  );
}
