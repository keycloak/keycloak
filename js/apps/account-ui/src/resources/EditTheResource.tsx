import {
  SelectControl,
  TextControl,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { Button, Form, Modal } from "@patternfly/react-core";
import { Fragment, useEffect } from "react";
import { FormProvider, useFieldArray, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { updatePermissions } from "../api";
import type { Permission, Resource } from "../api/representations";
import { useAccountAlerts } from "../utils/useAccountAlerts";

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
  const { addAlert, addError } = useAccountAlerts();

  const form = useForm<FormValues>();
  const { control, reset, handleSubmit } = form;

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
      addError("updateError", error);
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
              <TextControl
                name={`permissions.${index}.username`}
                label={t("user")}
                isDisabled
              />
              <SelectControl
                id={`permissions-${p.id}`}
                name={`permissions.${index}.scopes`}
                label="permissions"
                variant="typeaheadMulti"
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
