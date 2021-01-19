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
import { useForm } from "react-hook-form";

import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { KeyValueType, RoleAttributes } from "./RoleAttributes";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { RealmRoleForm } from "./RealmRoleForm";
import { useRealm } from "../context/realm-context/RealmContext";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";

const arrayToAttributes = (attributeArray: KeyValueType[]) => {
  const initValue: { [index: string]: string[] } = {};
  return attributeArray.reduce((acc, attribute) => {
    acc[attribute.key] = [attribute.value];
    return acc;
  }, initValue);
};

const attributesToArray = (attributes: { [key: string]: string }): any => {
  if (!attributes || Object.keys(attributes).length == 0) {
    return [
      {
        key: "",
        value: "",
      },
    ];
  }
  return Object.keys(attributes).map((key) => ({
    key: key,
    value: attributes[key],
  }));
};

export const RealmRoleTabs = () => {
  const { t } = useTranslation("roles");
  const form = useForm<RoleRepresentation>({ mode: "onChange" });
  const history = useHistory();
  const [name, setName] = useState("");
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const [role, setRole] = useState<RoleRepresentation>();

  const { id } = useParams<{ id: string }>();

  const { addAlert } = useAlerts();

  useEffect(() => {
    (async () => {
      if (id) {
        const fetchedRole = await adminClient.roles.findOneById({ id });
        setName(fetchedRole.name!);
        setupForm(fetchedRole);
        setRole(fetchedRole);
      } else {
        setName(t("createRole"));
      }
    })();
  }, []);

  const setupForm = (role: RoleRepresentation) => {
    Object.entries(role).map((entry) => {
      if (entry[0] === "attributes") {
        form.setValue(entry[0], attributesToArray(entry[1]));
      } else {
        form.setValue(entry[0], entry[1]);
      }
    });
  };

  // reset form to default values
  const reset = () => {
    setupForm(role!);
  };

  const save = async (role: RoleRepresentation) => {
    try {
      if (id) {
        if (role.attributes) {
          // react-hook-form will use `KeyValueType[]` here we convert it back into an indexed property of string[]
          role.attributes = arrayToAttributes(
            (role.attributes as unknown) as KeyValueType[]
          );
        }
        setRole(role!);
        setupForm(role!);
        await adminClient.roles.updateById({ id }, role);
      } else {
        await adminClient.roles.create(role);
        const createdRole = await adminClient.roles.findOneByName({
          name: role.name!,
        });
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
    messageKey: t("roles:roleDeleteConfirmDialog", { name }),
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
          <KeycloakTabs isBox>
            <Tab
              eventKey="details"
              title={<TabTitleText>{t("details")}</TabTitleText>}
            >
              <RealmRoleForm
                reset={reset}
                form={form}
                save={save}
                editMode={true}
              />
            </Tab>
            <Tab
              eventKey="attributes"
              title={<TabTitleText>{t("attributes")}</TabTitleText>}
            >
              <RoleAttributes form={form} save={save} reset={reset} />
            </Tab>
          </KeycloakTabs>
        )}
        {!id && (
          <RealmRoleForm
            reset={reset}
            form={form}
            save={save}
            editMode={false}
          />
        )}
      </PageSection>
    </>
  );
};
