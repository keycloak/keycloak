import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Divider,
  FormGroup,
  PageSection,
  Select,
  SelectGroup,
  SelectOption,
  SelectVariant,
  Switch,
  TextContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
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
import { cloneDeep, isEqual, uniqWith } from "lodash-es";
import { useEffect, useMemo, useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormPanel, HelpItem } from "ui-shared";
import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { FormAccess } from "../components/form/FormAccess";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../components/table-toolbar/PaginatingTableToolbar";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { DEFAULT_LOCALE } from "../i18n/i18n";
import { convertToFormValues, localeToDisplayName } from "../util";
import { useFetch } from "../utils/useFetch";
import useLocaleSort, { mapByKey } from "../utils/useLocaleSort";
import { AddMessageBundleModal } from "./AddMessageBundleModal";

type LocalizationTabProps = {
  save: (realm: RealmRepresentation) => void;
  refresh: () => void;
  realm: RealmRepresentation;
};

type LocaleSpecificEntry = {
  key: string;
  value: string;
};

export enum RowEditAction {
  Save = "save",
  Cancel = "cancel",
  Edit = "edit",
  Delete = "delete",
}

export type BundleForm = {
  key: string;
  value: string;
  messageBundle: KeyValueType;
};

export const LocalizationTab = ({ save, realm }: LocalizationTabProps) => {
  const { t } = useTranslation();
  const [addMessageBundleModalOpen, setAddMessageBundleModalOpen] =
    useState(false);

  const [supportedLocalesOpen, setSupportedLocalesOpen] = useState(false);
  const [defaultLocaleOpen, setDefaultLocaleOpen] = useState(false);
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [selectMenuLocale, setSelectMenuLocale] = useState(DEFAULT_LOCALE);

  const { setValue, getValues, control, handleSubmit, formState } = useForm();
  const [selectMenuValueSelected, setSelectMenuValueSelected] = useState(false);
  const [messageBundles, setMessageBundles] = useState<LocaleSpecificEntry[]>(
    [],
  );
  const [tableRows, setTableRows] = useState<IRow[]>([]);

  const themeTypes = useServerInfo().themes!;
  const allLocales = useMemo(() => {
    const locales = Object.values(themeTypes).flatMap((theme) =>
      theme.flatMap(({ locales }) => (locales ? locales : [])),
    );
    return Array.from(new Set(locales));
  }, [themeTypes]);
  const bundleForm = useForm<BundleForm>({ mode: "onChange" });
  const { addAlert, addError } = useAlerts();
  const { realm: currentRealm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const localeSort = useLocaleSort();

  const defaultSupportedLocales = realm.supportedLocales?.length
    ? realm.supportedLocales
    : [DEFAULT_LOCALE];

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    setValue("supportedLocales", defaultSupportedLocales);
  };

  useEffect(setupForm, []);

  const watchSupportedLocales: string[] = useWatch({
    control,
    name: "supportedLocales",
    defaultValue: defaultSupportedLocales,
  });
  const internationalizationEnabled = useWatch({
    control,
    name: "internationalizationEnabled",
    defaultValue: realm.internationalizationEnabled,
  });

  const [tableKey, setTableKey] = useState(0);
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");

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
        // prevents server error in dev mode due to snowpack
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
      const bundles = localeSort(
        Object.entries(result).map<LocaleSpecificEntry>(([key, value]) => ({
          key,
          value,
        })),
        mapByKey("key"),
      ).slice(first, first + max + 1);

      setMessageBundles(bundles);

      const updatedRows = bundles.map<IRow>((messageBundle) => ({
        rowEditBtnAriaLabel: () =>
          t("rowEditBtnAriaLabel", {
            messageBundle: messageBundle.value,
          }),
        rowSaveBtnAriaLabel: () =>
          t("rowSaveBtnAriaLabel", {
            messageBundle: messageBundle.value,
          }),
        rowCancelBtnAriaLabel: () =>
          t("rowCancelBtnAriaLabel", {
            messageBundle: messageBundle.value,
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
                inputAriaLabel={messageBundle.key}
              />
            ),
            props: {
              value: messageBundle.key,
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
                inputAriaLabel={messageBundle.value}
              />
            ),
            props: {
              value: messageBundle.value,
            },
          },
        ],
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
        {localeToDisplayName(DEFAULT_LOCALE, whoAmI.getLocale())}
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
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("internationalization")}
            fieldId="kc-internationalization"
            labelIcon={
              <HelpItem
                helpText={t("internationalizationHelp")}
                fieldLabelId="internationalization"
              />
            }
          >
            <Controller
              name="internationalizationEnabled"
              control={control}
              defaultValue={realm.internationalizationEnabled}
              render={({ field }) => (
                <Switch
                  id="kc-l-internationalization"
                  label={t("enabled")}
                  labelOff={t("disabled")}
                  isChecked={field.value}
                  data-testid={
                    field.value
                      ? "internationalization-enabled"
                      : "internationalization-disabled"
                  }
                  onChange={field.onChange}
                  aria-label={t("internationalization")}
                />
              )}
            />
          </FormGroup>
          {internationalizationEnabled && (
            <>
              <FormGroup
                label={t("supportedLocales")}
                fieldId="kc-l-supported-locales"
              >
                <Controller
                  name="supportedLocales"
                  control={control}
                  defaultValue={defaultSupportedLocales}
                  render={({ field }) => (
                    <Select
                      toggleId="kc-l-supported-locales"
                      onToggle={(open) => {
                        setSupportedLocalesOpen(open);
                      }}
                      onSelect={(_, v) => {
                        const option = v as string;
                        if (field.value.includes(option)) {
                          field.onChange(
                            field.value.filter(
                              (item: string) => item !== option,
                            ),
                          );
                        } else {
                          field.onChange([...field.value, option]);
                        }
                      }}
                      onClear={() => {
                        field.onChange([]);
                      }}
                      selections={field.value}
                      variant={SelectVariant.typeaheadMulti}
                      aria-label={t("supportedLocales")}
                      isOpen={supportedLocalesOpen}
                      placeholderText={t("selectLocales")}
                    >
                      {allLocales.map((locale) => (
                        <SelectOption
                          selected={field.value.includes(locale)}
                          key={locale}
                          value={locale}
                        >
                          {localeToDisplayName(locale, whoAmI.getLocale())}
                        </SelectOption>
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("defaultLocale")}
                fieldId="kc-l-default-locale"
              >
                <Controller
                  name="defaultLocale"
                  control={control}
                  defaultValue={DEFAULT_LOCALE}
                  render={({ field }) => (
                    <Select
                      toggleId="kc-default-locale"
                      onToggle={() => setDefaultLocaleOpen(!defaultLocaleOpen)}
                      onSelect={(_, value) => {
                        field.onChange(value as string);
                        setDefaultLocaleOpen(false);
                      }}
                      selections={
                        field.value
                          ? localeToDisplayName(field.value, whoAmI.getLocale())
                          : realm.defaultLocale !== ""
                            ? localeToDisplayName(
                                realm.defaultLocale || DEFAULT_LOCALE,
                                whoAmI.getLocale(),
                              )
                            : t("placeholderText")
                      }
                      variant={SelectVariant.single}
                      aria-label={t("defaultLocale")}
                      isOpen={defaultLocaleOpen}
                      placeholderText={t("placeholderText")}
                      data-testid="select-default-locale"
                    >
                      {watchSupportedLocales.map((locale, idx) => (
                        <SelectOption
                          key={`default-locale-${idx}`}
                          value={locale}
                        >
                          {localeToDisplayName(locale, whoAmI.getLocale())}
                        </SelectOption>
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
            </>
          )}
          <ActionGroup>
            <Button
              variant="primary"
              isDisabled={!formState.isDirty}
              type="submit"
              data-testid="localization-tab-save"
            >
              {t("save")}
            </Button>
            <Button variant="link" onClick={setupForm}>
              {t("revert")}
            </Button>
          </ActionGroup>
        </FormAccess>

        <FormPanel className="kc-message-bundles" title="Edit message bundles">
          <TextContent className="messageBundleDescription">
            {t("messageBundleDescription")}
          </TextContent>
          <div className="tableBorder">
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
                <Button
                  data-testid="add-bundle-button"
                  onClick={() => setAddMessageBundleModalOpen(true)}
                >
                  {t("addMessageBundle")}
                </Button>
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
                        ? localeToDisplayName(
                            selectMenuLocale,
                            whoAmI.getLocale(),
                          )
                        : realm.defaultLocale !== ""
                          ? localeToDisplayName(
                              DEFAULT_LOCALE,
                              whoAmI.getLocale(),
                            )
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
                  onRowEdit={(_, type, _b, rowIndex, validation) =>
                    updateEditableRows(type, rowIndex, validation)
                  }
                  actions={[
                    {
                      title: t("delete"),
                      onClick: (_, row) =>
                        deleteKey(
                          (tableRows[row].cells?.[0] as IRowCell).props.value,
                        ),
                    },
                  ]}
                >
                  <TableHeader />
                  <TableBody />
                </Table>
              )}
            </PaginatingTableToolbar>
          </div>
        </FormPanel>
      </PageSection>
    </>
  );
};
