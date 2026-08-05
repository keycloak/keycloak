import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  TextAreaControl,
  TextControl,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import { Button, Form, Modal, ModalVariant } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";

type OrganizationRoleForm = Pick<RoleRepresentation, "name" | "description">;

type CreateOrganizationRoleModalProps = {
  organizationId: string;
  onClose: () => void;
  onCreated: () => void;
};

export const CreateOrganizationRoleModal = ({
  organizationId,
  onClose,
  onCreated,
}: CreateOrganizationRoleModalProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const form = useForm<OrganizationRoleForm>({ mode: "onChange" });

  const save = async (role: OrganizationRoleForm) => {
    try {
      await adminClient.organizations.createRole({
        orgId: organizationId,
        ...role,
        name: role.name?.trim(),
      });
      addAlert(t("organizationRoleCreated"));
      onCreated();
    } catch (error) {
      addError("organizationRoleCreateError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("createOrganizationRole")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          key="create"
          variant="primary"
          type="submit"
          form="create-organization-role-form"
          isLoading={form.formState.isSubmitting}
          isDisabled={form.formState.isSubmitting}
        >
          {t("create")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form
          id="create-organization-role-form"
          onSubmit={form.handleSubmit(save)}
        >
          <TextControl
            name="name"
            label={t("roleName")}
            rules={{
              required: t("required"),
              validate: (value) => value?.trim().length > 0 || t("required"),
            }}
          />
          <TextAreaControl
            name="description"
            label={t("description")}
            rules={{
              maxLength: {
                value: 255,
                message: t("maxLength", { length: 255 }),
              },
            }}
          />
        </Form>
      </FormProvider>
    </Modal>
  );
};
