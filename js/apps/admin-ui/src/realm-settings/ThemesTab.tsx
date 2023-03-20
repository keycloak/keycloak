import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { convertToFormValues } from "../util";

type RealmSettingsThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const RealmSettingsThemesTab = ({
  realm,
  save,
}: RealmSettingsThemesTabProps) => {
  const { t } = useTranslation("realm-settings");

  const [loginThemeOpen, setLoginThemeOpen] = useState(false);
  const [accountThemeOpen, setAccountThemeOpen] = useState(false);
  const [adminUIThemeOpen, setAdminUIThemeOpen] = useState(false);
  const [emailThemeOpen, setEmailThemeOpen] = useState(false);

  const { control, handleSubmit, setValue } = useForm<RealmRepresentation>();
  const themeTypes = useServerInfo().themes!;

  const setupForm = () => {
    convertToFormValues(realm, setValue);
  };
  useEffect(setupForm, []);

  return (
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
              helpText={t("realm-settings-help:loginTheme")}
              fieldLabelId="realm-settings:loginTheme"
            />
          }
        >
          <Controller
            name="loginTheme"
            control={control}
            defaultValue=""
            render={({ field }) => (
              <Select
                toggleId="kc-login-theme"
                onToggle={() => setLoginThemeOpen(!loginThemeOpen)}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setLoginThemeOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("loginTheme")}
                isOpen={loginThemeOpen}
                placeholderText="Select a theme"
                data-testid="select-login-theme"
              >
                {themeTypes.login.map((theme, idx) => (
                  <SelectOption
                    selected={theme.name === field.value}
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
              helpText={t("realm-settings-help:accountTheme")}
              fieldLabelId="realm-settings:accountTheme"
            />
          }
        >
          <Controller
            name="accountTheme"
            control={control}
            defaultValue=""
            render={({ field }) => (
              <Select
                toggleId="kc-account-theme"
                onToggle={() => setAccountThemeOpen(!accountThemeOpen)}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setAccountThemeOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("accountTheme")}
                isOpen={accountThemeOpen}
                placeholderText="Select a theme"
                data-testid="select-account-theme"
              >
                {themeTypes.account.map((theme, idx) => (
                  <SelectOption
                    selected={theme.name === field.value}
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
          fieldId="kc-admin-ui-theme"
          labelIcon={
            <HelpItem
              helpText={t("realm-settings-help:adminUITheme")}
              fieldLabelId="realm-settings:adminTheme"
            />
          }
        >
          <Controller
            name="adminTheme"
            control={control}
            defaultValue=""
            render={({ field }) => (
              <Select
                toggleId="kc-admin-ui-theme"
                onToggle={() => setAdminUIThemeOpen(!adminUIThemeOpen)}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setAdminUIThemeOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("adminUITheme")}
                isOpen={adminUIThemeOpen}
                placeholderText="Select a theme"
                data-testid="select-admin-theme"
              >
                {themeTypes.admin.map((theme, idx) => (
                  <SelectOption
                    selected={theme.name === field.value}
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
              helpText={t("realm-settings-help:emailTheme")}
              fieldLabelId="realm-settings:emailTheme"
            />
          }
        >
          <Controller
            name="emailTheme"
            control={control}
            defaultValue=""
            render={({ field }) => (
              <Select
                toggleId="kc-email-theme"
                onToggle={() => setEmailThemeOpen(!emailThemeOpen)}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setEmailThemeOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("emailTheme")}
                isOpen={emailThemeOpen}
                placeholderText="Select a theme"
                data-testid="select-email-theme"
              >
                {themeTypes.email.map((theme, idx) => (
                  <SelectOption
                    selected={theme.name === field.value}
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
        <ActionGroup>
          <Button variant="primary" type="submit" data-testid="themes-tab-save">
            {t("common:save")}
          </Button>
          <Button variant="link" onClick={setupForm}>
            {t("common:revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
