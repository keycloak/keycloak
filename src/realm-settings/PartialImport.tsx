import React, { useState, useEffect } from "react";
import {
  Button,
  ButtonVariant,
  Divider,
  Modal,
  ModalVariant,
  Stack,
  StackItem,
  Text,
  TextContent,
} from "@patternfly/react-core";

import { useTranslation } from "react-i18next";

import {
  JsonFileUpload,
  JsonFileUploadEvent,
} from "../components/json-file-upload/JsonFileUpload";

export type PartialImportProps = {
  open: boolean;
  toggleDialog: () => void;
};

export const PartialImportDialog = (props: PartialImportProps) => {
  const tRealm = useTranslation("realm-settings").t;
  const { t } = useTranslation("partial-import");
  const [importEnabled, setImportEnabled] = useState(false);

  // when dialog opens or closes, reset importEnabled to false
  useEffect(() => {
    setImportEnabled(false);
  }, [props.open]);

  const handleFileChange = (
    value: string | File,
    filename: string,
    event: JsonFileUploadEvent
  ) => {
    setImportEnabled(value !== null);

    // if user pressed clear button reset importEnabled
    const nativeEvent = event.nativeEvent;
    if (
      nativeEvent instanceof MouseEvent &&
      !(nativeEvent instanceof DragEvent)
    ) {
      setImportEnabled(false);
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={tRealm("partialImport")}
      isOpen={props.open}
      onClose={props.toggleDialog}
      actions={[
        <Button
          id="modal-import"
          data-testid="import-button"
          key="import"
          isDisabled={!importEnabled}
          onClick={() => {
            props.toggleDialog();
          }}
        >
          {t("import")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel-button"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            props.toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Stack hasGutter>
        <StackItem>
          <TextContent>
            <Text>{t("partialImportHeaderText")}</Text>
          </TextContent>
        </StackItem>
        <StackItem>
          <JsonFileUpload
            id="partial-import-file"
            onChange={handleFileChange}
          />
        </StackItem>
        {importEnabled && (
          <StackItem>
            <Divider />
            TODO: This section will include{" "}
            <strong>Choose the resources...</strong> and{" "}
            <strong>If a resource already exists....</strong>
            <Divider />
          </StackItem>
        )}
      </Stack>
    </Modal>
  );
};
