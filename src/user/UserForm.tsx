import React from "react";
import {
  ActionGroup,
  Button,
  FormGroup,
  TextArea,
  TextInput,
//   ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { UseFormMethods } from "react-hook-form";

import { FormAccess } from "../components/form-access/FormAccess";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
// import { RoleFormType } from "./RealmRoleTabs";

export type UserFormProps = {
  form: UseFormMethods<UserRepresentation>;
  save: (user: UserRepresentation) => void;
  editMode: boolean;
  reset: () => void;
};

export const UserForm = ({
  form,
  save,
  editMode,
  reset,
}: UserFormProps) => {
  const { t } = useTranslation("users");
  return (
    <FormAccess
      isHorizontal
      onSubmit={form.handleSubmit(save)}
      role="manage-realm"
      className="pf-u-mt-lg"
    >
      <FormGroup
        label={t("roleName")}
        fieldId="kc-name"
        isRequired
        // validated={form.errors.name ? "error" : "default"}
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          ref={form.register({ required: !editMode })}
          type="text"
          id="kc-name"
          name="name"
          isReadOnly={editMode}
        />
      </FormGroup>
      <FormGroup
        label={t("common:description")}
        fieldId="kc-description"
        // validated={
        //   form.errors.description
        //     ? ValidatedOptions.error
        //     : ValidatedOptions.default
        // }
        // helperTextInvalid={form.errors.description?.message}
      >
        <TextArea
          name="description"
          ref={form.register({
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
          type="text"
        //   validated={
        //     form.errors.description
        //       ? ValidatedOptions.error
        //       : ValidatedOptions.default
        //   }
          id="kc-role-description"
        />
      </FormGroup>
      <ActionGroup>
        <Button variant="primary" type="submit">
          {t("common:save")}
        </Button>
        <Button onClick={() => reset()} variant="link">
          {editMode ? t("common:reload") : t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
