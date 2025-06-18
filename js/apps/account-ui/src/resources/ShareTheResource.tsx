import {
  FormErrorText,
  SelectControl,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Chip,
  ChipGroup,
  Form,
  FormGroup,
  InputGroup,
  InputGroupItem,
  Modal,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useEffect } from "react";
import {
  FormProvider,
  useFieldArray,
  useForm,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import { updateRequest } from "../api";
import { Permission, Resource } from "../api/representations";
import { useAccountAlerts } from "../utils/useAccountAlerts";
import { SharedWith } from "./SharedWith";

type ShareTheResourceProps = {
  resource: Resource;
  permissions?: Permission[];
  open: boolean;
  onClose: () => void;
};

type FormValues = {
  permissions: string[];
  usernames: { value: string }[];
};

export const ShareTheResource = ({
  resource,
  permissions,
  open,
  onClose,
}: ShareTheResourceProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();
  const form = useForm<FormValues>();
  const {
    control,
    register,
    reset,
    formState: { errors, isValid },
    setError,
    clearErrors,
    handleSubmit,
  } = form;
  const { fields, append, remove } = useFieldArray<FormValues>({
    control,
    name: "usernames",
  });

  useEffect(() => {
    if (fields.length === 0) {
      append({ value: "" });
    }
  }, [fields]);

  const watchFields = useWatch({
    control,
    name: "usernames",
    defaultValue: [],
  });

  const isDisabled = watchFields.every(
    ({ value }) => value.trim().length === 0,
  );

  const addShare = async ({ usernames, permissions }: FormValues) => {
    try {
      await Promise.all(
        usernames
          .filter(({ value }) => value !== "")
          .map(({ value: username }) =>
            updateRequest(context, resource._id, username, permissions),
          ),
      );
      addAlert(t("shareSuccess"));
      onClose();
    } catch (error) {
      addError("shareError", error);
    }
    reset({});
  };

  const validateUser = async () => {
    const userOrEmails = fields.map((f) => f.value).filter((f) => f !== "");
    const userPermission = permissions
      ?.map((p) => [p.username, p.email])
      .flat();

    const hasUsers = userOrEmails.length > 0;
    const alreadyShared =
      userOrEmails.filter((u) => userPermission?.includes(u)).length !== 0;

    if (!hasUsers || alreadyShared) {
      setError("usernames", {
        message: !hasUsers ? t("required") : t("resourceAlreadyShared"),
      });
    } else {
      clearErrors();
    }

    return hasUsers && !alreadyShared;
  };

  return (
    <Modal
      title={t("shareTheResource", { name: resource.name })}
      variant="medium"
      isOpen={open}
      onClose={onClose}
      actions={[
        <Button
          key="confirm"
          variant="primary"
          data-testid="done"
          isDisabled={!isValid}
          type="submit"
          form="share-form"
        >
          {t("done")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form id="share-form" onSubmit={handleSubmit(addShare)}>
        <FormGroup
          label={t("shareUser")}
          type="string"
          fieldId="users"
          isRequired
        >
          <InputGroup>
            <InputGroupItem>
              <TextInput
                id="users"
                data-testid="users"
                placeholder={t("usernamePlaceholder")}
                validated={
                  errors.usernames
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                {...register(`usernames.${fields.length - 1}.value`, {
                  validate: validateUser,
                })}
              />
            </InputGroupItem>
            <InputGroupItem>
              <Button
                key="add-user"
                variant="primary"
                data-testid="add"
                onClick={() => append({ value: "" })}
                isDisabled={isDisabled}
              >
                {t("add")}
              </Button>
            </InputGroupItem>
          </InputGroup>
          {fields.length > 1 && (
            <ChipGroup categoryName={t("shareWith") + " "}>
              {fields.map(
                (field, index) =>
                  index !== fields.length - 1 && (
                    <Chip key={field.id} onClick={() => remove(index)}>
                      {field.value}
                    </Chip>
                  ),
              )}
            </ChipGroup>
          )}
          {errors.usernames && (
            <FormErrorText message={errors.usernames.message!} />
          )}
        </FormGroup>
        <FormProvider {...form}>
          <FormGroup
            label=""
            fieldId="permissions-selected"
            data-testid="permissions"
          >
            <SelectControl
              name="permissions"
              variant="typeaheadMulti"
              controller={{ defaultValue: [] }}
              options={resource.scopes.map(({ name, displayName }) => ({
                key: name,
                value: displayName || name,
              }))}
            />
          </FormGroup>
        </FormProvider>
        <FormGroup>
          <SharedWith permissions={permissions} />
        </FormGroup>
      </Form>
    </Modal>
  );
};
