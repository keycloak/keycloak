import {
  Button,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextArea,
  TextInput,
} from "@patternfly/react-core";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

interface OrganizationRoleForm {
  name: string;
  description?: string;
}

interface CreateOrganizationRoleModalProps {
  organizationId: string;
  onClose: () => void;
  onSuccess: () => void;
}

export function CreateOrganizationRoleModal({
  organizationId,
  onClose,
  onSuccess,
}: CreateOrganizationRoleModalProps) {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<OrganizationRoleForm>();

  const save = async (role: OrganizationRoleForm): Promise<void> => {
    try {
      await adminClient.organizations.createRole({
        orgId: organizationId,
        ...role,
      });
      
      addAlert(t("roleCreatedSuccess"));
      onSuccess();
    } catch (error) {
      addError("roleCreateError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("createRole")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          key="save"
          variant="primary"
          type="submit"
          form="create-role-form"
          isLoading={isSubmitting}
          isDisabled={isSubmitting}
        >
          {t("create")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form id="create-role-form" onSubmit={handleSubmit(save)}>
        <FormGroup label={t("name")} fieldId="name" isRequired>
          <TextInput
            id="name"
            data-testid="name"
            {...register("name", { required: t("required") })}
            validated={errors.name ? "error" : "default"}
          />
          {errors.name && (
            <div className="pf-v5-c-form__helper-text pf-m-error">
              {errors.name.message}
            </div>
          )}
        </FormGroup>
        <FormGroup label={t("description")} fieldId="description">
          <TextArea
            id="description"
            data-testid="description"
            {...register("description")}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
}
