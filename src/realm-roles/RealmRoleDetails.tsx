import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
  TextArea,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { ViewHeader } from "../components/view-header/ViewHeader";
import { RoleRepresentation } from "../model/role-model";
import { FormAccess } from "../components/form-access/FormAccess";

export const RolesForm = () => {
  const { t } = useTranslation("client-scopes");
  const { register, errors } = useForm<RoleRepresentation>();
  const history = useHistory();

  const [activeTab, setActiveTab] = useState(0);

  return (
    <>
      <ViewHeader titleKey={"Role Name"} subKey="" />

      <PageSection variant="light">
        <Tabs
          activeKey={activeTab}
          onSelect={(_, key) => setActiveTab(key as number)}
          isBox
        >
          <Tab eventKey={0} title={<TabTitleText>{t("Details")}</TabTitleText>}>
            <FormAccess isHorizontal role="manage-realm" className="pf-u-mt-lg">
              <FormGroup
                label={t("Role name")}
                fieldId="kc-name"
                isRequired
                validated={errors.name ? "error" : "default"}
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register({ required: true })}
                  type="text"
                  id="kc-name"
                  name="name"
                />
              </FormGroup>
              <FormGroup label={t("description")} fieldId="kc-description">
                <TextArea
                  ref={register}
                  type="text"
                  id="kc-description"
                  name="description"
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
        </Tabs>
      </PageSection>
    </>
  );
};
