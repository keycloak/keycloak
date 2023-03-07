import { useState } from "react";
import { saveAs } from "file-saver";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Radio,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";

import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import type { KeyTypes } from "./SamlKeys";
import { HelpItem } from "ui-shared";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { KeyForm } from "./GenerateKeyDialog";
import { Certificate } from "./Certificate";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";

type SamlKeysDialogProps = {
  id: string;
  attr: KeyTypes;
  onClose: () => void;
  onCancel: () => void;
};

export type SamlKeysDialogForm = KeyStoreConfig & {
  file: File;
};

export const submitForm = async (
  form: SamlKeysDialogForm,
  id: string,
  attr: KeyTypes,
  adminClient: KeycloakAdminClient,
  callback: (error?: unknown) => void
) => {
  try {
    const formData = new FormData();
    const { file, ...rest } = form;
    Object.entries(rest).map(([key, value]) =>
      formData.append(
        key === "format" ? "keystoreFormat" : key,
        value.toString()
      )
    );
    formData.append("file", file);

    await adminClient.clients.uploadKey({ id, attr }, formData);
    callback();
  } catch (error) {
    callback(error);
  }
};

export const SamlKeysDialog = ({
  id,
  attr,
  onClose,
  onCancel,
}: SamlKeysDialogProps) => {
  const { t } = useTranslation("clients");
  const [type, setType] = useState(false);
  const [keys, setKeys] = useState<CertificateRepresentation>();
  const form = useForm<SamlKeysDialogForm>({ mode: "onChange" });
  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const submit = (form: SamlKeysDialogForm) => {
    submitForm(form, id, attr, adminClient, (error) => {
      if (error) {
        addError("clients:importError", error);
      } else {
        addAlert(t("importSuccess"), AlertVariant.success);
      }
    });
  };

  const generate = async () => {
    try {
      const key = await adminClient.clients.generateKey({
        id,
        attr,
      });
      setKeys(key);
      saveAs(
        new Blob([key.privateKey!], {
          type: "application/octet-stream",
        }),
        "private.key"
      );

      addAlert(t("generateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clients:generateError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      aria-labelledby={t("enableClientSignatureRequired")}
      header={
        <TextContent>
          <Title headingLevel="h1">{t("enableClientSignatureRequired")}</Title>
          <Text>{t("enableClientSignatureRequiredExplain")}</Text>
        </TextContent>
      }
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          data-testid="confirm"
          variant="primary"
          isDisabled={!isValid && !keys}
          onClick={() => {
            if (type) {
              handleSubmit(submit)();
            }
            onClose();
          }}
        >
          {t("confirm")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={onCancel}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form isHorizontal>
          <FormGroup
            label={t("selectMethod")}
            fieldId="selectMethod"
            hasNoPaddingTop
          >
            <Flex>
              <FlexItem>
                <Radio
                  isChecked={!type}
                  name="selectMethodType"
                  onChange={() => setType(false)}
                  label={t("selectMethodType.generate")}
                  id="selectMethodType-generate"
                />
              </FlexItem>
              <FlexItem>
                <Radio
                  isChecked={type}
                  name="selectMethodType"
                  onChange={() => setType(true)}
                  label={t("selectMethodType.import")}
                  id="selectMethodType-import"
                />
              </FlexItem>
            </Flex>
          </FormGroup>
          {!type && (
            <FormGroup
              label={t("certificate")}
              fieldId="certificate"
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:certificate")}
                  fieldLabelId="clients:certificate"
                />
              }
            >
              <Split hasGutter>
                <SplitItem isFilled>
                  <Certificate plain keyInfo={keys} />
                </SplitItem>
                <SplitItem>
                  <Button
                    variant="secondary"
                    data-testid="generate"
                    onClick={generate}
                  >
                    {t("generate")}
                  </Button>
                </SplitItem>
              </Split>
            </FormGroup>
          )}
        </Form>
        {type && <KeyForm useFile />}
      </FormProvider>
    </Modal>
  );
};
