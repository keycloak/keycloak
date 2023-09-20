import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Card,
  CardBody,
  Form,
  FormGroup,
  PageSection,
  Switch,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { Fragment, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { FormPanel } from "../../components/scroll-form/FormPanel";
import { convertAttributeNameToForm } from "../../util";
import { useFetch } from "../../utils/useFetch";
import useToggle from "../../utils/useToggle";
import { FormFields } from "../ClientDetails";
import { Certificate } from "./Certificate";
import { ExportSamlKeyDialog } from "./ExportSamlKeyDialog";
import { SamlImportKeyDialog } from "./SamlImportKeyDialog";
import { SamlKeysDialog } from "./SamlKeysDialog";

type SamlKeysProps = {
  clientId: string;
  save: () => void;
};

const KEYS = ["saml.signing", "saml.encryption"] as const;
export type KeyTypes = (typeof KEYS)[number];

const KEYS_MAPPING: { [key in KeyTypes]: { [index: string]: string } } = {
  "saml.signing": {
    name: convertAttributeNameToForm("attributes.saml.client.signature"),
    title: "signingKeysConfig",
    key: "clientSignature",
  },
  "saml.encryption": {
    name: convertAttributeNameToForm("attributes.saml.encrypt"),
    title: "encryptionKeysConfig",
    key: "encryptAssertions",
  },
};

type KeySectionProps = {
  clientId: string;
  keyInfo?: CertificateRepresentation;
  attr: KeyTypes;
  onChanged: (key: KeyTypes) => void;
  onGenerate: (key: KeyTypes, regenerate: boolean) => void;
  onImport: (key: KeyTypes) => void;
};

const KeySection = ({
  clientId,
  keyInfo,
  attr,
  onChanged,
  onGenerate,
  onImport,
}: KeySectionProps) => {
  const { t } = useTranslation("clients");
  const { control, watch } = useFormContext<FormFields>();
  const title = KEYS_MAPPING[attr].title;
  const key = KEYS_MAPPING[attr].key;
  const name = KEYS_MAPPING[attr].name;

  const [showImportDialog, toggleImportDialog] = useToggle();

  const section = watch(name as keyof FormFields);
  return (
    <>
      {showImportDialog && (
        <ExportSamlKeyDialog
          keyType={attr}
          clientId={clientId}
          close={toggleImportDialog}
        />
      )}
      <FormPanel title={t(title)} className="kc-form-panel__panel">
        <TextContent className="pf-u-pb-lg">
          <Text>{t(`${title}Explain`)}</Text>
        </TextContent>
        <FormAccess role="manage-clients" isHorizontal>
          <FormGroup
            labelIcon={
              <HelpItem
                helpText={t(`clients-help:${key}`)}
                fieldLabelId={`clients:${key}`}
              />
            }
            label={t(key)}
            fieldId={key}
            hasNoPaddingTop
          >
            <Controller
              name={name as keyof FormFields}
              control={control}
              defaultValue="false"
              render={({ field }) => (
                <Switch
                  data-testid={key}
                  id={key}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => {
                    const v = value.toString();
                    if (v === "true") {
                      onChanged(attr);
                      field.onChange(v);
                    } else {
                      onGenerate(attr, false);
                    }
                  }}
                  aria-label={t(key)}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      {keyInfo?.certificate && section === "true" && (
        <Card isFlat>
          <CardBody className="kc-form-panel__body">
            <Form isHorizontal>
              <Certificate keyInfo={keyInfo} />
              <ActionGroup>
                <Button
                  variant="secondary"
                  onClick={() => onGenerate(attr, true)}
                >
                  {t("regenerate")}
                </Button>
                <Button variant="secondary" onClick={() => onImport(attr)}>
                  {t("importKey")}
                </Button>
                <Button variant="tertiary" onClick={toggleImportDialog}>
                  {t("common:export")}
                </Button>
              </ActionGroup>
            </Form>
          </CardBody>
        </Card>
      )}
    </>
  );
};

export const SamlKeys = ({ clientId, save }: SamlKeysProps) => {
  const { t } = useTranslation("clients");
  const [isChanged, setIsChanged] = useState<KeyTypes>();
  const [keyInfo, setKeyInfo] = useState<CertificateRepresentation[]>();
  const [selectedType, setSelectedType] = useState<KeyTypes>();
  const [openImport, setImportOpen] = useState<KeyTypes>();
  const [refresh, setRefresh] = useState(0);

  const { setValue } = useFormContext();
  const { addAlert, addError } = useAlerts();

  useFetch(
    () =>
      Promise.all(
        KEYS.map((attr) =>
          adminClient.clients.getKeyInfo({ id: clientId, attr }),
        ),
      ),
    (info) => setKeyInfo(info),
    [refresh],
  );

  const generate = async (attr: KeyTypes) => {
    const index = KEYS.indexOf(attr);
    try {
      const info = [...(keyInfo || [])];
      info[index] = await adminClient.clients.generateKey({
        id: clientId,
        attr,
      });

      setKeyInfo(info);
      saveAs(
        new Blob([info[index].privateKey!], {
          type: "application/octet-stream",
        }),
        "private.key",
      );

      addAlert(t("generateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clients:generateError", error);
    }
  };

  const key = selectedType ? KEYS_MAPPING[selectedType].key : "";
  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: t("disableSigning", {
      key: t(key),
    }),
    messageKey: t("disableSigningExplain", {
      key: t(key),
    }),
    continueButtonLabel: "common:yes",
    cancelButtonLabel: "common:no",
    onConfirm: () => {
      setValue(KEYS_MAPPING[selectedType!].name, "false");
      save();
    },
  });

  const [toggleReGenerateDialog, ReGenerateConfirm] = useConfirmDialog({
    titleKey: "clients:reGenerateSigning",
    messageKey: "clients:reGenerateSigningExplain",
    continueButtonLabel: "common:yes",
    cancelButtonLabel: "common:no",
    onConfirm: () => {
      generate(selectedType!);
    },
  });

  return (
    <PageSection variant="light" className="keycloak__form">
      {isChanged && (
        <SamlKeysDialog
          id={clientId}
          attr={isChanged}
          onClose={() => {
            setIsChanged(undefined);
            save();
            setRefresh(refresh + 1);
          }}
          onCancel={() => {
            setValue(KEYS_MAPPING[selectedType!].name, "false");
            setIsChanged(undefined);
          }}
        />
      )}
      <DisableConfirm />
      <ReGenerateConfirm />
      {KEYS.map((attr, index) => (
        <Fragment key={attr}>
          {openImport === attr && (
            <SamlImportKeyDialog
              id={clientId}
              attr={attr}
              onClose={() => setImportOpen(undefined)}
            />
          )}
          <KeySection
            clientId={clientId}
            keyInfo={keyInfo?.[index]}
            attr={attr}
            onChanged={setIsChanged}
            onGenerate={(type, isNew) => {
              setSelectedType(type);
              if (!isNew) {
                toggleDisableDialog();
              } else {
                toggleReGenerateDialog();
              }
            }}
            onImport={() => setImportOpen(attr)}
          />
        </Fragment>
      ))}
    </PageSection>
  );
};
