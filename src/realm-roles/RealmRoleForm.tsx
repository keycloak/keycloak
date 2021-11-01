import React from "react";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  TextArea,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import type { UseFormMethods } from "react-hook-form";
import { FormAccess } from "../components/form-access/FormAccess";
import type { AttributeForm } from "../components/attribute-form/AttributeForm";

export type RealmRoleFormProps = {
  form: UseFormMethods<AttributeForm>;
  save: () => void;
  editMode: boolean;
  reset: () => void;
};

export const RealmRoleForm = ({
  form: { handleSubmit, errors, register, getValues },
  save,
  editMode,
  reset,
}: RealmRoleFormProps) => {
  const { t } = useTranslation("roles");

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        onSubmit={handleSubmit(save)}
        role="manage-realm"
        className="pf-u-mt-lg"
      >
        <FormGroup
          label={t("roleName")}
          fieldId="kc-name"
          isRequired
          validated={errors.name ? "error" : "default"}
          helperTextInvalid={t("common:required")}
        >
          <TextInput
            ref={register({ required: !editMode })}
            type="text"
            id="kc-name"
            name="name"
            isReadOnly={editMode}
          />
        </FormGroup>
        <FormGroup
          label={t("common:description")}
          fieldId="kc-description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          helperTextInvalid={errors.description?.message}
        >
          <TextArea
            name="description"
            aria-label="description"
            isDisabled={getValues().name?.includes("default-roles")}
            ref={register({
              maxLength: {
                value: 255,
                message: t("common:maxLength", { length: 255 }),
              },
            })}
            type="text"
            validated={
              errors.description
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            id="kc-role-description"
          />
        </FormGroup>
        <ActionGroup>
          <Button
            variant="primary"
            onClick={save}
            data-testid="realm-roles-save-button"
          >
            {t("common:save")}
          </Button>
          <Button onClick={() => reset()} variant="link">
            {editMode ? t("common:revert") : t("common:cancel")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
