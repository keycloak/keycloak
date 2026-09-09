import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { fetchWithError } from "@keycloak/keycloak-admin-client";
import { SelectControl, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  Label,
  LabelGroup,
  PageSection,
  Split,
  SplitItem,
  Title,
} from "@patternfly/react-core";
import { TrashIcon } from "@patternfly/react-icons";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import {
  useRefreshServerInfo,
  useServerInfo,
} from "../../context/server-info/ServerInfoProvider";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { joinPath } from "../../utils/joinPath";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertToFormValues } from "../../util";

type ThemeSettingsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

type CustomThemeInfo = {
  name: string;
  types: string[];
};

export const ThemeSettingsTab = ({ realm, save }: ThemeSettingsTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();
  const refreshServerInfo = useRefreshServerInfo();
  const uploadRef = useRef<HTMLInputElement>(null);
  const [customThemes, setCustomThemes] = useState<CustomThemeInfo[]>([]);
  const [themeToDelete, setThemeToDelete] = useState("");

  const form = useForm<RealmRepresentation>();
  const { handleSubmit, setValue } = form;
  const themeTypes = useServerInfo().themes!;

  const setupForm = () => {
    convertToFormValues(realm, setValue);
  };

  const loadCustomThemes = async () => {
    try {
      const response = await fetchWithError(
        joinPath(adminClient.baseUrl, `admin/realms/${realmName}/themes`),
        {
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
          },
        },
      );
      setCustomThemes(await response.json());
    } catch {
      setCustomThemes([]);
    }
  };

  useEffect(() => {
    setupForm();
    void loadCustomThemes();
  }, []);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteThemeConfirmTitle",
    children: t("deleteThemeConfirmMessage", { name: themeToDelete }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await fetchWithError(
          joinPath(
            adminClient.baseUrl,
            `admin/realms/${realmName}/themes/${themeToDelete}`,
          ),
          {
            method: "DELETE",
            headers: {
              ...getAuthorizationHeaders(await adminClient.getAccessToken()),
            },
          },
        );
        addAlert(t("deleteThemeSuccess"), AlertVariant.success);
        refreshServerInfo();
        void loadCustomThemes();
      } catch (error) {
        addError("deleteThemeError", error);
      }
    },
  });

  const handleThemeUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      await fetchWithError(
        joinPath(adminClient.baseUrl, `admin/realms/${realmName}/themes`),
        {
          method: "POST",
          body: formData,
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
          },
        },
      );
      addAlert(t("uploadThemeSuccess"), AlertVariant.success);
      refreshServerInfo();
      void loadCustomThemes();
    } catch (error) {
      addError("uploadThemeError", error);
    }

    event.target.value = "";
  };

  const appendEmptyChoice = (items: { key: string; value: string }[]) => [
    { key: "", value: t("choose") },
    ...items,
  ];

  return (
    <PageSection variant="light">
      <DeleteConfirm />
      <input
        ref={uploadRef}
        type="file"
        accept=".zip"
        style={{ display: "none" }}
        onChange={handleThemeUpload}
      />
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
                description: theme.description,
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
                description: theme.description,
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
                description: theme.description,
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
                description: theme.description,
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
          <Button
            variant="secondary"
            onClick={() => uploadRef.current?.click()}
            data-testid="themes-tab-upload"
          >
            {t("uploadTheme")}
          </Button>
        </ActionGroup>
      </FormAccess>

      {customThemes.length > 0 && (
        <>
          <Divider className="pf-v5-u-mt-lg pf-v5-u-mb-md" />
          <Title headingLevel="h4" size="md" className="pf-v5-u-mb-md">
            {t("customThemes")}
          </Title>
          {customThemes.map((theme) => (
            <Split
              key={theme.name}
              hasGutter
              className="pf-v5-u-mb-sm pf-v5-u-align-items-center"
            >
              <SplitItem isFilled>
                <strong>{theme.name}</strong>
                <LabelGroup className="pf-v5-u-ml-sm" numLabels={6}>
                  {theme.types.map((type) => (
                    <Label key={type} color="blue" isCompact>
                      {type}
                    </Label>
                  ))}
                </LabelGroup>
              </SplitItem>
              <SplitItem>
                <Button
                  variant="plain"
                  aria-label={t("deleteTheme")}
                  onClick={() => {
                    setThemeToDelete(theme.name);
                    toggleDeleteDialog();
                  }}
                >
                  <TrashIcon />
                </Button>
              </SplitItem>
            </Split>
          ))}
        </>
      )}
    </PageSection>
  );
};
