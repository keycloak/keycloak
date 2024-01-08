import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  Tab,
  TabTitleText,
  Tabs,
} from "@patternfly/react-core";
import { useEffect, useMemo, useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { DEFAULT_LOCALE } from "../../i18n/i18n";
import { convertToFormValues, localeToDisplayName } from "../../util";
import { EffectiveMessageBundles } from "./EffectiveMessageBundles";
import { RealmOverrides } from "./RealmOverrides";

type LocalizationTabProps = {
  save: (realm: RealmRepresentation) => void;
  realm: RealmRepresentation;
};

export const LocalizationTab = ({ save, realm }: LocalizationTabProps) => {
  const { t } = useTranslation();
  const { whoAmI } = useWhoAmI();

  const [activeTab, setActiveTab] = useState(0);
  const { setValue, control, handleSubmit, formState } = useForm();
  const [supportedLocalesOpen, setSupportedLocalesOpen] = useState(false);
  const [defaultLocaleOpen, setDefaultLocaleOpen] = useState(false);

  const defaultSupportedLocales = realm.supportedLocales?.length
    ? realm.supportedLocales
    : [DEFAULT_LOCALE];

  const themeTypes = useServerInfo().themes!;
  const allLocales = useMemo(() => {
    const locales = Object.values(themeTypes).flatMap((theme) =>
      theme.flatMap(({ locales }) => (locales ? locales : [])),
    );
    return Array.from(new Set(locales));
  }, [themeTypes]);

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

  return (
    <Tabs
      activeKey={activeTab}
      onSelect={(_, key) => setActiveTab(key as number)}
    >
      <Tab
        id="locales"
        eventKey={0}
        title={<TabTitleText>{t("locales")}</TabTitleText>}
        data-testid="rs-localization-locales-tab"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg pf-u-ml-md"
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
                  className="pf-u-mt-sm"
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
      </Tab>
      <Tab
        id="realm-overrides"
        eventKey={1}
        title={
          <TabTitleText>
            {t("realmOverrides")}{" "}
            <HelpItem
              fieldLabelId="realm-overrides"
              helpText={t("realmOverridesHelp")}
              noVerticalAlign={false}
              unWrap
            />
          </TabTitleText>
        }
        data-testid="rs-localization-realm-overrides-tab"
      >
        <RealmOverrides
          internationalizationEnabled={internationalizationEnabled}
          watchSupportedLocales={watchSupportedLocales}
          realm={realm}
        />
      </Tab>
      <Tab
        id="effective-message-bundles"
        eventKey={2}
        title={
          <TabTitleText>
            {t("effectiveMessageBundles")}
            <HelpItem
              fieldLabelId="effective-message-bundles"
              helpText={t("effectiveMessageBundlesHelp")}
              noVerticalAlign={false}
              unWrap
            />
          </TabTitleText>
        }
        data-testid="rs-localization-effective-message-bundles-tab"
      >
        <EffectiveMessageBundles
          defaultSupportedLocales={defaultSupportedLocales}
        />
      </Tab>
    </Tabs>
  );
};
