import { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { AttributeGeneralSettings } from "./user-profile/attribute/AttributeGeneralSettings";
import { AttributePermission } from "./user-profile/attribute/AttributePermission";
import { AttributeValidations } from "./user-profile/attribute/AttributeValidations";
import { toUserProfile } from "./routes/UserProfile";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { AttributeAnnotations } from "./user-profile/attribute/AttributeAnnotations";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { UserProfileProvider } from "./user-profile/UserProfileContext";
import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import type { AttributeParams } from "./routes/Attribute";
import { convertToFormValues } from "../util";
import { flatten } from "flat";

import "./realm-settings-section.css";

type IndexedAnnotations = {
  key: string;
  value: unknown;
};

export type IndexedValidations = {
  key: string;
  value?: Record<string, unknown>[];
};

type UserProfileAttributeType = Omit<
  UserProfileAttribute,
  "validations" | "annotations"
> &
  Attribute &
  Permission & {
    validations: IndexedValidations[];
    annotations: IndexedAnnotations[];
  };

type Attribute = {
  roles: string[];
  scopes: string[];
  isRequired: boolean;
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

export const USERNAME_EMAIL = ["username", "email"];

const CreateAttributeFormContent = ({
  save,
}: {
  save: (profileConfig: UserProfileConfig) => void;
}) => {
  const { t } = useTranslation("realm-settings");
  const form = useFormContext();
  const { realm, attributeName } = useParams<AttributeParams>();
  const editMode = attributeName ? true : false;

  return (
    <UserProfileProvider>
      <ScrollForm
        sections={[
          { title: t("generalSettings"), panel: <AttributeGeneralSettings /> },
          { title: t("permission"), panel: <AttributePermission /> },
          { title: t("validations"), panel: <AttributeValidations /> },
          { title: t("annotations"), panel: <AttributeAnnotations /> },
        ]}
      />
      <Form onSubmit={form.handleSubmit(save)}>
        <ActionGroup className="keycloak__form_actions">
          <Button
            variant="primary"
            type="submit"
            data-testid="attribute-create"
          >
            {editMode ? t("common:save") : t("common:create")}
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
  const { realm, attributeName } = useParams<AttributeParams>();
  const { adminClient } = useAdminClient();
  const form = useForm<UserProfileConfig>({ shouldUnregister: false });
  const { t } = useTranslation("realm-settings");
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const [config, setConfig] = useState<UserProfileConfig | null>(null);
  const editMode = attributeName ? true : false;

  useFetch(
    () => adminClient.users.getProfile(),
    (config) => {
      setConfig(config);
      const {
        annotations,
        validations,
        permissions,
        selector,
        required,
        ...values
      } =
        config.attributes!.find(
          (attribute) => attribute.name === attributeName
        ) || {};
      convertToFormValues(values, form.setValue);
      Object.entries(
        flatten<any, any>({ permissions, selector, required }, { safe: true })
      ).map(([key, value]) => form.setValue(key, value));
      form.setValue(
        "annotations",
        Object.entries(annotations || {}).map(([key, value]) => ({
          key,
          value,
        }))
      );
      form.setValue(
        "validations",
        Object.entries(validations || {}).map(([key, value]) => ({
          key,
          value,
        }))
      );
      form.setValue("isRequired", required !== undefined);
    },
    []
  );

  const save = async (profileConfig: UserProfileAttributeType) => {
    const validations = profileConfig.validations.reduce(
      (prevValidations, currentValidations) => {
        prevValidations[currentValidations.key] =
          currentValidations.value?.length === 0
            ? {}
            : currentValidations.value;
        return prevValidations;
      },
      {} as Record<string, unknown>
    );

    const annotations = profileConfig.annotations.reduce(
      (obj, item) => Object.assign(obj, { [item.key]: item.value }),
      {}
    );

    const patchAttributes = () =>
      config?.attributes!.map((attribute) => {
        if (attribute.name !== attributeName) {
          return attribute;
        }

        delete attribute.required;
        return Object.assign(
          {
            ...attribute,
            name: attributeName,
            displayName: profileConfig.displayName!,
            selector: profileConfig.selector,
            permissions: profileConfig.permissions!,
            annotations,
            validations,
          },
          profileConfig.isRequired
            ? { required: profileConfig.required }
            : undefined,
          profileConfig.group ? { group: profileConfig.group } : undefined
        );
      });

    const addAttribute = () =>
      config?.attributes!.concat([
        Object.assign(
          {
            name: profileConfig.name,
            displayName: profileConfig.displayName!,
            required: profileConfig.isRequired ? profileConfig.required : {},
            selector: profileConfig.selector,
            permissions: profileConfig.permissions!,
            annotations,
          },
          profileConfig.isRequired
            ? { required: profileConfig.required }
            : undefined,
          profileConfig.group ? { group: profileConfig.group } : undefined
        ),
      ] as UserProfileAttribute);

    const updatedAttributes = editMode ? patchAttributes() : addAttribute();

    try {
      await adminClient.users.updateProfile({
        ...config,
        attributes: updatedAttributes as UserProfileAttribute[],
        realm,
      });

      navigate(toUserProfile({ realm, tab: "attributes" }));

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
        titleKey={editMode ? attributeName : t("createAttribute")}
        subKey={editMode ? "" : t("createAttributeSubTitle")}
      />
      <PageSection variant="light">
        <CreateAttributeFormContent save={() => form.handleSubmit(save)()} />
      </PageSection>
    </FormProvider>
  );
}
