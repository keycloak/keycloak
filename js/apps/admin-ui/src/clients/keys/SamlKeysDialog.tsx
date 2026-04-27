import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
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
import { saveAs } from "file-saver";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { Certificate } from "./Certificate";
import { KeyForm } from "./GenerateKeyDialog";
import type { KeyTypes } from "./SamlKeys";

type SamlKeysDialogProps = {
  id: string;
  attr: KeyTypes;
  localeKey: string;
  onClose: () => void;
  onCancel: () => void;
};

export type SamlKeysDialogForm = KeyStoreConfig & {
  file: File;
};

export const submitForm = async (
  adminClient: KeycloakAdminClient,
  form: SamlKeysDialogForm,
  id: string,
  attr: KeyTypes,
  callback: (error?: unknown) => void,
) => {
  try {
    const formData = new FormData();
    const { file, ...rest } = form;
    Object.entries(rest).map(([key, value]) =>
      formData.append(
        key === "format" ? "keystoreFormat" : key,
        value.toString(),
      ),
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
  localeKey,
  onClose,
  onCancel,
}: SamlKeysDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const [type, setType] = useState(false);
  const [keys, setKeys] = useState<CertificateRepresentation>();
  const form = useForm<SamlKeysDialogForm>({ mode: "onChange" });
  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  const { addAlert, addError } = useAlerts();

  const submit = async (form: SamlKeysDialogForm) => {
    await submitForm(adminClient, form, id, attr, (error) => {
      if (error) {
        addError("importError", error);
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
        "private.key",
      );

      addAlert(t("generateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("generateError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      aria-label={t("enableClientSignatureRequiredModal")}
      header={
        <TextContent>
          <Title headingLevel="h1">
            {t("enableClientSignatureRequired", {
              key: t(localeKey),
            })}
          </Title>
          <Text>
            {t("enableClientSignatureRequiredExplain", {
              key: t(localeKey),
            })}
          </Text>
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
          onClick={async () => {
            if (type) {
              await handleSubmit(submit)();
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
          {t("cancel")}
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
                  helpText={t(`saml${localeKey}CertificateHelp`)}
                  fieldLabelId="certificate"
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
        {type && <KeyForm useFile hasPem />}
      </FormProvider>
    </Modal>
  );
};
