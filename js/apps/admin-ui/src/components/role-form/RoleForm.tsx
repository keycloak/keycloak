import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  ValidatedOptions,
} from "@patternfly/react-core";
import { SubmitHandler, UseFormReturn, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, To } from "react-router-dom";

import { FormAccess } from "../form/FormAccess";
import { AttributeForm } from "../key-value-form/AttributeForm";
import { KeycloakTextArea } from "../keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../view-header/ViewHeader";

export type RoleFormProps = {
  form: UseFormReturn<AttributeForm>;
  onSubmit: SubmitHandler<AttributeForm>;
  cancelLink: To;
  role: "manage-realm" | "manage-clients";
  editMode: boolean;
};

export const RoleForm = ({
  form: {
    register,
    control,
    handleSubmit,
    formState: { errors },
  },
  onSubmit,
  cancelLink,
  role,
  editMode,
}: RoleFormProps) => {
  const { t } = useTranslation();

  const roleName = useWatch({
    control,
    defaultValue: undefined,
    name: "name",
  });

  return (
    <>
      {!editMode && <ViewHeader titleKey={t("createRole")} />}
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
          role={role}
          className="pf-u-mt-lg"
        >
          <FormGroup
            label={t("roleName")}
            fieldId="kc-name"
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            helperTextInvalid={t("required")}
            isRequired={!editMode}
          >
            <KeycloakTextInput
              id="kc-name"
              isReadOnly={editMode}
              {...register("name", {
                required: !editMode,
                validate: (value) => {
                  if (!value?.trim()) {
                    return t("required").toString();
                  }
                },
              })}
            />
          </FormGroup>
          <FormGroup
            label={t("description")}
            fieldId="kc-description"
            validated={
              errors.description
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={errors.description?.message}
          >
            <KeycloakTextArea
              id="kc-description"
              validated={
                errors.description
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isDisabled={roleName?.includes("default-roles")}
              {...register("description", {
                maxLength: {
                  value: 255,
                  message: t("maxLength", { length: 255 }),
                },
              })}
            />
          </FormGroup>
          <ActionGroup>
            <Button data-testid="save" type="submit" variant="primary">
              {t("save")}
            </Button>
            <Button
              data-testid="cancel"
              variant="link"
              component={(props) => <Link {...props} to={cancelLink} />}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
