import React from "react";
import { useTranslation } from "react-i18next";
import {
  Text,
  PageSection,
  TextContent,
  FormGroup,
  Form,
  TextInput,
  ActionGroup,
  Button,
  Divider,
  AlertVariant,
  TextArea,
  ValidatedOptions,
} from "@patternfly/react-core";

import { useAlerts } from "../../components/alert/Alerts";
import { Controller, useForm } from "react-hook-form";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";

export const NewRoleForm = () => {
  const { t } = useTranslation("roles");
  const { addAlert } = useAlerts();
  const adminClient = useAdminClient();

  const { register, control, errors, handleSubmit } = useForm<
    RoleRepresentation
  >();

  const save = async (role: RoleRepresentation) => {
    try {
      await adminClient.roles.create(role);
      addAlert(t("roleCreated"), AlertVariant.success);
    } catch (error) {
      addAlert(`${t("roleCreateError")} '${error}'`, AlertVariant.danger);
    }
  };

  return (
    <>
      <PageSection variant="light">
        <TextContent>
          <Text component="h1">{t("createRole")}</Text>
        </TextContent>
      </PageSection>
      <Divider />
      <PageSection variant="light">
        <Form isHorizontal onSubmit={handleSubmit(save)}>
          <FormGroup
            label={t("roleName")}
            isRequired
            fieldId="kc-role-name"
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              isRequired
              type="text"
              id="kc-role-name"
              name="name"
              ref={register({ required: true })}
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
            />
          </FormGroup>
          <FormGroup
            label={t("description")}
            fieldId="kc-role-description"
            validated={
              errors.description
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:maxLength", { length: 255 })}
          >
            <Controller
              name="description"
              defaultValue=""
              control={control}
              rules={{ maxLength: 255 }}
              render={({ onChange, value }) => (
                <TextArea
                  type="text"
                  validated={
                    errors.description
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                  id="kc-role-description"
                  value={value}
                  onChange={onChange}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:create")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
