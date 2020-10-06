import React, { useContext } from "react";
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

import { RoleRepresentation } from "../../model/role-model";
import { HttpClientContext } from "../../context/http-service/HttpClientContext";
import { useAlerts } from "../../components/alert/Alerts";
import { Controller, useForm } from "react-hook-form";
import { RealmContext } from "../../context/realm-context/RealmContext";

export const NewRoleForm = () => {
  const { t } = useTranslation("roles");
  const httpClient = useContext(HttpClientContext)!;
  const { addAlert } = useAlerts();
  const { realm } = useContext(RealmContext);

  const { register, control, errors, handleSubmit } = useForm<
    RoleRepresentation
  >();

  const save = async (role: RoleRepresentation) => {
    try {
      await httpClient.doPost(`admin/realms/${realm}/roles`, role);
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
          <FormGroup label={t("roleName")} isRequired fieldId="kc-role-name">
            <TextInput
              isRequired
              type="text"
              id="kc-role-name"
              name="name"
              ref={register()}
            />
          </FormGroup>
          <FormGroup
            label={t("description")}
            fieldId="kc-role-description"
            helperTextInvalid="Max length 255"
            validated={
              errors ? ValidatedOptions.error : ValidatedOptions.default
            }
          >
            <Controller
              name="description"
              defaultValue=""
              control={control}
              rules={{ maxLength: 255 }}
              render={({ onChange, value }) => (
                <TextArea
                  type="text"
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
