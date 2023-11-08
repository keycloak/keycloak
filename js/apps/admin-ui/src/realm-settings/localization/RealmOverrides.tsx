import { ChangeEvent, useState, useEffect } from "react";
import {
  AlertVariant,
  Button,
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
import { useTranslation } from "react-i18next";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { adminClient } from "../../admin-client";
import { useForm } from "react-hook-form";
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
import {
  CheckIcon,
  PencilAltIcon,
  SearchIcon,
  TimesIcon,
} from "@patternfly/react-icons";
import { AddMessageBundleModal } from "../AddMessageBundleModal";
import { DEFAULT_LOCALE } from "../../i18n/i18n";
import { cloneDeep, isEqual, uniqWith } from "lodash-es";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { useAlerts } from "../../components/alert/Alerts";
import { KeyValueType } from "../../components/key-value-form/key-value-convert";
import RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import "./localization.css";

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
        console.error("Error fetching localization texts:", error);
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
  });

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
                  toggle={
                    <KebabToggle onToggle={() => setKebabOpen(!kebabOpen)} />
                  }
                  isOpen={kebabOpen}
                  isPlain
                  dropdownItems={[
                    <DropdownItem
                      key="action"
                      component="button"
                      isDisabled={messageBundles.length === 0}
                      onClick={() => setKebabOpen(false)}
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
            >
              <Thead>
                <Tr>
                  <Th>
                    <input
                      type="checkbox"
                      checked={areAllRowsSelected}
                      onChange={toggleSelectAllRows}
                    />
                  </Th>
                  <Th>{t("key")}</Th>
                  <Th>{t("value")}</Th>
                  <Th aria-hidden="true" />
                </Tr>
              </Thead>
              <Tbody>
                {tableRows.map((row, rowIndex) => (
                  <Tr key={(row.cells?.[0] as IRowCell).props.value}>
                    <Td
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
                    <Td dataLabel={t("key")}>
                      {(row.cells?.[0] as IRowCell).props.value}
                    </Td>
                    <Td dataLabel={t("value")} key={rowIndex}>
                      <Form
                        isHorizontal
                        className="kc-form-bundleValue"
                        onSubmit={handleSubmit(() => {
                          onSubmit(formValue, rowIndex);
                        })}
                      >
                        <FormGroup
                          fieldId="kc-bundleValue"
                          className="kc-bundleValue-row"
                        >
                          <div className="kc-form-group-bundleValue">
                            {editStates[rowIndex] ? (
                              <>
                                <KeycloakTextInput
                                  aria-label={t("editUserLabel")}
                                  type="text"
                                  value={formValue}
                                  onChange={(
                                    event: ChangeEvent<HTMLInputElement>,
                                  ) => {
                                    setFormValue(event.target.value);
                                  }}
                                  key={`edit-input-${rowIndex}`}
                                />
                                <Button
                                  data-testid="editUserLabelAcceptBtn"
                                  variant="link"
                                  className="kc-editUserLabelAcceptBtn"
                                  type="submit"
                                  icon={<CheckIcon />}
                                />
                                <Button
                                  data-testid="editUserLabelCancelBtn"
                                  variant="link"
                                  className="kc-editBundleValue-cancelBtn"
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
                                  className="kc-editBundleValue-btn"
                                  data-testid="editUserLabelBtn"
                                >
                                  <PencilAltIcon />
                                </Button>
                              </>
                            )}
                          </div>
                        </FormGroup>
                      </Form>
                    </Td>
                    <Td isActionCell>
                      <Dropdown
                        toggle={
                          <KebabToggle className="kc-realmOverrides-kebabToggle" />
                        }
                        onClick={() => {
                          deleteKey((row.cells?.[0] as IRowCell).props.value);
                        }}
                      />
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </PaginatingTableToolbar>
      </div>
    </>
  );
};
