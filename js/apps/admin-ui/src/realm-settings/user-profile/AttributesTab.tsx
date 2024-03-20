import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  Button,
  ButtonVariant,
  Divider,
  Select,
  SelectOption,
  SelectVariant,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import { uniqBy } from "lodash-es";
import { useMemo, useState } from "react";
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
import { useFetch } from "../../utils/useFetch";
import { adminClient } from "../../admin-client";
import { DEFAULT_LOCALE } from "../../i18n/i18n";

const RESTRICTED_ATTRIBUTES = ["username", "email"];

type movedAttributeType = UserProfileAttribute;

type AttributesTabProps = {
  setTableData: React.Dispatch<
    React.SetStateAction<Record<string, string>[] | undefined>
  >;
};

export const AttributesTab = ({ setTableData }: AttributesTabProps) => {
  const { config, save } = useUserProfile();
  const { realm: realmName } = useRealm();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [filter, setFilter] = useState("allGroups");
  const [isFilterTypeDropdownOpen, toggleIsFilterTypeDropdownOpen] =
    useToggle();
  const [data, setData] = useState(config?.attributes);
  const [attributeToDelete, setAttributeToDelete] = useState("");
  const [realm, setRealm] = useState<RealmRepresentation>();

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      if (!realm) {
        throw new Error(t("notFound"));
      }
      setRealm(realm);
    },
    [],
  );

  const defaultSupportedLocales = useMemo(() => {
    return realm?.supportedLocales?.length
      ? realm.supportedLocales
      : [DEFAULT_LOCALE];
  }, [realm]);

  const defaultLocales = useMemo(() => {
    return realm?.defaultLocale?.length ? [realm.defaultLocale] : [];
  }, [realm]);

  const combinedLocales = useMemo(() => {
    return Array.from(new Set([...defaultLocales, ...defaultSupportedLocales]));
  }, [defaultLocales, defaultSupportedLocales]);

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
        (attribute: UserProfileAttribute) =>
          attribute.name === attributeToDelete,
      )?.displayName;

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
                  realm: realmName,
                  selectedLocale: locale,
                });

              if (response) {
                await adminClient.realms.deleteRealmLocalizationTexts({
                  realm: realmName,
                  selectedLocale: locale,
                  key: formattedTranslationsToDelete,
                });

                const updatedData =
                  await adminClient.realms.getRealmLocalizationTexts({
                    realm: realmName,
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
          (attribute: UserProfileAttribute) =>
            attribute.name !== attributeToDelete,
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
        realm: realmName,
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
                <Link {...props} to={toAddAttribute({ realm: realmName })} />
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
                  realm: realmName,
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
