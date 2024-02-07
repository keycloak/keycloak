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

type NewOrgRoleProps = {
  orgId: string;
  handleModalToggle: () => void;
  refresh: (role?: RoleRepresentation) => void;
};

export const NewOrgRoleModal = ({
  handleModalToggle,
  orgId,
  refresh,
}: NewOrgRoleProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { createRoleForOrg } = useOrgFetcher(realm);
  const { addAlert, addError } = useAlerts();
  const {
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm({});

  const submitForm = async (role: RoleRepresentation) => {
    try {
      const resp = await createRoleForOrg(orgId, role);
      if (resp.success) {
        refresh(role);
        handleModalToggle();
        addAlert("Role created for this organization", AlertVariant.success);
        return;
      }
      throw new Error(resp.message);
    } catch (e: any) {
      addError(
        `Could not create the role for this organization, ${e.message}`,
        e,
      );
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("createRole")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid={`createRole`}
          key="confirm"
          variant="primary"
          type="submit"
          form="role-name-form"
          isDisabled={isSubmitting}
        >
          {t("create")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
          isDisabled={isSubmitting}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form
        id="role-name-form"
        isHorizontal
        onSubmit={handleSubmit(submitForm)}
      >
        <FormGroup
          name="create-role-name"
          label={t("roleName")}
          fieldId="role-name"
          helperText="All lowercase, no spaces."
          helperTextInvalid={t("required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <Controller
            name="name"
            control={control}
            rules={{ required: true, pattern: /\b[a-z]+/ }}
            render={({ field }) => (
              <TextInput
                id="create-role-name"
                value={field.value}
                onChange={field.onChange}
                data-testid="create-role-name-input"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            )}
          />
        </FormGroup>

        <FormGroup
          name="role-description"
          label={t("description")}
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
