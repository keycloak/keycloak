import React, { useState, useEffect, ReactElement, useContext } from "react";
import {
  Alert,
  AlertVariant,
  Form,
  FormGroup,
  ModalVariant,
  Select,
  SelectOption,
  SelectVariant,
  Stack,
  StackItem,
  TextArea,
} from "@patternfly/react-core";
import FileSaver from "file-saver";

import { ConfirmDialogModal } from "../confirm-dialog/ConfirmDialog";
import { HelpItem } from "../help-enabler/HelpItem";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { useAdminClient } from "../../context/auth/AdminClient";
import { HelpContext } from "../help-enabler/HelpHeader";

export type DownloadDialogProps = {
  id: string;
  protocol?: string;
};

type DownloadDialogModalProps = DownloadDialogProps & {
  open: boolean;
  toggleDialog: () => void;
};

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
  const adminClient = useAdminClient();
  const { t } = useTranslation("common");
  const { enabled } = useContext(HelpContext);
  const serverInfo = useServerInfo();

  const configFormats = serverInfo.clientInstallations![protocol];
  const [selected, setSelected] = useState(
    configFormats[configFormats.length - 1].id
  );
  const [snippet, setSnippet] = useState("");
  const [openType, setOpenType] = useState(false);

  useEffect(() => {
    let isMounted = true;
    (async () => {
      const snippet = await adminClient.clients.getInstallationProviders({
        id,
        providerId: selected,
      });
      if (isMounted) {
        if (typeof snippet === "string") {
          setSnippet(snippet);
        } else {
          setSnippet(JSON.stringify(snippet, undefined, 3));
        }
      }
    })();
    return () => {
      isMounted = false;
    };
  }, [selected]);
  return (
    <ConfirmDialogModal
      titleKey={t("clients:downloadAdaptorTitle")}
      continueButtonLabel={t("download")}
      onConfirm={() => {
        const config = configFormats.find((config) => config.id === selected)!;
        FileSaver.saveAs(
          new Blob([snippet], { type: config.mediaType }),
          config.filename
        );
      }}
      open={open}
      toggleDialog={toggleDialog}
      variant={ModalVariant.medium}
    >
      <Form>
        <Stack hasGutter>
          {enabled && (
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
          )}
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
                  forID="details"
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
