import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  KeycloakSelect,
  ListEmptyState,
  PaginatingTableToolbar,
  SelectVariant,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  Dropdown,
  DropdownItem,
  DropdownList,
  Form,
  FormGroup,
  MenuToggle,
  SelectGroup,
  SelectOption,
  Content,
  TextInput,
  ContentVariants,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  CheckIcon,
  EllipsisVIcon,
  PencilAltIcon,
  SearchIcon,
  TimesIcon,
} from "@patternfly/react-icons";
import {
  ActionsColumn,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { DEFAULT_LOCALE } from "../../i18n/constants";
import { localeToDisplayName } from "../../util";
import { AddTranslationModal } from "../AddTranslationModal";

type RealmOverridesProps = {
  internationalizationEnabled: boolean;
  watchSupportedLocales: string[];
  realm: RealmRepresentation;
  tableData: Record<string, string>[] | undefined;
};

type TranslationDraft = {
  value: string;
  isSaving: boolean;
};

type LocaleTranslations = {
  realm: string;
  locale: string;
  entries: [string, string][];
};

function translationId(realm: string, locale: string, key: string) {
  return JSON.stringify([realm, locale, key]);
}

export type TranslationForm = {
  key: string;
  value: string;
  translation: KeyValueType;
};

export enum RowEditAction {
  Save = "save",
  Cancel = "cancel",
  Edit = "edit",
  Delete = "delete",
}

export const RealmOverrides = ({
  internationalizationEnabled,
  watchSupportedLocales,
  realm,
  tableData,
}: RealmOverridesProps) => {
  const { adminClient } = useAdminClient();

  const { t, i18n } = useTranslation();
  const [addTranslationModalOpen, setAddTranslationModalOpen] = useState(false);
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [selectMenuLocale, setSelectMenuLocale] = useState(DEFAULT_LOCALE);
  const [kebabOpen, setKebabOpen] = useState(false);
  const [selectMenuValueSelected, setSelectMenuValueSelected] = useState(false);
  const [localeTranslations, setLocaleTranslations] =
    useState<LocaleTranslations>();
  const [tableKey, setTableKey] = useState(0);
  const tableRevision = useRef(0);
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");
  const translationForm = useForm<TranslationForm>({ mode: "onChange" });
  const { addAlert, addError } = useAlerts();
  const { realm: currentRealm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const [areAllRowsSelected, setAreAllRowsSelected] = useState(false);
  const [drafts, setDrafts] = useState<
    Partial<Record<string, TranslationDraft>>
  >({});
  const [deletingIds, setDeletingIds] = useState(() => new Set<string>());
  const realmName = realm.realm!;
  const isDeleting = deletingIds.size > 0;
  const canDeleteSelection =
    !isDeleting &&
    selectedRowKeys.length > 0 &&
    !selectedRowKeys.some(
      (key) =>
        drafts[translationId(realmName, selectMenuLocale, key)]?.isSaving,
    );

  const refreshTable = () => {
    // Invalidate pending reads before the refresh effect runs.
    setTableKey(++tableRevision.current);
  };

  const updateLocaleTranslations = (
    updateEntries: (
      entries: LocaleTranslations["entries"],
    ) => LocaleTranslations["entries"],
  ) => {
    setLocaleTranslations((previous) =>
      previous?.realm === realmName && previous.locale === selectMenuLocale
        ? { ...previous, entries: updateEntries(previous.entries) }
        : previous,
    );
    refreshTable();
  };

  useFetch(
    () =>
      adminClient.realms
        .getRealmLocalizationTexts({
          realm: realmName,
          selectedLocale: selectMenuLocale,
        })
        .catch(() => undefined),
    (result) => {
      if (result && tableKey === tableRevision.current) {
        setLocaleTranslations({
          realm: realmName,
          locale: selectMenuLocale,
          entries: Object.entries(result),
        });
      }
    },
    [adminClient, realmName, selectMenuLocale, tableKey, tableData, first, max],
  );

  const translations =
    localeTranslations?.realm === realmName &&
    localeTranslations.locale === selectMenuLocale
      ? localeTranslations.entries
      : [];
  const filteredTranslations = filter
    ? [
        ...translations.filter(([key]) => key.includes(filter)),
        ...translations.filter(
          ([key, value]) => !key.includes(filter) && value.includes(filter),
        ),
      ]
    : translations;
  const tableRows = filteredTranslations.slice(first, first + max);

  const handleModalToggle = () => {
    setAddTranslationModalOpen(!addTranslationModalOpen);
  };

  const options = [
    <SelectGroup label={t("defaultLocale")} key="group1">
      <SelectOption key={DEFAULT_LOCALE} value={DEFAULT_LOCALE}>
        {localeToDisplayName(DEFAULT_LOCALE, whoAmI.displayName)}
      </SelectOption>
    </SelectGroup>,
    <Divider key="divider" />,
    <SelectGroup label={t("supportedLocales")} key="group2">
      {watchSupportedLocales.map((locale) => (
        <SelectOption key={locale} value={locale}>
          {localeToDisplayName(locale, whoAmI.locale)}
        </SelectOption>
      ))}
    </SelectGroup>,
  ];

  const addKeyValue = async (pair: KeyValueType): Promise<void> => {
    try {
      await adminClient.realms.addLocalization(
        {
          realm: currentRealm!,
          selectedLocale: selectMenuLocale,
          key: pair.key,
        },
        pair.value,
      );

      adminClient.setConfig({
        realmName: currentRealm!,
      });
      refreshTable();
      translationForm.setValue("key", "");
      translationForm.setValue("value", "");
      await i18n.reloadResources();

      addAlert(t("addTranslationSuccess"), AlertVariant.success);
    } catch (error) {
      addError("addTranslationError", error);
    }
  };

  const clearDraft = (id: string, expectedDraft?: TranslationDraft) => {
    setDrafts((previous) => {
      if (!previous[id] || (expectedDraft && previous[id] !== expectedDraft)) {
        return previous;
      }
      const next = { ...previous };
      delete next[id];
      return next;
    });
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteConfirmTranslationTitle",
    messageKey: t("translationDeleteConfirmDialog", {
      count: selectedRowKeys.length,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    confirmButtonDisabled: !canDeleteSelection,
    onCancel: () => {
      setSelectedRowKeys([]);
      setAreAllRowsSelected(false);
    },
    onConfirm: async () => {
      const deletedKeys = new Set<string>();
      setDeletingIds(
        new Set(
          selectedRowKeys.map((key) =>
            translationId(realmName, selectMenuLocale, key),
          ),
        ),
      );

      try {
        for (const key of selectedRowKeys) {
          await adminClient.realms.deleteRealmLocalizationTexts({
            realm: realmName,
            selectedLocale: selectMenuLocale,
            key,
          });
          deletedKeys.add(key);

          const messages = i18n.getResourceBundle(
            selectMenuLocale,
            encodeURIComponent(realmName),
          ) as Record<string, string> | undefined;
          if (messages) {
            delete messages[key];
          }
        }
        addAlert(t("deleteAllTranslationsSuccess"), AlertVariant.success);
      } catch (error) {
        addError("deleteAllTranslationsError", error);
      } finally {
        if (deletedKeys.size > 0) {
          updateLocaleTranslations((entries) =>
            entries.filter(([key]) => !deletedKeys.has(key)),
          );
          for (const key of deletedKeys) {
            clearDraft(translationId(realmName, selectMenuLocale, key));
          }
          setSelectedRowKeys((previous) =>
            previous === selectedRowKeys
              ? previous.filter((key) => !deletedKeys.has(key))
              : previous,
          );
          setAreAllRowsSelected(false);
        }
        setDeletingIds(new Set());
      }
    },
  });

  const handleRowSelect = (selectedKey: string, isSelected: boolean) => {
    if (isSelected) {
      setSelectedRowKeys((prevSelected) => [...prevSelected, selectedKey]);
    } else {
      setSelectedRowKeys((prevSelected) =>
        prevSelected.filter((key) => key !== selectedKey),
      );
    }

    setAreAllRowsSelected(
      tableRows.length === selectedRowKeys.length + (isSelected ? 1 : -1),
    );
  };

  const toggleSelectAllRows = () => {
    if (areAllRowsSelected) {
      setSelectedRowKeys([]);
    } else {
      setSelectedRowKeys(tableRows.map(([key]) => key));
    }
    setAreAllRowsSelected(!areAllRowsSelected);
  };

  const saveTranslation = async (key: string, draft: TranslationDraft) => {
    const id = translationId(realmName, selectMenuLocale, key);
    const savingDraft = { ...draft, isSaving: true };
    setDrafts((previous) => ({ ...previous, [id]: savingDraft }));

    try {
      await adminClient.realms.addLocalization(
        { realm: realmName, selectedLocale: selectMenuLocale, key },
        draft.value,
      );

      updateLocaleTranslations((entries) =>
        entries.map((entry) => (entry[0] === key ? [key, draft.value] : entry)),
      );
      clearDraft(id, savingDraft);
      addAlert(t("updateTranslationSuccess"), AlertVariant.success);
    } catch (error) {
      setDrafts((previous) =>
        previous[id] === savingDraft ? { ...previous, [id]: draft } : previous,
      );
      addError("updateTranslationError", error);
    }
  };

  return (
    <>
      <DeleteConfirm />
      {addTranslationModalOpen && (
        <AddTranslationModal
          handleModalToggle={handleModalToggle}
          save={async (pair: any) => {
            await addKeyValue(pair);
            handleModalToggle();
          }}
          form={translationForm}
        />
      )}
      <Content>
        <Content
          className="pf-v6-u-mt-lg pf-v6-u-ml-md"
          component={ContentVariants.p}
        >
          {t("realmOverridesDescription")}
        </Content>
      </Content>
      <PaginatingTableToolbar
        count={translations.length}
        first={first}
        max={max}
        onNextClick={setFirst}
        onPreviousClick={setFirst}
        onPerPageSelect={(first, max) => {
          setFirst(first);
          setMax(max);
        }}
        inputGroupName={"search"}
        inputGroupOnEnter={(search) => {
          setFilter(search);
          setFirst(0);
          setMax(10);
          refreshTable();
        }}
        inputGroupPlaceholder={t("searchForTranslation")}
        toolbarItem={
          <>
            <Button
              data-testid="add-translationBtn"
              onClick={() => {
                setAddTranslationModalOpen(true);
                setAreAllRowsSelected(false);
                setSelectedRowKeys([]);
              }}
            >
              {t("addTranslation")}
            </Button>
            <ToolbarItem>
              <Dropdown
                onOpenChange={(isOpen) => setKebabOpen(isOpen)}
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    onClick={() => setKebabOpen(!kebabOpen)}
                    variant="plain"
                    isExpanded={kebabOpen}
                    data-testid="toolbar-deleteBtn"
                    aria-label="kebab"
                  >
                    <EllipsisVIcon />
                  </MenuToggle>
                )}
                isOpen={kebabOpen}
                isPlain
              >
                <DropdownList>
                  <DropdownItem
                    key="action"
                    component="button"
                    data-testid="delete-selected-TranslationBtn"
                    isDisabled={
                      translations.length === 0 || !canDeleteSelection
                    }
                    onClick={() => {
                      toggleDeleteDialog();
                      setKebabOpen(false);
                    }}
                  >
                    {t("delete")}
                  </DropdownItem>
                </DropdownList>
              </Dropdown>
            </ToolbarItem>
          </>
        }
        searchTypeComponent={
          <ToolbarItem>
            <KeycloakSelect
              width={180}
              isOpen={filterDropdownOpen}
              className="kc-filter-by-locale-select"
              variant={SelectVariant.single}
              isDisabled={!internationalizationEnabled}
              onToggle={(isExpanded) => setFilterDropdownOpen(isExpanded)}
              onSelect={(value) => {
                const locale = value.toString();
                if (locale !== selectMenuLocale) {
                  setSelectMenuLocale(locale);
                  setSelectedRowKeys([]);
                  setAreAllRowsSelected(false);
                  setFirst(0);
                }
                setSelectMenuValueSelected(true);
                refreshTable();
                setFilterDropdownOpen(false);
              }}
              selections={
                selectMenuValueSelected
                  ? localeToDisplayName(selectMenuLocale, whoAmI.locale)
                  : realm.defaultLocale !== ""
                    ? localeToDisplayName(DEFAULT_LOCALE, whoAmI.locale)
                    : t("placeholderText")
              }
            >
              {options}
            </KeycloakSelect>
          </ToolbarItem>
        }
      >
        {translations.length === 0 && !filter && (
          <ListEmptyState
            hasIcon
            message={t("noTranslations")}
            instructions={t("noTranslationsInstructions")}
            onPrimaryAction={handleModalToggle}
          />
        )}
        {translations.length === 0 && filter && (
          <ListEmptyState
            hasIcon
            icon={SearchIcon}
            isSearchVariant
            message={t("noSearchResults")}
            instructions={t("noRealmOverridesSearchResultsInstructions")}
          />
        )}
        {translations.length !== 0 && (
          <Table
            aria-label={t("editableRowsTable")}
            data-testid="editable-rows-table"
          >
            <Thead>
              <Tr>
                <Th className="pf-v6-u-px-lg">
                  <input
                    type="checkbox"
                    aria-label={t("selectAll")}
                    checked={areAllRowsSelected}
                    disabled={isDeleting}
                    onChange={toggleSelectAllRows}
                    data-testid="selectAll"
                  />
                </Th>
                <Th className="pf-v6-u-py-lg">{t("key")}</Th>
                <Th className="pf-v6-u-py-lg">{t("value")}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {tableRows.map(([key, value], rowIndex) => {
                const id = translationId(realmName, selectMenuLocale, key);
                const draft = drafts[id];
                const isDeletingRow = deletingIds.has(id);

                return (
                  <Tr key={id}>
                    <Td
                      className="pf-v6-u-px-lg"
                      select={{
                        rowIndex,
                        onSelect: (_, isSelected) =>
                          handleRowSelect(key, isSelected),
                        isSelected: selectedRowKeys.includes(key),
                        isDisabled: isDeleting,
                      }}
                    />
                    <Td className="pf-m-sm pf-v6-u-px-sm" dataLabel={t("key")}>
                      {key}
                    </Td>
                    <Td
                      className="pf-m-sm pf-v6-u-px-sm"
                      dataLabel={t("value")}
                    >
                      <Form
                        isHorizontal
                        className="kc-form-translationValue"
                        onSubmit={(event) => {
                          event.preventDefault();
                          if (draft && !draft.isSaving && !isDeletingRow) {
                            void saveTranslation(key, draft);
                          }
                        }}
                      >
                        <FormGroup
                          fieldId="kc-translationValue"
                          className="pf-v6-u-display-inline-block"
                        >
                          {draft ? (
                            <>
                              <TextInput
                                aria-label={t("editTranslationValue")}
                                type="text"
                                className="pf-v6-u-w-initial"
                                data-testid={`editTranslationValueInput-${rowIndex}`}
                                value={draft.value}
                                isDisabled={draft.isSaving || isDeletingRow}
                                onChange={(_, value) => {
                                  setDrafts((previous) => ({
                                    ...previous,
                                    [id]: { ...draft, value },
                                  }));
                                }}
                              />
                              <Button
                                key="save"
                                variant="link"
                                className="pf-m-plain"
                                data-testid={`editTranslationAcceptBtn-${rowIndex}`}
                                type="submit"
                                isDisabled={draft.isSaving || isDeletingRow}
                                aria-label={t("acceptBtn")}
                                icon={<CheckIcon />}
                              />
                              <Button
                                variant="link"
                                className="pf-m-plain"
                                data-testid={`editTranslationCancelBtn-${rowIndex}`}
                                type="button"
                                isDisabled={draft.isSaving || isDeletingRow}
                                icon={<TimesIcon />}
                                aria-label={t("cancelBtn")}
                                onClick={() => clearDraft(id, draft)}
                              />
                            </>
                          ) : (
                            <>
                              <span>{value}</span>
                              <Button
                                key="edit"
                                type="button"
                                isDisabled={isDeletingRow}
                                icon={<PencilAltIcon />}
                                onClick={() => {
                                  setDrafts((previous) => ({
                                    ...previous,
                                    [id]: { value, isSaving: false },
                                  }));
                                }}
                                aria-label={t("editBtn")}
                                variant="link"
                                className="pf-m-plain"
                                data-testid={`editTranslationBtn-${rowIndex}`}
                              />
                            </>
                          )}
                        </FormGroup>
                      </Form>
                    </Td>
                    <Td isActionCell>
                      <ActionsColumn
                        isDisabled={isDeleting || draft?.isSaving}
                        items={[
                          {
                            title: t("delete"),
                            onClick: () => {
                              setSelectedRowKeys([key]);

                              if (translations.length === 1) {
                                setAreAllRowsSelected(true);
                              }

                              toggleDeleteDialog();
                              setKebabOpen(false);
                            },
                          },
                        ]}
                      />
                    </Td>
                  </Tr>
                );
              })}
            </Tbody>
          </Table>
        )}
      </PaginatingTableToolbar>
    </>
  );
};
