import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useFormContext, useWatch } from "react-hook-form";
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

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { AddMessageBundleModal } from "./AddMessageBundleModal";
import { useAlerts } from "../components/alert/Alerts";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useRealm } from "../context/realm-context/RealmContext";
import { DEFAULT_LOCALE } from "../i18n";

type LocalizationTabProps = {
  save: (realm: RealmRepresentation) => void;
  reset: () => void;
  refresh: () => void;
  realm: RealmRepresentation;
};

export type KeyValueType = { key: string; value: string };

export type BundleForm = {
  messageBundle: KeyValueType;
};

export const LocalizationTab = ({
  save,
  reset,
  realm,
}: LocalizationTabProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const [addMessageBundleModalOpen, setAddMessageBundleModalOpen] =
    useState(false);

  const [supportedLocalesOpen, setSupportedLocalesOpen] = useState(false);
  const [defaultLocaleOpen, setDefaultLocaleOpen] = useState(false);
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [selectMenuLocale, setSelectMenuLocale] = useState(DEFAULT_LOCALE);

  const { getValues, control, handleSubmit, formState } = useFormContext();
  const [selectMenuValueSelected, setSelectMenuValueSelected] = useState(false);

  const themeTypes = useServerInfo().themes!;
  const bundleForm = useForm<BundleForm>({ mode: "onChange" });
  const { addAlert, addError } = useAlerts();
  const { realm: currentRealm } = useRealm();

  const watchSupportedLocales = useWatch<string[]>({
    control,
    name: "supportedLocales",
  });
  const internationalizationEnabled = useWatch({
    control,
    name: "internationalizationEnabled",
    defaultValue: false,
  });

  const loader = async () => {
    try {
      const result = await adminClient.realms.getRealmLocalizationTexts({
        realm: realm.realm!,
        selectedLocale:
          selectMenuLocale || getValues("defaultLocale") || DEFAULT_LOCALE,
      });

      return Object.entries(result);
    } catch (error) {
      return [];
    }
  };

  const tableLoader = async () => {
    try {
      const result = await adminClient.realms.getRealmLocalizationTexts({
        realm: currentRealm,
        selectedLocale: selectMenuLocale,
      });

      return Object.entries(result);
    } catch (error) {
      return [];
    }
  };

  const handleModalToggle = () => {
    setAddMessageBundleModalOpen(!addMessageBundleModalOpen);
  };

  const options = [
    <SelectGroup label={t("defaultLocale")} key="group1">
      {watchSupportedLocales
        ?.filter((item) => item === realm.defaultLocale)
        .map((locale) => (
          <SelectOption key={locale} value={locale}>
            {t(`allSupportedLocales.${locale}`)}
          </SelectOption>
        ))}
    </SelectGroup>,
    <Divider key="divider" />,
    <SelectGroup label={t("supportedLocales")} key="group2">
      {watchSupportedLocales
        ?.filter((item) => item !== realm.defaultLocale)
        .map((locale) => (
          <SelectOption key={locale} value={locale}>
            {t(`allSupportedLocales.${locale}`)}
          </SelectOption>
        ))}
    </SelectGroup>,
  ];

  const [tableKey, setTableKey] = useState(0);

  const refreshTable = () => {
    setTableKey(new Date().getTime());
  };

  const addKeyValue = async (pair: KeyValueType): Promise<void> => {
    try {
      adminClient.setConfig({
        requestConfig: { headers: { "Content-Type": "text/plain" } },
      });
      await adminClient.realms.addLocalization(
        {
          realm: currentRealm!,
          selectedLocale:
            selectMenuLocale || getValues("defaultLocale") || DEFAULT_LOCALE,
          key: pair.key,
        },
        pair.value
      );

      adminClient.setConfig({
        realmName: currentRealm!,
      });
      refreshTable();
      addAlert(t("pairCreatedSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:pairCreatedError", error);
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
                helpText="realm-settings-help:internationalization"
                fieldLabelId="realm-settings:internationalization"
              />
            }
          >
            <Controller
              name="internationalizationEnabled"
              control={control}
              defaultValue={false}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-l-internationalization"
                  label={t("common:enabled")}
                  labelOff={t("common:disabled")}
                  isChecked={internationalizationEnabled}
                  data-testid={
                    value
                      ? "internationalization-enabled"
                      : "internationalization-disabled"
                  }
                  onChange={onChange}
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
                  defaultValue={[DEFAULT_LOCALE]}
                  render={({ onChange, value }) => (
                    <Select
                      toggleId="kc-l-supported-locales"
                      onToggle={(open) => {
                        setSupportedLocalesOpen(open);
                      }}
                      onSelect={(_, v) => {
                        const option = v as string;
                        if (value.includes(option)) {
                          onChange(
                            value.filter(
                              (item: string) =>
                                item !== option || option === DEFAULT_LOCALE
                            )
                          );
                        } else {
                          onChange([...value, option]);
                        }
                      }}
                      onClear={() => {
                        onChange([]);
                      }}
                      selections={value}
                      variant={SelectVariant.typeaheadMulti}
                      aria-label={t("supportedLocales")}
                      isOpen={supportedLocalesOpen}
                      placeholderText={t("selectLocales")}
                    >
                      {themeTypes.login![0].locales.map(
                        (locale: string, idx: number) => (
                          <SelectOption
                            selected={value.includes(locale)}
                            key={`locale-${idx}`}
                            value={locale}
                          >
                            {t(`allSupportedLocales.${locale}`)}
                          </SelectOption>
                        )
                      )}
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
                  render={({ onChange, value }) => (
                    <Select
                      toggleId="kc-default-locale"
                      onToggle={() => setDefaultLocaleOpen(!defaultLocaleOpen)}
                      onSelect={(_, value) => {
                        onChange(value as string);
                        setDefaultLocaleOpen(false);
                      }}
                      selections={
                        value
                          ? t(`allSupportedLocales.${value}`)
                          : realm.defaultLocale !== ""
                          ? t(
                              `allSupportedLocales.${
                                realm.defaultLocale || DEFAULT_LOCALE
                              }`
                            )
                          : t("placeholderText")
                      }
                      variant={SelectVariant.single}
                      aria-label={t("defaultLocale")}
                      isOpen={defaultLocaleOpen}
                      placeholderText={t("placeholderText")}
                      data-testid="select-default-locale"
                    >
                      {watchSupportedLocales?.map(
                        (locale: string, idx: number) => (
                          <SelectOption
                            key={`default-locale-${idx}`}
                            value={locale}
                          >
                            {t(`allSupportedLocales.${locale}`)}
                          </SelectOption>
                        )
                      )}
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
              {t("common:save")}
            </Button>
            <Button variant="link" onClick={reset}>
              {t("common:revert")}
            </Button>
          </ActionGroup>
        </FormAccess>

        <FormPanel className="kc-message-bundles" title="Edit message bundles">
          <TextContent className="messageBundleDescription">
            {t("messageBundleDescription")}
          </TextContent>
          <div className="tableBorder">
            <KeycloakDataTable
              key={tableKey}
              loader={selectMenuValueSelected ? tableLoader : loader}
              ariaLabelKey="realm-settings:localization"
              searchTypeComponent={
                <ToolbarItem>
                  <Select
                    width={180}
                    data-testid="filter-by-locale-select"
                    isOpen={filterDropdownOpen}
                    className="kc-filter-by-locale-select"
                    variant={SelectVariant.single}
                    isDisabled={!formState.isSubmitSuccessful}
                    onToggle={(isExpanded) => setFilterDropdownOpen(isExpanded)}
                    onSelect={(_, value) => {
                      setSelectMenuLocale(value.toString());
                      setSelectMenuValueSelected(true);
                      refreshTable();
                      setFilterDropdownOpen(false);
                    }}
                    selections={
                      selectMenuValueSelected
                        ? t(`allSupportedLocales.${selectMenuLocale}`)
                        : realm.defaultLocale !== ""
                        ? t(`allSupportedLocales.${DEFAULT_LOCALE}`)
                        : t("placeholderText")
                    }
                  >
                    {options}
                  </Select>
                </ToolbarItem>
              }
              toolbarItem={
                <Button
                  data-testid="add-bundle-button"
                  isDisabled={!formState.isSubmitSuccessful}
                  onClick={() => setAddMessageBundleModalOpen(true)}
                >
                  {t("addMessageBundle")}
                </Button>
              }
              searchPlaceholderKey=" "
              emptyState={
                <ListEmptyState
                  hasIcon={true}
                  message={t("noMessageBundles")}
                  instructions={t("noMessageBundlesInstructions")}
                  onPrimaryAction={handleModalToggle}
                  primaryActionText={t("addMessageBundle")}
                />
              }
              canSelectAll
              columns={[
                {
                  name: "Key",
                  cellRenderer: (row) => row[0],
                },
                {
                  name: "Value",
                  cellRenderer: (row) => row[1],
                },
              ]}
            />
          </div>
        </FormPanel>
      </PageSection>
    </>
  );
};
