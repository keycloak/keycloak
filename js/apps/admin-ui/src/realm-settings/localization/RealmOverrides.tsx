import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  Dropdown,
  DropdownItem,
  Form,
  FormGroup,
  KebabToggle,
  Select,
  SelectGroup,
  SelectOption,
  SelectVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  CheckIcon,
  PencilAltIcon,
  SearchIcon,
  TimesIcon,
} from "@patternfly/react-icons";
import {
  IRow,
  IRowCell,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";
import { cloneDeep, isEqual, uniqWith } from "lodash-es";
import { ChangeEvent, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { DEFAULT_LOCALE } from "../../i18n/i18n";
import { localeToDisplayName } from "../../util";
import { AddMessageBundleModal } from "../AddMessageBundleModal";

type RealmOverridesProps = {
  internationalizationEnabled: boolean;
  watchSupportedLocales: string[];
  realm: RealmRepresentation;
};

type EditStatesType = { [key: number]: boolean };

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
  const { getValues, handleSubmit } = useForm();
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
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const [areAllRowsSelected, setAreAllRowsSelected] = useState(false);
  const [editStates, setEditStates] = useState<EditStatesType>({});
  const [formValue, setFormValue] = useState("");
  const refreshTable = () => {
    setTableKey(tableKey + 1);
  };

  useEffect(() => {
    const fetchLocalizationTexts = async () => {
      try {
        let result = await adminClient.realms.getRealmLocalizationTexts({
          first,
          max,
          realm: realm.realm!,
          selectedLocale:
            selectMenuLocale ||
            getValues("defaultLocale") ||
            whoAmI.getLocale(),
        });

        if (filter) {
          const searchInBundles = (idx: number) => {
            return Object.entries(result).filter((i) =>
              i[idx].includes(filter),
            );
          };

          const filtered = uniqWith(
            searchInBundles(0).concat(searchInBundles(1)),
            isEqual,
          );

          result = Object.fromEntries(filtered);
        }

        return Object.entries(result).slice(first, first + max);
      } catch (error) {
        return [];
      }
    };

    fetchLocalizationTexts().then((bundles) => {
      setMessageBundles(bundles);

      const updatedRows: IRow[] = bundles.map(
        (messageBundle): IRow => ({
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
              title: messageBundle[0],
              props: {
                value: messageBundle[0],
              },
            },
            {
              title: messageBundle[1],
              props: {
                value: messageBundle[1],
              },
            },
          ],
        }),
      );

      setTableRows(updatedRows);
    });
  }, [tableKey, first, max, filter]);

  const handleModalToggle = () => {
    setAddMessageBundleModalOpen(!addMessageBundleModalOpen);
  };

  const options = [
    <SelectGroup label={t("defaultLocale")} key="group1">
      <SelectOption key={DEFAULT_LOCALE} value={DEFAULT_LOCALE}>
        {localeToDisplayName(DEFAULT_LOCALE, whoAmI.getDisplayName())}
      </SelectOption>
    </SelectGroup>,
    <Divider key="divider" />,
    <SelectGroup label={t("supportedLocales")} key="group2">
      {watchSupportedLocales.map((locale) => (
        <SelectOption key={locale} value={locale}>
          {localeToDisplayName(locale, whoAmI.getLocale())}
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
      bundleForm.setValue("key", "");
      bundleForm.setValue("value", "");
      addAlert(t("addMessageBundleSuccess"), AlertVariant.success);
    } catch (error) {
      addError(t("addMessageBundleError"), error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteConfirmMessageBundle",
    messageKey: t("messageBundleDeleteConfirmDialog", {
      count: selectedRowKeys.length,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        for (const key of selectedRowKeys) {
          await adminClient.realms.deleteRealmLocalizationTexts({
            realm: currentRealm!,
            selectedLocale: selectMenuLocale,
            key: key,
          });
        }
        setAreAllRowsSelected(false);
        setSelectedRowKeys([]);
        refreshTable();
        addAlert(t("deleteAllMessagesBundleSuccess"), AlertVariant.success);
      } catch (error) {
        addError("deleteAllMessagesBundleError", error);
      }
    },
  });

  const handleRowSelect = (
    event: ChangeEvent<HTMLInputElement>,
    rowIndex: number,
  ) => {
    const selectedKey = (tableRows[rowIndex].cells?.[0] as IRowCell).props
      .value;
    if (event.target.checked) {
      setSelectedRowKeys((prevSelected) => [...prevSelected, selectedKey]);
    } else {
      setSelectedRowKeys((prevSelected) =>
        prevSelected.filter((key) => key !== selectedKey),
      );
    }

    setAreAllRowsSelected(
      tableRows.length ===
        selectedRowKeys.length + (event.target.checked ? 1 : -1),
    );
  };

  const toggleSelectAllRows = () => {
    if (areAllRowsSelected) {
      setSelectedRowKeys([]);
    } else {
      setSelectedRowKeys(
        tableRows.map((row) => (row.cells?.[0] as IRowCell).props.value),
      );
    }
    setAreAllRowsSelected(!areAllRowsSelected);
  };

  const isRowSelected = (key: any) => {
    return selectedRowKeys.includes(key);
  };

  const onSubmit = async (inputValue: string, rowIndex: number) => {
    const newRows = cloneDeep(tableRows);

    const newRow = cloneDeep(newRows[rowIndex]);
    (newRow.cells?.[1] as IRowCell).props.value = inputValue;
    newRows[rowIndex] = newRow;

    try {
      const key = (newRow.cells?.[0] as IRowCell).props.value;
      const value = (newRow.cells?.[1] as IRowCell).props.value;

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
      setTableRows(newRows);
    } catch (error) {
      addAlert(t("updateMessageBundleError"), AlertVariant.danger);
    }

    setEditStates((prevEditStates) => ({
      ...prevEditStates,
      [rowIndex]: false,
    }));
  };

  return (
    <>
      <DeleteConfirm />
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
                toggle={
                  <KebabToggle onToggle={() => setKebabOpen(!kebabOpen)} />
                }
                isOpen={kebabOpen}
                isPlain
                data-testid="toolbar-deleteBtn"
                dropdownItems={[
                  <DropdownItem
                    key="action"
                    component="button"
                    data-testid="delete-selected-bundleBtn"
                    isDisabled={
                      messageBundles.length === 0 ||
                      selectedRowKeys.length === 0
                    }
                    onClick={() => {
                      toggleDeleteDialog();
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
                  ? localeToDisplayName(selectMenuLocale, whoAmI.getLocale())
                  : realm.defaultLocale !== ""
                    ? localeToDisplayName(DEFAULT_LOCALE, whoAmI.getLocale())
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
            instructions={t("noRealmOverridesSearchResultsInstructions")}
          />
        )}
        {messageBundles.length !== 0 && (
          <Table
            aria-label={t("editableRowsTable")}
            data-testid="editable-rows-table"
          >
            <Thead>
              <Tr>
                <Th className="pf-u-px-lg">
                  <input
                    type="checkbox"
                    aria-label={t("selectAll")}
                    checked={areAllRowsSelected}
                    onChange={toggleSelectAllRows}
                    data-testid="selectAll"
                  />
                </Th>
                <Th className="pf-u-py-lg">{t("key")}</Th>
                <Th className="pf-u-py-lg">{t("value")}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {tableRows.map((row, rowIndex) => (
                <Tr key={(row.cells?.[0] as IRowCell).props.value}>
                  <Td
                    className="pf-u-px-lg"
                    select={{
                      rowIndex,
                      onSelect: (event) =>
                        handleRowSelect(
                          event as ChangeEvent<HTMLInputElement>,
                          rowIndex,
                        ),
                      isSelected: isRowSelected(
                        (row.cells?.[0] as IRowCell).props.value,
                      ),
                    }}
                  />
                  <Td className="pf-m-sm pf-u-px-sm" dataLabel={t("key")}>
                    {(row.cells?.[0] as IRowCell).props.value}
                  </Td>
                  <Td
                    className="pf-m-sm pf-u-px-sm"
                    dataLabel={t("value")}
                    key={rowIndex}
                  >
                    <Form
                      isHorizontal
                      className="kc-form-bundleValue"
                      onSubmit={handleSubmit(() => {
                        onSubmit(formValue, rowIndex);
                      })}
                    >
                      <FormGroup
                        fieldId="kc-bundleValue"
                        className="pf-u-display-inline-block"
                      >
                        {editStates[rowIndex] ? (
                          <>
                            <KeycloakTextInput
                              aria-label={t("editUserLabel")}
                              type="text"
                              className="pf-u-w-initial"
                              data-testid={`editUserLabelInput-${rowIndex}`}
                              value={formValue}
                              onChange={(
                                event: ChangeEvent<HTMLInputElement>,
                              ) => {
                                setFormValue(event.target.value);
                              }}
                              key={`edit-input-${rowIndex}`}
                            />
                            <Button
                              variant="link"
                              className="pf-m-plain"
                              data-testid={`editUserLabelAcceptBtn-${rowIndex}`}
                              type="submit"
                              icon={<CheckIcon />}
                            />
                            <Button
                              variant="link"
                              className="pf-m-plain"
                              data-testid={`editUserLabelCancelBtn-${rowIndex}`}
                              icon={<TimesIcon />}
                              onClick={() => {
                                setEditStates((prevEditStates) => ({
                                  ...prevEditStates,
                                  [rowIndex]: false,
                                }));
                              }}
                            />
                          </>
                        ) : (
                          <>
                            <span>
                              {(row.cells?.[1] as IRowCell).props.value}
                            </span>
                            <Button
                              onClick={() => {
                                const currentValue = (
                                  tableRows[rowIndex].cells?.[1] as IRowCell
                                ).props.value;
                                setFormValue(currentValue);
                                setEditStates((prevState) => ({
                                  ...prevState,
                                  [rowIndex]: true,
                                }));
                              }}
                              key={`edit-button-${rowIndex}`}
                              aria-label={t("editUserLabel")}
                              variant="link"
                              className="pf-m-plain"
                              data-testid={`editUserLabelBtn-${rowIndex}`}
                            >
                              <PencilAltIcon />
                            </Button>
                          </>
                        )}
                      </FormGroup>
                    </Form>
                  </Td>
                  <Td isActionCell>
                    <Dropdown
                      toggle={
                        <KebabToggle
                          className="pf-m-plain"
                          data-testid="realmOverrides-deleteKebabToggle"
                        />
                      }
                      onClick={() => {
                        setSelectedRowKeys([
                          (row.cells?.[0] as IRowCell).props.value,
                        ]);
                        toggleDeleteDialog();
                        setKebabOpen(false);
                      }}
                    />
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </PaginatingTableToolbar>
    </>
  );
};
