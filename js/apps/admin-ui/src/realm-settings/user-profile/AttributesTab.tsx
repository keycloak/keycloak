import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  Button,
  ButtonVariant,
  Divider,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { FilterIcon } from "@patternfly/react-icons";
import { uniqBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { DraggableTable } from "../../authentication/components/DraggableTable";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";
import { toAddAttribute } from "../routes/AddAttribute";
import { toAttribute } from "../routes/Attribute";
import { useUserProfile } from "./UserProfileContext";
import useLocale from "../../utils/useLocale";
import { useAdminClient } from "../../admin-client";

const RESTRICTED_ATTRIBUTES = ["username", "email"];

type movedAttributeType = UserProfileAttribute;

type AttributesTabProps = {
  setTableData: React.Dispatch<
    React.SetStateAction<Record<string, string>[] | undefined>
  >;
};

export const AttributesTab = ({ setTableData }: AttributesTabProps) => {
  const { adminClient } = useAdminClient();
  const { config, save } = useUserProfile();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const combinedLocales = useLocale();
  const navigate = useNavigate();
  const [filter, setFilter] = useState("allGroups");
  const [isFilterTypeDropdownOpen, toggleIsFilterTypeDropdownOpen] =
    useToggle();
  const [data, setData] = useState(config?.attributes);
  const [attributeToDelete, setAttributeToDelete] = useState("");

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteAttributeConfirmTitle"),
    messageKey: t("deleteAttributeConfirm", {
      attributeName: attributeToDelete,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      if (!config?.attributes) return;

      const translationsToDelete = config.attributes.find(
        (attribute) => attribute.name === attributeToDelete,
      )?.displayName;

      // Remove the the `${}` from translationsToDelete string
      const formattedTranslationsToDelete = translationsToDelete?.substring(
        2,
        translationsToDelete.length - 1,
      );

      try {
        await Promise.all(
          combinedLocales.map(async (locale) => {
            try {
              const response =
                await adminClient.realms.getRealmLocalizationTexts({
                  realm,
                  selectedLocale: locale,
                });

              if (response) {
                await adminClient.realms.deleteRealmLocalizationTexts({
                  realm,
                  selectedLocale: locale,
                  key: formattedTranslationsToDelete,
                });

                const updatedData =
                  await adminClient.realms.getRealmLocalizationTexts({
                    realm,
                    selectedLocale: locale,
                  });
                setTableData([updatedData]);
              }
            } catch (error) {
              console.error(`Error removing translations for ${locale}`);
            }
          }),
        );

        const updatedAttributes = config.attributes.filter(
          (attribute) => attribute.name !== attributeToDelete,
        );

        save(
          { ...config, attributes: updatedAttributes, groups: config.groups },
          {
            successMessageKey: "deleteAttributeSuccess",
            errorMessageKey: "deleteAttributeError",
          },
        );

        setAttributeToDelete("");
      } catch (error) {
        console.error(
          `Error removing translations or updating attributes: ${error}`,
        );
      }
    },
  });

  if (!config) {
    return <KeycloakSpinner />;
  }

  const attributes = config.attributes ?? [];
  const groups = config.groups ?? [];

  const executeMove = async (
    attribute: UserProfileAttribute,
    newIndex: number,
  ) => {
    const fromIndex = attributes.findIndex((attr) => {
      return attr.name === attribute.name;
    });

    let movedAttribute: movedAttributeType = {};
    movedAttribute = attributes[fromIndex];
    attributes.splice(fromIndex, 1);
    attributes.splice(newIndex, 0, movedAttribute);

    save(
      { attributes, groups },
      {
        successMessageKey: "updatedUserProfileSuccess",
        errorMessageKey: "updatedUserProfileError",
      },
    );
  };

  const cellFormatter = (row: UserProfileAttribute) => (
    <Link
      to={toAttribute({
        realm,
        attributeName: row.name!,
      })}
      key={row.name}
    >
      {row.name}
    </Link>
  );

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <Select
              width={200}
              data-testid="filter-select"
              isOpen={isFilterTypeDropdownOpen}
              variant={SelectVariant.single}
              onToggle={toggleIsFilterTypeDropdownOpen}
              toggleIcon={<FilterIcon />}
              onSelect={(_, value) => {
                const filter = value.toString();
                setFilter(filter);
                setData(
                  filter === "allGroups"
                    ? attributes
                    : attributes.filter((attr) => attr.group === filter),
                );
                toggleIsFilterTypeDropdownOpen();
              }}
              selections={filter === "allGroups" ? t(filter) : filter}
            >
              {[
                <SelectOption
                  key="allGroups"
                  data-testid="all-groups"
                  value="allGroups"
                >
                  {t("allGroups")}
                </SelectOption>,
                ...uniqBy(
                  attributes.filter((attr) => !!attr.group),
                  "group",
                ).map((attr) => (
                  <SelectOption
                    key={attr.group}
                    data-testid={`${attr.group}-option`}
                    value={attr.group}
                  />
                )),
              ]}
            </Select>
          </ToolbarItem>
          <ToolbarItem className="kc-toolbar-attributesTab">
            <Button
              data-testid="createAttributeBtn"
              variant="primary"
              component={(props) => (
                <Link {...props} to={toAddAttribute({ realm })} />
              )}
            >
              {t("createAttribute")}
            </Button>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <Divider />
      <DeleteConfirm />
      <DraggableTable
        keyField="name"
        onDragFinish={async (nameDragged, items) => {
          const keys = attributes.map((e) => e.name);
          const newIndex = items.indexOf(nameDragged);
          const oldIndex = keys.indexOf(nameDragged);
          const dragged = attributes[oldIndex];
          if (!dragged.name) return;

          executeMove(dragged, newIndex);
        }}
        actions={[
          {
            title: t("edit"),
            onClick: (_key, _idx, component) => {
              navigate(
                toAttribute({
                  realm,
                  attributeName: component.name,
                }),
              );
            },
          },
          {
            title: t("delete"),
            isActionable: ({ name }) => !RESTRICTED_ATTRIBUTES.includes(name!),
            isDisabled: RESTRICTED_ATTRIBUTES.includes(name!),
            onClick: (_key, _idx, component) => {
              setAttributeToDelete(component.name);
              toggleDeleteDialog();
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: t("attributeName"),
            cellRenderer: cellFormatter,
          },
          {
            name: "displayName",
            displayKey: t("attributeDisplayName"),
          },
          {
            name: "group",
            displayKey: t("attributeGroup"),
          },
        ]}
        data={data ?? attributes}
      />
    </>
  );
};
