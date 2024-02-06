import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";

import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { useAlerts } from "../../components/alert/Alerts";
import useOrgFetcher from "../useOrgFetcher";
import { useRealm } from "../../context/realm-context/RealmContext";

type EditOrgRoleProps = {
  orgId: string;
  handleModalToggle: () => void;
  refresh: (role?: RoleRepresentation) => void;
  role: RoleRepresentation;
};

export const EditOrgRoleModal = ({
  handleModalToggle,
  orgId,
  refresh,
  role,
}: EditOrgRoleProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { updateRoleForOrg } = useOrgFetcher(realm);
  const { addAlert, addError } = useAlerts();
  const {
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      name: role.name,
      description: role.description,
    },
  });

  const submitForm = async (role: RoleRepresentation) => {
    try {
      const resp = await updateRoleForOrg(orgId, role);
      if (resp.success) {
        refresh(role);
        handleModalToggle();
        addAlert("Role updated for this organization", AlertVariant.success);
        return;
      }
      throw new Error(resp.message);
    } catch (e: any) {
      addError(
        `Could not update the role for this organization. ${e.message}`,
        e,
      );
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("editRole")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid={`updateRole`}
          key="confirm"
          variant="primary"
          type="submit"
          form="role-name-form"
          isDisabled={isSubmitting}
        >
          {t("common:save")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form
        id="role-name-form"
        isHorizontal
        onSubmit={handleSubmit(submitForm)}
      >
        <FormGroup
          name="update-role-name"
          label={t("roleName")}
          fieldId="role-name"
          helperTextInvalid={t("common:required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
          disabled
        >
          <Controller
            name="name"
            control={control}
            rules={{ required: true, pattern: /\b[a-z]+/ }}
            render={({ field }) => (
              <TextInput
                id="update-role-name"
                value={field.value}
                onChange={field.onChange}
                data-testid="update-role-name-input"
                autoFocus
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                isDisabled
              />
            )}
          />
        </FormGroup>

        <FormGroup
          name="role-description"
          label={t("common:description")}
          fieldId="role-description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        >
          <Controller
            name="description"
            control={control}
            render={({ field }) => (
              <TextInput
                id="role-description"
                value={field.value}
                onChange={field.onChange}
                data-testid="role-description-input"
                validated={
                  errors.description
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            )}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
