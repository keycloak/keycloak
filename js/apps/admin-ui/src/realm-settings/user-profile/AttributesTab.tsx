import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { KeycloakSelect, SelectVariant } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Divider,
  SelectOption,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import { uniqBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { DraggableTable } from "../../authentication/components/DraggableTable";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import useLocale from "../../utils/useLocale";
import useToggle from "../../utils/useToggle";
import { toAddAttribute } from "../routes/AddAttribute";
import { toAttribute } from "../routes/Attribute";
import { useUserProfile } from "./UserProfileContext";

const RESTRICTED_ATTRIBUTES = ["username", "email"];

type AttributesTabProps = {
  setTableData: React.Dispatch<
    React.SetStateAction<Record<string, string>[] | undefined>
  >;
};

export const AttributesTab = ({ setTableData }: AttributesTabProps) => {
  const { adminClient } = useAdminClient();
  const { config, save, isSaving } = useUserProfile();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const combinedLocales = useLocale();
  const navigate = useNavigate();
  const [filter, setFilter] = useState("allGroups");
  const [isFilterTypeDropdownOpen, toggleIsFilterTypeDropdownOpen] =
    useToggle();
  const [attributeToDelete, setAttributeToDelete] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const isUpdating = isSaving || isDeleting;

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteAttributeConfirmTitle"),
    messageKey: t("deleteAttributeConfirm", {
      attributeName: attributeToDelete,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    confirmButtonDisabled: isUpdating,
    onConfirm: async () => {
      if (isUpdating || !config?.attributes) return;

      const translationsToDelete = config.attributes.find(
        (attribute) => attribute.name === attributeToDelete,
      )?.displayName;

      // Only a `${...}` display name refers to a translation key. Without one
      // there is nothing to delete, and passing an empty key would target the
      // endpoint that removes every message of the locale.
      const formattedTranslationsToDelete =
        translationsToDelete?.startsWith("${") &&
        translationsToDelete.endsWith("}")
          ? translationsToDelete.slice(2, -1)
          : undefined;

      setIsDeleting(true);
      try {
        if (formattedTranslationsToDelete) {
          await Promise.all(
            combinedLocales.map(async (locale) => {
              try {
                const response =
                  (await adminClient.realms.getRealmLocalizationTexts({
                    realm,
                    selectedLocale: locale,
                  })) as Record<string, string> | undefined;

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
              } catch {
                console.error(`Error removing translations for ${locale}`);
              }
            }),
          );
        }

        const updatedAttributes = config.attributes.filter(
          (attribute) => attribute.name !== attributeToDelete,
        );

        await save(
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
      } finally {
        setIsDeleting(false);
      }
    },
  });

  if (!config) {
    return <KeycloakSpinner />;
  }

  const attributes = config.attributes ?? [];
  const matchesFilter = (attribute: UserProfileAttribute) =>
    filter === "allGroups" || attribute.group === filter;
  const visibleAttributes = attributes.filter(matchesFilter);

  const executeMove = async (nameDragged: string, newOrder: string[]) => {
    const fromIndex = visibleAttributes.findIndex(
      (attribute) => attribute.name === nameDragged,
    );
    const toIndex = newOrder.indexOf(nameDragged);
    if (
      isUpdating ||
      newOrder.length !== visibleAttributes.length ||
      fromIndex < 0 ||
      toIndex < 0 ||
      fromIndex === toIndex
    ) {
      return;
    }

    const reordered = [...visibleAttributes];
    const [movedAttribute] = reordered.splice(fromIndex, 1);
    reordered.splice(toIndex, 0, movedAttribute);

    // Refill the slots in the full list that belong to the visible attributes.
    let visibleIndex = 0;
    const updatedAttributes = attributes.map((attribute) =>
      matchesFilter(attribute) ? reordered[visibleIndex++] : attribute,
    );

    await save(
      { ...config, attributes: updatedAttributes, groups: config.groups ?? [] },
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
            <KeycloakSelect
              toggleId="kc-group-filter"
              width={200}
              data-testid="filter-select"
              isOpen={isFilterTypeDropdownOpen}
              variant={SelectVariant.single}
              onToggle={toggleIsFilterTypeDropdownOpen}
              toggleIcon={<FilterIcon />}
              onSelect={(value) => {
                setFilter(value.toString());
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
                  <SelectOption key={attr.group} value={attr.group}>
                    {attr.group}
                  </SelectOption>
                )),
              ]}
            </KeycloakSelect>
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
        onDragFinish={executeMove}
        restoreOrderOnDrop
        actions={[
          {
            title: t("edit"),
            onClick: (_key, _idx, component) => {
              void navigate(
                toAttribute({
                  realm,
                  attributeName: component.name,
                }),
              );
            },
          },
          {
            title: t("delete"),
            isActionable: ({ name }) =>
              !isUpdating && !RESTRICTED_ATTRIBUTES.includes(name!),
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
        data={visibleAttributes}
      />
    </>
  );
};
