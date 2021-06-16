import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";

import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";

type RealmSettingsThemesTabProps = {
  save: (realm: RealmRepresentation) => void;
  reset: () => void;
  realm: RealmRepresentation;
};

export const RealmSettingsThemesTab = ({
  save,
  reset,
  realm,
}: RealmSettingsThemesTabProps) => {
  const { t } = useTranslation("realm-settings");

  const [loginThemeOpen, setLoginThemeOpen] = useState(false);
  const [accountThemeOpen, setAccountThemeOpen] = useState(false);
  const [adminConsoleThemeOpen, setAdminConsoleThemeOpen] = useState(false);
  const [emailThemeOpen, setEmailThemeOpen] = useState(false);

  const [supportedLocalesOpen, setSupportedLocalesOpen] = useState(false);
  const [defaultLocaleOpen, setDefaultLocaleOpen] = useState(false);

  const { control, handleSubmit } = useFormContext();

  const themeTypes = useServerInfo().themes!;

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

  return (
    <>
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("loginTheme")}
            fieldId="kc-login-theme"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:loginTheme"
                forLabel={t("loginTheme")}
                forID="kc-login-theme"
              />
            }
          >
            <Controller
              name="loginTheme"
              control={control}
              defaultValue=""
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-login-theme"
                  onToggle={() => setLoginThemeOpen(!loginThemeOpen)}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setLoginThemeOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("loginTheme")}
                  isOpen={loginThemeOpen}
                  placeholderText="Select a theme"
                  data-testid="select-login-theme"
                >
                  {themeTypes.login.map((theme, idx) => (
                    <SelectOption
                      selected={theme.name === value}
                      key={`login-theme-${idx}`}
                      value={theme.name}
                    >
                      {t(`${theme.name}`)}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("accountTheme")}
            fieldId="kc-account-theme"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:accountTheme"
                forLabel={t("accountTheme")}
                forID="kc-account-theme"
              />
            }
          >
            <Controller
              name="accountTheme"
              control={control}
              defaultValue=""
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-account-theme"
                  onToggle={() => setAccountThemeOpen(!accountThemeOpen)}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setAccountThemeOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("accountTheme")}
                  isOpen={accountThemeOpen}
                  placeholderText="Select a theme"
                  data-testid="select-account-theme"
                >
                  {themeTypes.account.map((theme, idx) => (
                    <SelectOption
                      selected={theme.name === value}
                      key={`account-theme-${idx}`}
                      value={theme.name}
                    >
                      {t(`${theme.name}`)}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("adminTheme")}
            fieldId="kc-admin-console-theme"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:adminConsoleTheme"
                forLabel={t("adminTheme")}
                forID="kc-admin-console-theme"
              />
            }
          >
            <Controller
              name="adminTheme"
              control={control}
              defaultValue=""
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-admin-console-theme"
                  onToggle={() =>
                    setAdminConsoleThemeOpen(!adminConsoleThemeOpen)
                  }
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setAdminConsoleThemeOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("adminConsoleTheme")}
                  isOpen={adminConsoleThemeOpen}
                  placeholderText="Select a theme"
                  data-testid="select-admin-theme"
                >
                  {themeTypes.admin.map((theme, idx) => (
                    <SelectOption
                      selected={theme.name === value}
                      key={`admin-theme-${idx}`}
                      value={theme.name}
                    >
                      {t(`${theme.name}`)}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("emailTheme")}
            fieldId="kc-email-theme"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:emailTheme"
                forLabel={t("emailTheme")}
                forID="kc-email-theme"
              />
            }
          >
            <Controller
              name="emailTheme"
              control={control}
              defaultValue=""
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-email-theme"
                  onToggle={() => setEmailThemeOpen(!emailThemeOpen)}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setEmailThemeOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("emailTheme")}
                  isOpen={emailThemeOpen}
                  placeholderText="Select a theme"
                  data-testid="select-email-theme"
                >
                  {themeTypes.email.map((theme, idx) => (
                    <SelectOption
                      selected={theme.name === value}
                      key={`email-theme-${idx}`}
                      value={theme.name}
                    >
                      {t(`${theme.name}`)}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("internationalization")}
            fieldId="kc-internationalization"
          >
            <Controller
              name="internationalizationEnabled"
              control={control}
              defaultValue={internationalizationEnabled}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-t-internationalization"
                  label={t("common:enabled")}
                  labelOff={t("common:disabled")}
                  isChecked={value}
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
                fieldId="kc-t-supported-locales"
              >
                <Controller
                  name="supportedLocales"
                  control={control}
                  defaultValue={themeTypes?.account![0].locales}
                  render={({ value, onChange }) => (
                    <Select
                      toggleId="kc-t-supported-locales"
                      onToggle={() => {
                        setSupportedLocalesOpen(!supportedLocalesOpen);
                      }}
                      onSelect={(_, v) => {
                        const option = v as string;
                        if (!value) {
                          onChange([option]);
                        } else if (value!.includes(option)) {
                          onChange(
                            value.filter((item: string) => item !== option)
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
              <FormGroup label={t("defaultLocale")} fieldId="kc-default-locale">
                <Controller
                  name="defaultLocale"
                  control={control}
                  defaultValue=""
                  render={({ onChange, value }) => (
                    <Select
                      toggleId="kc-t-default-locale"
                      onToggle={() => setDefaultLocaleOpen(!defaultLocaleOpen)}
                      onSelect={(_, value) => {
                        onChange(value as string);
                        setDefaultLocaleOpen(false);
                      }}
                      selections={value && t(`allSupportedLocales.${value}`)}
                      variant={SelectVariant.single}
                      aria-label={t("defaultLocale")}
                      isOpen={defaultLocaleOpen}
                      placeholderText="Select one"
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
              data-testid="themes-tab-save"
            >
              {t("common:save")}
            </Button>
            <Button variant="link" onClick={reset}>
              {t("common:revert")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
