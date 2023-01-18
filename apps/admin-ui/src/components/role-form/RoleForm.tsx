import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  ValidatedOptions,
} from "@patternfly/react-core";
import { SubmitHandler, useWatch, UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, To } from "react-router-dom-v5-compat";

import { FormAccess } from "../form-access/FormAccess";
import { AttributeForm } from "../key-value-form/AttributeForm";
import { KeycloakTextArea } from "../keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../view-header/ViewHeader";

export type RoleFormProps = {
  form: UseFormMethods<AttributeForm>;
  onSubmit: SubmitHandler<AttributeForm>;
  cancelLink: To;
  role: "manage-realm" | "manage-clients";
  editMode: boolean;
};

export const RoleForm = ({
  form: { register, control, handleSubmit, errors },
  onSubmit,
  cancelLink,
  role,
  editMode,
}: RoleFormProps) => {
  const { t } = useTranslation("roles");

  const roleName = useWatch<string | undefined>({
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
            helperTextInvalid={t("common:required")}
            isRequired={!editMode}
          >
            <KeycloakTextInput
              id="kc-name"
              name="name"
              ref={register({
                required: !editMode,
                validate: (value: string) =>
                  !!value.trim() || t("common:required").toString(),
              })}
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
            <KeycloakTextArea
              id="kc-description"
              name="description"
              ref={register({
                maxLength: {
                  value: 255,
                  message: t("common:maxLength", { length: 255 }),
                },
              })}
              validated={
                errors.description
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isDisabled={roleName?.includes("default-roles")}
            />
          </FormGroup>
          <ActionGroup>
            <Button data-testid="save" type="submit" variant="primary">
              {t("common:save")}
            </Button>
            <Button
              data-testid="cancel"
              variant="link"
              component={(props) => <Link {...props} to={cancelLink} />}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
