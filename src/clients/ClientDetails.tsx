import React, { useEffect, useState } from "react";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Spinner,
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
import { ClientScopes } from "./scopes/ClientScopes";
import { EvaluateScopes } from "./scopes/EvaluateScopes";
import { ServiceAccount } from "./service-account/ServiceAccount";

type ClientDetailHeaderProps = {
  onChange: (...event: any[]) => void;
  value: any;
  save: () => void;
  client: ClientRepresentation;
  toggleDownloadDialog: () => void;
  toggleDeleteDialog: () => void;
};

const ClientDetailHeader = ({
  onChange,
  value,
  save,
  client,
  toggleDownloadDialog,
  toggleDeleteDialog,
}: ClientDetailHeaderProps) => {
  const { t } = useTranslation("clients");
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
        titleKey={client ? client.clientId! : ""}
        subKey="clients:clientsExplain"
        dropdownItems={[
          <DropdownItem key="download" onClick={() => toggleDownloadDialog()}>
            {t("downloadAdapterConfig")}
          </DropdownItem>,
          <DropdownItem key="export" onClick={() => exportClient(client)}>
            {t("common:export")}
          </DropdownItem>,
          <DropdownItem key="delete" onClick={() => toggleDeleteDialog()}>
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
};

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
  const [activeTab2, setActiveTab2] = useState(30);
  const [client, setClient] = useState<ClientRepresentation>();

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
        setClient(fetchedClient);
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
        setClient(client);
        addAlert(t("clientSaveSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(`${t("clientSaveError")} '${error}'`, AlertVariant.danger);
      }
    }
  };

  if (!client) {
    return (
      <div className="pf-u-text-align-center">
        <Spinner />
      </div>
    );
  }
  return (
    <>
      <DeleteConfirm />
      <DownloadDialog />
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ onChange, value }) => (
          <ClientDetailHeader
            value={value}
            onChange={onChange}
            client={client}
            save={save}
            toggleDeleteDialog={toggleDeleteDialog}
            toggleDownloadDialog={toggleDownloadDialog}
          />
        )}
      />
      <PageSection variant="light">
        <Tabs
          activeKey={activeTab}
          onSelect={(_, key) => setActiveTab(key as number)}
          isBox
        >
          <Tab
            eventKey={0}
            title={<TabTitleText>{t("common:settings")}</TabTitleText>}
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
          <Tab
            eventKey={2}
            title={<TabTitleText>{t("clientScopes")}</TabTitleText>}
          >
            <Tabs
              activeKey={activeTab2}
              isSecondary
              onSelect={(_, key) => setActiveTab2(key as number)}
            >
              <Tab
                eventKey={30}
                title={<TabTitleText>{t("setup")}</TabTitleText>}
              >
                <ClientScopes clientId={id} protocol={client!.protocol!} />
              </Tab>
              <Tab
                eventKey={31}
                title={<TabTitleText>{t("evaluate")}</TabTitleText>}
              >
                <EvaluateScopes />
              </Tab>
            </Tabs>
          </Tab>
          {client && client.serviceAccountsEnabled && (
            <Tab
              eventKey={3}
              title={<TabTitleText>{t("serviceAccount")}</TabTitleText>}
            >
              <ServiceAccount clientId={id} />
            </Tab>
          )}
        </Tabs>
      </PageSection>
    </>
  );
};
