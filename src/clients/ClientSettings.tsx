import React, { useContext, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  TextInput,
  Form,
  Switch,
  TextArea,
  PageSection,
  ActionGroup,
  Button,
  AlertVariant,
  ButtonVariant,
  DropdownItem,
} from "@patternfly/react-core";
import { useParams } from "react-router-dom";
import { Controller, useForm } from "react-hook-form";

import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { ClientDescription } from "./ClientDescription";
import { CapabilityConfig } from "./add/CapabilityConfig";
import { RealmContext } from "../context/realm-context/RealmContext";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { ClientRepresentation } from "../realm/models/Realm";
import {
  convertToMultiline,
  MultiLineInput,
  toValue,
} from "../components/multi-line-input/MultiLineInput";
import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { exportClient } from "../util";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useDownloadDialog } from "../components/download-dialog/DownloadDialog";

export const ClientSettings = () => {
  const { t } = useTranslation("clients");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const { addAlert } = useAlerts();

  const { id } = useParams<{ id: string }>();
  const [name, setName] = useState("");
  const form = useForm();
  const url = `/admin/realms/${realm}/clients/${id}`;

  useEffect(() => {
    (async () => {
      const fetchedClient = await httpClient.doGet<ClientRepresentation>(url);
      if (fetchedClient.data) {
        setName(fetchedClient.data.clientId);
        Object.entries(fetchedClient.data).map((entry) => {
          if (entry[0] !== "redirectUris") {
            form.setValue(entry[0], entry[1]);
          } else if (entry[1] && entry[1].length > 0) {
            form.setValue(entry[0], convertToMultiline(entry[1]));
          }
        });
      }
    })();
  }, []);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:clientDeleteConfirmTitle",
    messageKey: "clients:clientDeleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: () => {
      try {
        httpClient.doDelete(`/admin/realms/${realm}/clients/${id}`);
        addAlert(t("clientDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(`${t("clientDeleteError")} ${error}`, AlertVariant.danger);
      }
    },
  });

  const [toggleDownloadDialog, DownloadDialog] = useDownloadDialog({
    id,
    protocol: form.getValues("protocol"),
  });

  const save = async () => {
    if (await form.trigger()) {
      const redirectUris = toValue(form.getValues()["redirectUris"]);
      try {
        httpClient.doPut(url, { ...form.getValues(), redirectUris });
        addAlert(t("clientSaveSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(`${t("clientSaveError")} '${error}'`, AlertVariant.danger);
      }
    }
  };

  return (
    <>
      <DeleteConfirm />
      <DownloadDialog />
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ onChange, value }) => {
          const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
            titleKey: "clients:disableConfirmTitle",
            messageKey: "clients:disableConfirm",
            continueButtonLabel: "common:disable",
            onConfirm: () => {
              onChange(!value);
              save();
            },
          });
          return (
            <>
              <DisableConfirm />
              <ViewHeader
                titleKey={name}
                subKey="clients:clientsExplain"
                dropdownItems={[
                  <DropdownItem
                    key="download"
                    onClick={() => toggleDownloadDialog()}
                  >
                    {t("downloadAdapterConfig")}
                  </DropdownItem>,
                  <DropdownItem
                    key="export"
                    onClick={() => exportClient(form.getValues())}
                  >
                    {t("common:export")}
                  </DropdownItem>,
                  <DropdownItem
                    key="delete"
                    onClick={() => toggleDeleteDialog()}
                  >
                    {t("common:delete")}
                  </DropdownItem>,
                ]}
                isEnabled={value}
                onToggle={(value) => {
                  if (!value) {
                    toggleDisableDialog();
                  } else {
                    onChange(value);
                    save();
                  }
                }}
              />
            </>
          );
        }}
      />
      <PageSection variant="light">
        <ScrollForm
          sections={[
            t("capabilityConfig"),
            t("generalSettings"),
            t("accessSettings"),
            t("loginSettings"),
          ]}
        >
          <CapabilityConfig form={form} />
          <Form isHorizontal>
            <ClientDescription form={form} />
          </Form>
          <Form isHorizontal>
            <FormGroup label={t("rootUrl")} fieldId="kc-root-url">
              <TextInput
                type="text"
                id="kc-root-url"
                name="rootUrl"
                ref={form.register}
              />
            </FormGroup>
            <FormGroup label={t("validRedirectUri")} fieldId="kc-redirect">
              <MultiLineInput form={form} name="redirectUris" />
            </FormGroup>
            <FormGroup label={t("homeURL")} fieldId="kc-home-url">
              <TextInput
                type="text"
                id="kc-home-url"
                name="baseUrl"
                ref={form.register}
              />
            </FormGroup>
          </Form>
          <Form isHorizontal>
            <FormGroup
              label={t("consentRequired")}
              fieldId="kc-consent"
              hasNoPaddingTop
            >
              <Controller
                name="consentRequired"
                defaultValue={false}
                control={form.control}
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-consent"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("displayOnClient")}
              fieldId="kc-display-on-client"
              hasNoPaddingTop
            >
              <Controller
                name="alwaysDisplayInConsole"
                defaultValue={false}
                control={form.control}
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-display-on-client"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("consentScreenText")}
              fieldId="kc-consent-screen-text"
            >
              <TextArea
                id="kc-consent-screen-text"
                name="consentText"
                ref={form.register}
              />
            </FormGroup>
            <ActionGroup>
              <Button variant="primary" onClick={() => save()}>
                {t("common:save")}
              </Button>
              <Button variant="link">{t("common:cancel")}</Button>
            </ActionGroup>
          </Form>
        </ScrollForm>
      </PageSection>
    </>
  );
};
