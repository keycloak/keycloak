import React, { Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  Divider,
  Dropdown,
  DropdownItem,
  KebabToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { DraggableTable } from "../../authentication/components/DraggableTable";
import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { useHistory } from "react-router-dom";
import { toAddAttribute } from "../routes/AddAttribute";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useUserProfile } from "./UserProfileContext";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";

type movedAttributeType = UserProfileAttribute;

export const AttributesTab = () => {
  const { config, save } = useUserProfile();
  const { realm: realmName } = useRealm();
  const { t } = useTranslation("realm-settings");
  const history = useHistory();
  const [attributeToDelete, setAttributeToDelete] =
    useState<{ name: string }>();
  const [kebabOpen, setKebabOpen] = useState({
    status: false,
    rowKey: "",
  });

  const executeMove = async (
    attribute: UserProfileAttribute,
    newIndex: number
  ) => {
    const fromIndex = config?.attributes!.findIndex((attr) => {
      return attr.name === attribute.name;
    });

    let movedAttribute: movedAttributeType = {};
    movedAttribute = config?.attributes![fromIndex!]!;
    config?.attributes!.splice(fromIndex!, 1);
    config?.attributes!.splice(newIndex, 0, movedAttribute);

    save(
      { attributes: config?.attributes! },
      {
        successMessageKey: "realm-settings:updatedUserProfileSuccess",
        errorMessageKey: "realm-settings:updatedUserProfileError",
      }
    );
  };

  const goToCreate = () => history.push(toAddAttribute({ realm: realmName }));

  const updatedAttributes = config?.attributes!.filter(
    (attribute) => attribute.name !== attributeToDelete?.name
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteAttributeConfirmTitle"),
    messageKey: t("deleteAttributeConfirm", {
      attributeName: attributeToDelete?.name!,
    }),
    continueButtonLabel: t("common:delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      save(
        { attributes: updatedAttributes! },
        {
          successMessageKey: "realm-settings:deleteAttributeSuccess",
          errorMessageKey: "realm-settings:deleteAttributeError",
        }
      );
      setAttributeToDelete({
        name: "",
      });
    },
  });

  if (!config) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <div className="pf-u-mt-md pf-u-mb-md pf-u-ml-md">
        <ToolbarItem className="kc-toolbar-attributesTab">
          <Button
            data-testid="createAttributeBtn"
            variant="primary"
            onClick={goToCreate}
          >
            {t("createAttribute")}
          </Button>
        </ToolbarItem>
      </div>
      <Divider />
      <DeleteConfirm />
      <DraggableTable
        keyField="name"
        onDragFinish={async (nameDragged, items) => {
          const keys = config.attributes!.map((e) => e.name);
          const newIndex = items.indexOf(nameDragged);
          const oldIndex = keys.indexOf(nameDragged);
          const dragged = config.attributes![oldIndex];
          if (!dragged.name) return;

          executeMove(dragged, newIndex);
        }}
        columns={[
          {
            name: "name",
            displayKey: t("attributeName"),
          },
          {
            name: "displayName",
            displayKey: t("attributeDisplayName"),
          },
          {
            name: "group",
            displayKey: t("attributeGroup"),
          },
          {
            name: "",
            displayKey: "",
            cellRenderer: (row) => (
              <Dropdown
                id={`${row.name}`}
                label={t("attributesDropdown")}
                data-testid="actions-dropdown"
                toggle={
                  <KebabToggle
                    onToggle={(status) =>
                      setKebabOpen({
                        status,
                        rowKey: row.name!,
                      })
                    }
                    id={`toggle-${row.name}`}
                  />
                }
                isOpen={kebabOpen.status && kebabOpen.rowKey === row.name}
                isPlain
                dropdownItems={[
                  <DropdownItem
                    key={`edit-dropdown-item-${row.name}`}
                    data-testid="editDropdownAttributeItem"
                    onClick={() => {
                      setKebabOpen({
                        status: false,
                        rowKey: row.name!,
                      });
                    }}
                  >
                    {t("common:edit")}
                  </DropdownItem>,
                  <Fragment key={`delete-dropdown-${row.name}`}>
                    {row.name !== "email" && row.name !== "username"
                      ? [
                          <DropdownItem
                            key={`delete-dropdown-item-${row.name}`}
                            data-testid="deleteDropdownAttributeItem"
                            onClick={() => {
                              toggleDeleteDialog();
                              setAttributeToDelete({
                                name: row.name!,
                              });
                              setKebabOpen({
                                status: false,
                                rowKey: row.name!,
                              });
                            }}
                          >
                            {t("common:delete")}
                          </DropdownItem>,
                        ]
                      : []}
                  </Fragment>,
                ]}
              />
            ),
          },
        ]}
        data={config.attributes!}
      />
    </>
  );
};
