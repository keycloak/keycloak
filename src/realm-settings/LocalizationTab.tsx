import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useFormContext, useWatch } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextContent,
} from "@patternfly/react-core";

import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { AddMessageBundleModal } from "./AddMessageBundleModal";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";

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
  refresh,
}: LocalizationTabProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const [addMessageBundleModalOpen, setAddMessageBundleModalOpen] =
    useState(false);
  const [key, setKey] = useState(0);

  const [supportedLocalesOpen, setSupportedLocalesOpen] = useState(false);
  const [defaultLocaleOpen, setDefaultLocaleOpen] = useState(false);

  const { getValues, control, handleSubmit } = useFormContext();
  const [valueSelected, setValueSelected] = useState(false);
  const themeTypes = useServerInfo().themes!;
  const bundleForm = useForm<BundleForm>({ mode: "onChange" });
  const { addAlert } = useAlerts();
  const { realm: currentRealm } = useRealm();

  const watchSupportedLocales = useWatch({
    control,
    name: "supportedLocales",
    defaultValue: themeTypes?.account![0].locales,
  });

  const internationalizationEnabled = useWatch({
    control,
    name: "internationalizationEnabled",
    defaultValue: realm?.internationalizationEnabled,
  });

  const loader = async () => {
    if (realm) {
      const result = await adminClient.realms.getRealmLocalizationTexts({
        realm: realm.realm!,
        selectedLocale: getValues("defaultLocale") || "en",
      });
      return Object.keys(result).map((key) => [key, result[key]]);
    }
    return [[]];
  };

  const handleModalToggle = () => {
    setAddMessageBundleModalOpen(!addMessageBundleModalOpen);
  };

  const addKeyValue = async (pair: KeyValueType): Promise<void> => {
    try {
      adminClient.setConfig({
        requestConfig: { headers: { "Content-Type": "text/plain" } },
      });
      await adminClient.realms.addLocalization(
        {
          realm: currentRealm!,
          selectedLocale: getValues("defaultLocale") || "en",
          key: pair.key,
        },
        pair.value
      );

      adminClient.setConfig({
        realmName: currentRealm!,
      });
      refresh();
      addAlert(t("realm-settings:pairCreatedSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("realm-settings:pairCreatedError", { error }),
        AlertVariant.danger
      );
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
        <FormPanel
          className="kc-login-screen"
          title="Login screen customization"
        >
          <FormAccess
            isHorizontal
            role="manage-realm"
            className="pf-u-mt-lg"
            onSubmit={handleSubmit(save)}
          >
            <FormGroup
              label={t("internationalization")}
              fieldId="kc-internationalization"
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
                    defaultValue={themeTypes?.account![0].locales}
                    render={({ onChange }) => (
                      <Select
                        toggleId="kc-l-supported-locales"
                        onToggle={() => {
                          setSupportedLocalesOpen(!supportedLocalesOpen);
                        }}
                        onSelect={(_, v) => {
                          const option = v as string;
                          if (!watchSupportedLocales) {
                            onChange([option]);
                          } else if (watchSupportedLocales!.includes(option)) {
                            onChange(
                              watchSupportedLocales.filter(
                                (item: string) => item !== option
                              )
                            );
                          } else {
                            onChange([...watchSupportedLocales, option]);
                          }
                        }}
                        onClear={() => {
                          onChange([]);
                        }}
                        selections={watchSupportedLocales}
                        variant={SelectVariant.typeaheadMulti}
                        aria-label={t("supportedLocales")}
                        isOpen={supportedLocalesOpen}
                        placeholderText={"Select locales"}
                      >
                        {themeTypes?.login![0].locales.map(
                          (locale: string, idx: number) => (
                            <SelectOption
                              selected={true}
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
                    defaultValue={realm?.defaultLocale}
                    render={({ onChange, value }) => (
                      <Select
                        toggleId="kc-default-locale"
                        onToggle={() =>
                          setDefaultLocaleOpen(!defaultLocaleOpen)
                        }
                        onSelect={(_, value) => {
                          onChange(value as string);
                          setValueSelected(true);
                          setKey(new Date().getTime());
                          setDefaultLocaleOpen(false);
                        }}
                        selections={
                          valueSelected
                            ? t(`allSupportedLocales.${value}`)
                            : realm.defaultLocale !== ""
                            ? t(
                                `allSupportedLocales.${
                                  realm.defaultLocale || "en"
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
                        {watchSupportedLocales.map(
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
        </FormPanel>

        <FormPanel className="kc-message-bundles" title="Edit message bundles">
          <TextContent className="messageBundleDescription">
            {t("messageBundleDescription")}
          </TextContent>
          <div className="tableBorder">
            <KeycloakDataTable
              key={key}
              loader={loader}
              ariaLabelKey="client-scopes:clientScopeList"
              toolbarItem={
                <Button
                  data-testid="add-bundle-button"
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
