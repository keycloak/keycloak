import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button, PageSection } from "@patternfly/react-core";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertToFormValues } from "../../util";

type ThemeSettingsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const ThemeSettingsTab = ({ realm, save }: ThemeSettingsTabProps) => {
  const { t } = useTranslation();

  const form = useForm<RealmRepresentation>();
  const { handleSubmit, setValue } = form;
  const themeTypes = useServerInfo().themes!;

  const setupForm = () => {
    convertToFormValues(realm, setValue);
  };
  useEffect(setupForm, []);

  const appendEmptyChoice = (items: { key: string; value: string }[]) => [
    { key: "", value: t("choose") },
    ...items,
  ];

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-v5-u-mt-lg"
        onSubmit={handleSubmit(save)}
      >
        <FormProvider {...form}>
          <DefaultSwitchControl
            name="attributes.darkMode"
            labelIcon={t("darkModeEnabledHelp")}
            label={t("darkModeEnabled")}
            defaultValue="true"
            stringify
          />
          <SelectControl
            id="kc-login-theme"
            name="loginTheme"
            label={t("loginTheme")}
            labelIcon={t("loginThemeHelp")}
            controller={{ defaultValue: "" }}
            options={appendEmptyChoice(
              themeTypes.login.map((theme) => ({
                key: theme.name,
                value: theme.name,
              })),
            )}
          />
          <SelectControl
            id="kc-account-theme"
            name="accountTheme"
            label={t("accountTheme")}
            labelIcon={t("accountThemeHelp")}
            placeholderText={t("selectATheme")}
            controller={{ defaultValue: "" }}
            options={appendEmptyChoice(
              themeTypes.account.map((theme) => ({
                key: theme.name,
                value: theme.name,
              })),
            )}
          />
          <SelectControl
            id="kc-admin-theme"
            name="adminTheme"
            label={t("adminTheme")}
            labelIcon={t("adminThemeHelp")}
            placeholderText={t("selectATheme")}
            controller={{ defaultValue: "" }}
            options={appendEmptyChoice(
              themeTypes.admin.map((theme) => ({
                key: theme.name,
                value: theme.name,
              })),
            )}
          />
          <SelectControl
            id="kc-email-theme"
            name="emailTheme"
            label={t("emailTheme")}
            labelIcon={t("emailThemeHelp")}
            placeholderText={t("selectATheme")}
            controller={{ defaultValue: "" }}
            options={appendEmptyChoice(
              themeTypes.email.map((theme) => ({
                key: theme.name,
                value: theme.name,
              })),
            )}
          />
        </FormProvider>
        <ActionGroup>
          <Button variant="primary" type="submit" data-testid="themes-tab-save">
            {t("save")}
          </Button>
          <Button variant="link" onClick={setupForm}>
            {t("revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
