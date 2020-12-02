import React, { useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
  TextArea,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import { FormAccess } from "../components/form-access/FormAccess";

import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";

import { useAdminClient } from "../context/auth/AdminClient";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { RoleAttributes } from "./RoleAttributes";

export const RolesForm = () => {
  const { t } = useTranslation("roles");
  const { register, handleSubmit, errors, control, setValue } = useForm<
    RoleRepresentation
  >();
  const history = useHistory();
  const [name, setName] = useState("");
  const [activeTab, setActiveTab] = useState(0);

  const adminClient = useAdminClient();
  const form = useForm();


  const { id } = useParams<{ id: string }>();

  const { addAlert } = useAlerts();

  useEffect(() => {
    (async () => {
      const fetchedRole = await adminClient.roles.findOneById({ id });
      setName(fetchedRole.name!);
      setupForm(fetchedRole);
    })();
  }, []);

  const setupForm = (role: RoleRepresentation) => {
    Object.entries(role).map((entry) => {
      setValue(entry[0], entry[1]);
    });
  };

  const save = async (role: RoleRepresentation) => {
    try {
      await adminClient.roles.updateById({ id }, role);
      setupForm(role as RoleRepresentation);
      addAlert(t("roleSaveSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(`${t("roleSaveError")} '${error}'`, AlertVariant.danger);
    }
  };

  return (
    <>
      <ViewHeader titleKey={name} subKey="" />

      <PageSection variant="light">
        <Tabs
          activeKey={activeTab}
          onSelect={(_, key) => setActiveTab(key as number)}
          isBox
        >
          <Tab eventKey={0} title={<TabTitleText>{t("details")}</TabTitleText>}>
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
                {name ? (
                  <TextInput
                    ref={register({ required: true })}
                    type="text"
                    id="kc-name"
                    name="name"
                    isReadOnly
                  />
                ) : undefined}
              </FormGroup>
              <FormGroup label={t("description")} fieldId="kc-description">
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
                  {t("common:save")}
                </Button>
                <Button variant="link" onClick={() => history.push("/roles/")}>
                  {t("common:reload")}
                </Button>
              </ActionGroup>
            </FormAccess>
          </Tab>
          <Tab eventKey={1} title={<TabTitleText>{t("attributes")}</TabTitleText>}>
            <RoleAttributes form={form} />
          </Tab>
        </Tabs>
      </PageSection>
    </>
  );
};
