import React, { useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useFieldArray, useForm } from "react-hook-form";

import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { KeyValueType, RoleAttributes } from "./RoleAttributes";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { RealmRoleForm } from "./RealmRoleForm";
import { useRealm } from "../context/realm-context/RealmContext";
import { AssociatedRolesModal } from "./AssociatedRolesModal";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";

const arrayToAttributes = (attributeArray: KeyValueType[]) => {
  const initValue: { [index: string]: string[] } = {};
  return attributeArray.reduce((acc, attribute) => {
    acc[attribute.key] = [attribute.value];
    return acc;
  }, initValue);
};

const attributesToArray = (attributes?: {
  [key: string]: string[];
}): KeyValueType[] => {
  if (!attributes || Object.keys(attributes).length == 0) {
    return [];
  }
  return Object.keys(attributes).map((key) => ({
    key: key,
    value: attributes[key][0],
  }));
};

export type RoleFormType = Omit<RoleRepresentation, "attributes"> & {
  attributes: KeyValueType[];
};

export const RealmRoleTabs = () => {
  const { t } = useTranslation("roles");
  const form = useForm<RoleFormType>({ mode: "onChange" });
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const [role, setRole] = useState<RoleFormType>();

  const { id } = useParams<{ id: string }>();
  const { addAlert } = useAlerts();

  const [open, setOpen] = useState(false);
  const convert = (role: RoleRepresentation) => {
    const { attributes, ...rest } = role;
    return {
      attributes: attributesToArray(attributes),
      ...rest,
    };
  };

  useEffect(() => {
    (async () => {
      if (id) {
        const fetchedRole = await adminClient.roles.findOneById({ id });
        const convertedRole = convert(fetchedRole);
        Object.entries(convertedRole).map((entry) => {
          form.setValue(entry[0], entry[1]);
        });
        setRole(convertedRole);
      }
    })();
  }, []);

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "attributes",
  });

  useEffect(() => append({ key: "", value: "" }), [append, role]);

  const save = async (role: RoleFormType) => {
    try {
      const { attributes, ...rest } = role;
      const roleRepresentation: RoleRepresentation = rest;
      if (id) {
        if (attributes) {
          roleRepresentation.attributes = arrayToAttributes(attributes);
        }
        await adminClient.roles.updateById({ id }, roleRepresentation);
        setRole(role);
      } else {
        await adminClient.roles.create(roleRepresentation);
        const createdRole = await adminClient.roles.findOneByName({
          name: role.name!,
        });
        setRole(convert(createdRole));
        history.push(`/${realm}/roles/${createdRole.id}`);
      }
      addAlert(t(id ? "roleSaveSuccess" : "roleCreated"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t((id ? "roleSave" : "roleCreate") + "Error", {
          error: error.response.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleDeleteConfirm",
    messageKey: t("roles:roleDeleteConfirmDialog", {
      name: role?.name || t("createRole"),
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.roles.delById({ id });
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
        history.replace(`/${realm}/roles`);
      } catch (error) {
        addAlert(`${t("roleDeleteError")} ${error}`, AlertVariant.danger);
      }
    },
  });

  const toggleModal = () => setOpen(!open);

  return (
    <>
      <DeleteConfirm />
      <AssociatedRolesModal open={open} toggleDialog={() => setOpen(!open)} />
      <ViewHeader
        titleKey={role?.name || t("createRole")}
        subKey={id ? "" : "roles:roleCreateExplain"}
        dropdownItems={
          id
            ? [
                <DropdownItem
                  key="delete-role"
                  component="button"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("deleteRole")}
                </DropdownItem>,
                <DropdownItem
                  key="toggle-modal"
                  component="button"
                  onClick={() => toggleModal()}
                >
                  {t("addAssociatedRolesText")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        {id && (
          <KeycloakTabs isBox>
            <Tab
              eventKey="details"
              title={<TabTitleText>{t("details")}</TabTitleText>}
            >
              <RealmRoleForm form={form} save={save} editMode={true} />
            </Tab>
            <Tab
              eventKey="attributes"
              title={<TabTitleText>{t("attributes")}</TabTitleText>}
            >
              <RoleAttributes
                form={form}
                save={save}
                array={{ fields, append, remove }}
              />
            </Tab>
          </KeycloakTabs>
        )}
        {!id && <RealmRoleForm form={form} save={save} editMode={false} />}
      </PageSection>
    </>
  );
};