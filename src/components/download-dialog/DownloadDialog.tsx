import React, { useContext, useState, useEffect, ReactElement } from "react";
import {
  Alert,
  AlertVariant,
  Form,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Stack,
  StackItem,
  TextArea,
} from "@patternfly/react-core";
import { ConfirmDialogModal } from "../confirm-dialog/ConfirmDialog";
import { HttpClientContext } from "../../context/http-service/HttpClientContext";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { HelpItem } from "../help-enabler/HelpItem";
import { useTranslation } from "react-i18next";

export type DownloadDialogProps = {
  id: string;
  protocol?: string;
};

type DownloadDialogModalProps = DownloadDialogProps & {
  open: boolean;
  toggleDialog: () => void;
};

const serverInfo = [
  {
    id: "keycloak-oidc-jboss-subsystem-cli",
    protocol: "openid-connect",
    downloadOnly: false,
    displayType: "Keycloak OIDC JBoss Subsystem CLI",
    helpText:
      "CLI script you must edit and apply to your client app server. This type of configuration is useful when you can't or don't want to crack open your WAR file.",
    filename: "keycloak-oidc-subsystem.cli",
    mediaType: "text/plain",
  },
  {
    id: "keycloak-oidc-jboss-subsystem",
    protocol: "openid-connect",
    downloadOnly: false,
    displayType: "Keycloak OIDC JBoss Subsystem XML",
    helpText:
      "XML snippet you must edit and add to the Keycloak OIDC subsystem on your client app server.  This type of configuration is useful when you can't or don't want to crack open your WAR file.",
    filename: "keycloak-oidc-subsystem.xml",
    mediaType: "application/xml",
  },
  {
    id: "keycloak-oidc-keycloak-json",
    protocol: "openid-connect",
    downloadOnly: false,
    displayType: "Keycloak OIDC JSON",
    helpText:
      "keycloak.json file used by the Keycloak OIDC client adapter to configure clients.  This must be saved to a keycloak.json file and put in your WEB-INF directory of your WAR file.  You may also want to tweak this file after you download it.",
    filename: "keycloak.json",
    mediaType: "application/json",
  },
];

export const useDownloadDialog = (
  props: DownloadDialogProps
): [() => void, () => ReactElement] => {
  const [show, setShow] = useState(false);

  function toggleDialog() {
    setShow((show) => !show);
  }

  const Dialog = () => (
    <DownloadDialog {...props} open={show} toggleDialog={toggleDialog} />
  );
  return [toggleDialog, Dialog];
};

export const DownloadDialog = ({
  id,
  open,
  toggleDialog,
  protocol = "openid-connect",
}: DownloadDialogModalProps) => {
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const { t } = useTranslation("common");

  const configFormats = serverInfo; //serverInfo.clientInstallations[protocol];
  const [selected, setSelected] = useState(
    configFormats[configFormats.length - 1].id
  );
  const [snippet, setSnippet] = useState("");
  const [openType, setOpenType] = useState(false);

  useEffect(() => {
    (async () => {
      const response = await httpClient.doGet<string>(
        `admin/${realm}/master/clients/${id}/installation/providers/${selected}`
      );
      setSnippet(response.data!);
    })();
  }, [selected]);
  return (
    <ConfirmDialogModal
      titleKey={t("clients:downloadAdaptorTitle")}
      continueButtonLabel={t("download")}
      onConfirm={() => {}}
      open={open}
      toggleDialog={toggleDialog}
    >
      <Form>
        <Stack hasGutter>
          <StackItem>
            <Alert
              id={id}
              title={t("clients:description")}
              variant={AlertVariant.info}
              isInline
            >
              {
                configFormats.find(
                  (configFormat) => configFormat.id === selected
                )?.helpText
              }
            </Alert>
          </StackItem>
          <StackItem>
            <FormGroup
              fieldId="type"
              label={t("clients:formatOption")}
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:downloadType")}
                  forLabel={t("clients:formatOption")}
                  forID="type"
                />
              }
            >
              <Select
                toggleId="type"
                isOpen={openType}
                onToggle={() => {
                  setOpenType(!openType);
                }}
                variant={SelectVariant.single}
                value={selected}
                selections={selected}
                onSelect={(_, value) => {
                  setSelected(value as string);
                  setOpenType(false);
                }}
                aria-label="Select Input"
              >
                {configFormats.map((configFormat) => (
                  <SelectOption
                    key={configFormat.id}
                    value={configFormat.id}
                    isSelected={selected === configFormat.id}
                  >
                    {configFormat.displayType}
                  </SelectOption>
                ))}
              </Select>
            </FormGroup>
          </StackItem>
          <StackItem isFilled>
            <FormGroup
              fieldId="details"
              label={t("clients:details")}
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:details")}
                  forLabel={t("clients:details")}
                  forID=""
                />
              }
            >
              <TextArea
                id="details"
                readOnly
                rows={12}
                resizeOrientation="vertical"
                value={snippet}
                aria-label="text area example"
              />
            </FormGroup>
          </StackItem>
        </Stack>
      </Form>
    </ConfirmDialogModal>
  );
};
