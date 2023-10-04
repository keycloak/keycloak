import { useState } from "react";
import {
  AlertVariant,
  Button,
  Divider,
  Dropdown,
  DropdownItem,
  KebabToggle,
  Select,
  SelectGroup,
  SelectOption,
  SelectVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { useFetch } from "../../utils/useFetch";
import { adminClient } from "../../admin-client";
import { useForm } from "react-hook-form";
import {
  EditableTextCell,
  IEditableTextCell,
  IRow,
  IRowCell,
  RowEditType,
  RowErrors,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
  applyCellEdits,
  cancelCellEdits,
  validateCellEdits,
} from "@patternfly/react-table";
import { SearchIcon } from "@patternfly/react-icons";
import { AddMessageBundleModal } from "../AddMessageBundleModal";
import { DEFAULT_LOCALE } from "../../i18n/i18n";
import { cloneDeep, isEqual, uniqWith } from "lodash-es";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { useAlerts } from "../../components/alert/Alerts";
import { KeyValueType } from "../../components/key-value-form/key-value-convert";
import RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";

type RealmOverridesProps = {
  internationalizationEnabled: boolean;
  watchSupportedLocales: string[];
  realm: RealmRepresentation;
};

export type BundleForm = {
  key: string;
  value: string;
  messageBundle: KeyValueType;
};

export enum RowEditAction {
  Save = "save",
  Cancel = "cancel",
  Edit = "edit",
  Delete = "delete",
}

const localeToDisplayName = (locale: string) => {
  try {
    return new Intl.DisplayNames([locale], { type: "language" }).of(locale);
  } catch (error) {
    return locale;
  }
};

export const RealmOverrides = ({
  internationalizationEnabled,
  watchSupportedLocales,
  realm,
}: RealmOverridesProps) => {
  const { t } = useTranslation();
  const [addMessageBundleModalOpen, setAddMessageBundleModalOpen] =
    useState(false);
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [messageBundles, setMessageBundles] = useState<[string, string][]>([]);
  const [selectMenuLocale, setSelectMenuLocale] = useState(DEFAULT_LOCALE);
  const [kebabOpen, setKebabOpen] = useState(false);
  const { getValues } = useForm();
  const [selectMenuValueSelected, setSelectMenuValueSelected] = useState(false);
  const [tableRows, setTableRows] = useState<IRow[]>([]);
  const [tableKey, setTableKey] = useState(0);
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");
  const bundleForm = useForm<BundleForm>({ mode: "onChange" });
  const { addAlert, addError } = useAlerts();
  const { realm: currentRealm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const [selectedRows, setSelectedRows] = useState<IRow[]>([]);

  const refreshTable = () => {
    setTableKey(tableKey + 1);
  };

  useFetch(
    async () => {
      let result = await adminClient.realms
        .getRealmLocalizationTexts({
          first,
          max,
          realm: realm.realm!,
          selectedLocale:
            selectMenuLocale ||
            getValues("defaultLocale") ||
            whoAmI.getLocale(),
        })
        .catch(() => []);

      const searchInBundles = (idx: number) => {
        return Object.entries(result).filter((i) => i[idx].includes(filter));
      };

      if (filter) {
        const filtered = uniqWith(
          searchInBundles(0).concat(searchInBundles(1)),
          isEqual,
        );

        result = Object.fromEntries(filtered);
      }

      return { result };
    },
    ({ result }) => {
      const bundles = Object.entries(result).slice(first, first + max + 1);
      setMessageBundles(bundles);

      const updatedRows = bundles.map<IRow>((messageBundle) => ({
        rowEditBtnAriaLabel: () =>
          t("rowEditBtnAriaLabel", {
            messageBundle: messageBundle[1],
          }),
        rowSaveBtnAriaLabel: () =>
          t("rowSaveBtnAriaLabel", {
            messageBundle: messageBundle[1],
          }),
        rowCancelBtnAriaLabel: () =>
          t("rowCancelBtnAriaLabel", {
            messageBundle: messageBundle[1],
          }),
        cells: [
          {
            title: (value, rowIndex, cellIndex, props) => (
              <EditableTextCell
                value={value!}
                rowIndex={rowIndex!}
                cellIndex={cellIndex!}
                props={props}
                isDisabled
                handleTextInputChange={handleTextInputChange}
                inputAriaLabel={messageBundle[0]}
              />
            ),
            props: {
              value: messageBundle[0],
            },
          },
          {
            title: (value, rowIndex, cellIndex, props) => (
              <EditableTextCell
                value={value!}
                rowIndex={rowIndex!}
                cellIndex={cellIndex!}
                props={props}
                handleTextInputChange={handleTextInputChange}
                inputAriaLabel={messageBundle[1]}
              />
            ),
            props: {
              value: messageBundle[1],
            },
          },
        ],
        isSelected: false,
      }));
      setTableRows(updatedRows);

      return bundles;
    },
    [tableKey, filter, first, max],
  );

  const handleTextInputChange = (
    newValue: string,
    evt: any,
    rowIndex: number,
    cellIndex: number,
  ) => {
    setTableRows((prev) => {
      const newRows = cloneDeep(prev);
      const textCell = newRows[rowIndex]?.cells?.[
        cellIndex
      ] as IEditableTextCell;
      textCell.props.editableValue = newValue;
      return newRows;
    });
  };

  const updateEditableRows = async (
    type: RowEditType,
    rowIndex?: number,
    validationErrors?: RowErrors,
  ) => {
    if (rowIndex === undefined) {
      return;
    }
    const newRows = cloneDeep(tableRows);
    let newRow: IRow;
    const invalid =
      !!validationErrors && Object.keys(validationErrors).length > 0;

    if (invalid) {
      newRow = validateCellEdits(newRows[rowIndex], type, validationErrors);
    } else if (type === RowEditAction.Cancel) {
      newRow = cancelCellEdits(newRows[rowIndex]);
    } else {
      newRow = applyCellEdits(newRows[rowIndex], type);
    }
    newRows[rowIndex] = newRow;

    // Update the copy of the retrieved data set so we can save it when the user saves changes

    if (!invalid && type === RowEditAction.Save) {
      const key = (newRow.cells?.[0] as IRowCell).props.value;
      const value = (newRow.cells?.[1] as IRowCell).props.value;

      // We only have one editable value, otherwise we'd need to save each
      try {
        await adminClient.realms.addLocalization(
          {
            realm: realm.realm!,
            selectedLocale:
              selectMenuLocale || getValues("defaultLocale") || DEFAULT_LOCALE,
            key,
          },
          value,
        );
        addAlert(t("updateMessageBundleSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(t("updateMessageBundleError"), AlertVariant.danger);
      }
    }
    setTableRows(newRows);
  };

  const handleModalToggle = () => {
    setAddMessageBundleModalOpen(!addMessageBundleModalOpen);
  };

  const options = [
    <SelectGroup label={t("defaultLocale")} key="group1">
      <SelectOption key={DEFAULT_LOCALE} value={DEFAULT_LOCALE}>
        {localeToDisplayName(DEFAULT_LOCALE)}
      </SelectOption>
    </SelectGroup>,
    <Divider key="divider" />,
    <SelectGroup label={t("supportedLocales")} key="group2">
      {watchSupportedLocales.map((locale) => (
        <SelectOption key={locale} value={locale}>
          {localeToDisplayName(locale)}
        </SelectOption>
      ))}
    </SelectGroup>,
  ];

  const addKeyValue = async (pair: KeyValueType): Promise<void> => {
    try {
      await adminClient.realms.addLocalization(
        {
          realm: currentRealm!,
          selectedLocale:
            selectMenuLocale || getValues("defaultLocale") || DEFAULT_LOCALE,
          key: pair.key,
        },
        pair.value,
      );

      adminClient.setConfig({
        realmName: currentRealm!,
      });
      refreshTable();
      addAlert(t("addMessageBundleSuccess"), AlertVariant.success);
    } catch (error) {
      addError(t("addMessageBundleError"), error);
    }
  };

  const deleteKey = async (key: string) => {
    try {
      await adminClient.realms.deleteRealmLocalizationTexts({
        realm: currentRealm!,
        selectedLocale: selectMenuLocale,
        key,
      });
      refreshTable();
      addAlert(t("deleteMessageBundleSuccess"));
    } catch (error) {
      addError("deleteMessageBundleError", error);
    }
  };

  const handleRowSelect = (isSelected: boolean, rowIndex: number) => {
    if (isSelected) {
      setSelectedRows((prevSelected) => [...prevSelected, tableRows[rowIndex]]);
    } else {
      setSelectedRows((prevSelected) =>
        prevSelected.filter((row) => row !== tableRows[rowIndex]),
      );
    }
  };

  console.log("selectedRows", selectedRows);

  return (
    <>
      {addMessageBundleModalOpen && (
        <AddMessageBundleModal
          handleModalToggle={handleModalToggle}
          save={(pair: any) => {
            addKeyValue(pair);
            handleModalToggle();
          }}
          form={bundleForm}
        />
      )}
      <div className="kc-localization-table">
        <PaginatingTableToolbar
          count={messageBundles.length}
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
          }}
          inputGroupPlaceholder={t("searchForMessageBundle")}
          toolbarItem={
            <>
              <Button
                data-testid="add-bundle-button"
                onClick={() => setAddMessageBundleModalOpen(true)}
              >
                {t("addMessageBundle")}
              </Button>
              <ToolbarItem>
                <Dropdown
                  toggle={<KebabToggle onToggle={setKebabOpen} />}
                  isOpen={kebabOpen}
                  isPlain
                  dropdownItems={[
                    <DropdownItem
                      key="action"
                      component="button"
                      isDisabled={messageBundles.length === 0}
                      onClick={() => {
                        setKebabOpen(false);
                      }}
                    >
                      {t("delete")}
                    </DropdownItem>,
                  ]}
                />
              </ToolbarItem>
            </>
          }
          searchTypeComponent={
            <ToolbarItem>
              <Select
                width={180}
                data-testid="filter-by-locale-select"
                isOpen={filterDropdownOpen}
                className="kc-filter-by-locale-select"
                variant={SelectVariant.single}
                isDisabled={!internationalizationEnabled}
                onToggle={(isExpanded) => setFilterDropdownOpen(isExpanded)}
                onSelect={(_, value) => {
                  setSelectMenuLocale(value.toString());
                  setSelectMenuValueSelected(true);
                  refreshTable();
                  setFilterDropdownOpen(false);
                }}
                selections={
                  selectMenuValueSelected
                    ? localeToDisplayName(selectMenuLocale)
                    : realm.defaultLocale !== ""
                    ? localeToDisplayName(DEFAULT_LOCALE)
                    : t("placeholderText")
                }
              >
                {options}
              </Select>
            </ToolbarItem>
          }
        >
          {messageBundles.length === 0 && !filter && (
            <ListEmptyState
              hasIcon
              message={t("noMessageBundles")}
              instructions={t("noMessageBundlesInstructions")}
              onPrimaryAction={handleModalToggle}
            />
          )}
          {messageBundles.length === 0 && filter && (
            <ListEmptyState
              hasIcon
              icon={SearchIcon}
              isSearchVariant
              message={t("noSearchResults")}
              instructions={t("noSearchResultsInstructions")}
            />
          )}
          {messageBundles.length !== 0 && (
            <Table
              aria-label={t("editableRowsTable")}
              data-testid="editable-rows-table"
              variant={TableVariant.compact}
              cells={[t("key"), t("value")]}
              rows={tableRows}
              onSelect={(event, isSelected, rowIndex) => {
                handleRowSelect(isSelected, rowIndex);
              }}
              canSelectAll
              selectVariant="checkbox"
              onRowEdit={(_, type, _b, rowIndex, validation) =>
                updateEditableRows(type, rowIndex, validation)
              }
              actions={[
                {
                  title: t("delete"),
                  onClick: (_, row) => {
                    deleteKey(
                      (tableRows[row].cells?.[0] as IRowCell).props.value,
                    );
                  },
                },
              ]}
            >
              <TableHeader />
              <TableBody />
            </Table>
          )}
        </PaginatingTableToolbar>
      </div>
    </>
  );
};
