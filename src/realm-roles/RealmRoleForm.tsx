import React, { useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
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
import { SubmitHandler, useForm, UseFormMethods } from "react-hook-form";

import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";

import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";

import { useAdminClient } from "../context/auth/AdminClient";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { RoleAttributes } from "./RoleAttributes";

type RoleFormType = {
  form?: UseFormMethods;
  save?: SubmitHandler<RoleRepresentation>;
  editMode?: boolean;
};

export const RoleForm = ({ form, save, editMode }: RoleFormType) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  return (
    <FormAccess
      isHorizontal
      onSubmit={form!.handleSubmit(save!)}
      role="manage-realm"
      className="pf-u-mt-lg"
    >
      <FormGroup
        label={t("roleName")}
        fieldId="kc-name"
        isRequired
        validated={form!.errors.name ? "error" : "default"}
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          ref={form!.register({ required: true })}
          type="text"
          id="kc-name"
          name="name"
          isReadOnly={editMode}
        />
      </FormGroup>
      <FormGroup
        label={t("description")}
        fieldId="kc-description"
        validated={
          form!.errors.description
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={form!.errors.description?.message}
      >
        <TextArea
          name="description"
          ref={form!.register({
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
          type="text"
          validated={
            form!.errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          id="kc-role-description"
        />
      </FormGroup>
      <ActionGroup>
        <Button variant="primary" type="submit">
          {t("common:save")}
        </Button>
        <Button variant="link" onClick={() => history.push("/roles/")}>
          {editMode ? t("common:reload") : t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};

export const RealmRolesForm = () => {
  const { t } = useTranslation("roles");
  const form = useForm<RoleRepresentation>();
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const history = useHistory();

  const { id } = useParams<{ id: string }>();
  const [name, setName] = useState("");
  const [activeTab, setActiveTab] = useState(0);

  useEffect(() => {
    (async () => {
      if (id) {
        const fetchedRole = await adminClient.roles.findOneById({ id });
        setName(fetchedRole.name!);
        setupForm(fetchedRole);
      } else {
        setName(t("createRole"));
      }
    })();
  }, []);

  const setupForm = (role: RoleRepresentation) => {
    Object.entries(role).map((entry) => {
      form.setValue(entry[0], entry[1]);
    });
  };

  const save = async (role: RoleRepresentation) => {
    try {
      if (id) {
        await adminClient.roles.updateById({ id }, role);
      } else {
        await adminClient.roles.create(role);
        const createdRole = await adminClient.roles.findOneByName({
          name: role.name!,
        });
        history.push(`/roles/${createdRole.id}`);
      }
      addAlert(t(id ? "roleSaveSuccess" : "roleCreated"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t((id ? "roleSave" : "roleCreate") + "Error", { error }),
        AlertVariant.danger
      );
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleDeleteConfirm",
    messageKey: t("roles:roleDeleteConfirmDialog", { name }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.roles.delById({ id });
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
        history.push("/roles");
      } catch (error) {
        addAlert(`${t("roleDeleteError")} ${error}`, AlertVariant.danger);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={name}
        subKey={id ? "" : "roles:roleCreateExplain"}
        dropdownItems={
          id
            ? [
                <DropdownItem
                  key="action"
                  component="button"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("deleteRole")}
                </DropdownItem>,
              ]
            : undefined
        }
      />

      <PageSection variant="light">
        {id && (
          <Tabs
            activeKey={activeTab}
            onSelect={(_, key) => setActiveTab(key as number)}
            isBox
          >
            <Tab
              eventKey={0}
              title={<TabTitleText>{t("details")}</TabTitleText>}
            >
              <RoleForm form={form} save={save} editMode={true} />
            </Tab>
            <Tab
              eventKey={1}
              title={<TabTitleText>{t("attributes")}</TabTitleText>}
            >
              <RoleAttributes />
            </Tab>
          </Tabs>
        )}
        {!id && <RoleForm form={form} save={save} editMode={false} />}
      </PageSection>
    </>
  );
};
