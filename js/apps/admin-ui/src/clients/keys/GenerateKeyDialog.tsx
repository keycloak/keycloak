import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import {
  HelpItem,
  NumberControl,
  SelectControl,
  FileUploadControl,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { FormProvider, useForm, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { StoreSettings } from "./StoreSettings";

type GenerateKeyDialogProps = {
  clientId: string;
  toggleDialog: () => void;
  save: (keyStoreConfig: KeyStoreConfig) => void;
};

type KeyFormProps = {
  useFile?: boolean;
  isSaml?: boolean;
  hasPem?: boolean;
};

const CERT_PEM = "Certificate PEM" as const;

const extensions = new Map([
  ["PKCS12", "p12"],
  ["JKS", "jks"],
  ["BCFKS", "bcfks"],
]);

type FormFields = KeyStoreConfig & {
  file: string | File;
};

export const getFileExtension = (format: string) => extensions.get(format);

export const KeyForm = ({
  isSaml = false,
  hasPem = false,
  useFile = false,
}: KeyFormProps) => {
  const { t } = useTranslation();

  const { watch } = useFormContext<FormFields>();
  const format = watch("format");

  const { cryptoInfo } = useServerInfo();
  const supportedKeystoreTypes = [
    ...(cryptoInfo?.supportedKeystoreTypes ?? []),
    ...(hasPem ? [CERT_PEM] : []),
  ];
  const keySizes = ["4096", "3072", "2048"];

  return (
    <Form className="pf-v5-u-pt-lg">
      <SelectControl
        name="format"
        label={t("archiveFormat")}
        labelIcon={t("archiveFormatHelp")}
        controller={{
          defaultValue: supportedKeystoreTypes[0],
        }}
        menuAppendTo="parent"
        options={supportedKeystoreTypes}
      />
      {useFile && (
        <FileUploadControl
          label={t("importFile")}
          labelIcon={
            <HelpItem
              helpText={t("importFileHelp")}
              fieldLabelId="importFile"
            />
          }
          rules={{
            required: t("required"),
          }}
          name="file"
          id="importFile"
        />
      )}
      {format !== CERT_PEM && (
        <StoreSettings hidePassword={useFile} isSaml={isSaml} />
      )}
      {!useFile && (
        <>
          <SelectControl
            name="keySize"
            label={t("keySize")}
            labelIcon={t("keySizeHelp")}
            controller={{
              defaultValue: keySizes[0],
            }}
            menuAppendTo="parent"
            options={keySizes}
          />
          <NumberControl
            name="validity"
            label={t("validity")}
            labelIcon={t("validityHelp")}
            controller={{
              defaultValue: 3,
              rules: { required: t("required"), min: 1, max: 10 },
            }}
          />
        </>
      )}
    </Form>
  );
};

export const GenerateKeyDialog = ({
  clientId,
  save,
  toggleDialog,
}: GenerateKeyDialogProps) => {
  const { t } = useTranslation();
  const form = useForm<KeyStoreConfig>({
    defaultValues: { keyAlias: clientId },
    mode: "onChange",
  });

  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("generateKeys")}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          data-testid="confirm"
          isDisabled={!isValid}
          onClick={async () => {
            await handleSubmit((config) => {
              save(config);
              toggleDialog();
            })();
          }}
        >
          {t("generate")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            toggleDialog();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <TextContent>
        <Text>{t("generateKeysDescription")}</Text>
      </TextContent>
      <FormProvider {...form}>
        <KeyForm />
      </FormProvider>
    </Modal>
  );
};
