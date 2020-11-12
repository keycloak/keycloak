import React, { useEffect, useState } from "react";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useParams } from "react-router-dom";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";

import { ClientSettings } from "./ClientSettings";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useDownloadDialog } from "../components/download-dialog/DownloadDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { Credentials } from "./credentials/Credentials";
import {
  convertFormValuesToObject,
  convertToFormValues,
  exportClient,
} from "../util";
import {
  convertToMultiline,
  toValue,
} from "../components/multi-line-input/MultiLineInput";

export const ClientDetails = () => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const form = useForm();
  const publicClient = useWatch({
    control: form.control,
    name: "publicClient",
    defaultValue: false,
  });
  const { id } = useParams<{ id: string }>();

  const [activeTab, setActiveTab] = useState(0);
  const [name, setName] = useState("");

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:clientDeleteConfirmTitle",
    messageKey: "clients:clientDeleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clients.del({ id });
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

  const setupForm = (client: ClientRepresentation) => {
    form.reset(client);
    Object.entries(client).map((entry) => {
      if (entry[0] === "redirectUris") {
        form.setValue(entry[0], convertToMultiline(entry[1]));
      } else if (entry[0] === "attributes") {
        convertToFormValues(entry[1], "attributes", form.setValue);
      } else {
        form.setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedClient = await adminClient.clients.findOne({ id });
      if (fetchedClient) {
        setName(fetchedClient.clientId!);
        setupForm(fetchedClient);
      }
    })();
  }, []);

  const save = async () => {
    if (await form.trigger()) {
      const redirectUris = toValue(form.getValues()["redirectUris"]);
      const attributes = form.getValues()["attributes"]
        ? convertFormValuesToObject(form.getValues()["attributes"])
        : {};

      try {
        const client = {
          ...form.getValues(),
          redirectUris,
          attributes,
        };
        await adminClient.clients.update({ id }, client);
        setupForm(client as ClientRepresentation);
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
        <Tabs
          activeKey={activeTab}
          onSelect={(_, key) => setActiveTab(key as number)}
          isBox
        >
          <Tab
            eventKey={0}
            title={<TabTitleText>{t("settings")}</TabTitleText>}
          >
            <ClientSettings form={form} save={save} />
          </Tab>
          {publicClient && (
            <Tab
              eventKey={1}
              title={<TabTitleText>{t("credentials")}</TabTitleText>}
            >
              <Credentials clientId={id} form={form} save={save} />
            </Tab>
          )}
        </Tabs>
      </PageSection>
    </>
  );
};
