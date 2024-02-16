import { Button, Form, FormGroup, Modal } from "@patternfly/react-core";
import { Fragment, useEffect } from "react";
import { FormProvider, useFieldArray, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { KeycloakTextInput, SelectControl, useAlerts } from "ui-shared";
import { updatePermissions } from "../api";
import type { Permission, Resource } from "../api/representations";
import { useEnvironment } from "../root/KeycloakContext";

type EditTheResourceProps = {
  resource: Resource;
  permissions?: Permission[];
  onClose: () => void;
};

type FormValues = {
  permissions: Permission[];
};

export const EditTheResource = ({
  resource,
  permissions,
  onClose,
}: EditTheResourceProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAlerts();

  const form = useForm<FormValues>();
  const { control, register, reset, handleSubmit } = form;

  const { fields } = useFieldArray<FormValues>({
    control,
    name: "permissions",
  });

  useEffect(() => reset({ permissions }), []);

  const editShares = async ({ permissions }: FormValues) => {
    try {
      await Promise.all(
        permissions.map((permission) =>
          updatePermissions(context, resource._id, [permission]),
        ),
      );
      addAlert(t("updateSuccess"));
      onClose();
    } catch (error) {
      addError(t("updateError", { error }).toString());
    }
  };

  return (
    <Modal
      title={t("editTheResource", { name: resource.name })}
      variant="medium"
      isOpen
      onClose={onClose}
      actions={[
        <Button
          key="confirm"
          variant="primary"
          id="done"
          type="submit"
          form="edit-form"
        >
          {t("done")}
        </Button>,
      ]}
    >
      <Form id="edit-form" onSubmit={handleSubmit(editShares)}>
        <FormProvider {...form}>
          {fields.map((p, index) => (
            <Fragment key={p.id}>
              <FormGroup label={t("user")} fieldId={`user-${p.id}`}>
                <KeycloakTextInput
                  id={`user-${p.id}`}
                  type="text"
                  {...register(`permissions.${index}.username`)}
                  isDisabled
                />
              </FormGroup>
              <SelectControl
                id={`permissions-${p.id}`}
                name={`permissions.${index}.scopes`}
                label="permissions"
                variant="typeaheadmulti"
                controller={{ defaultValue: [] }}
                options={resource.scopes.map(({ name, displayName }) => ({
                  key: name,
                  value: displayName || name,
                }))}
              />
            </Fragment>
          ))}
        </FormProvider>
      </Form>
    </Modal>
  );
};
