import React, { Fragment, useState } from "react";
import FileSaver from "file-saver";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  CardBody,
  PageSection,
  TextContent,
  Text,
  FormGroup,
  Switch,
  Card,
  Form,
  ActionGroup,
  Button,
  AlertVariant,
} from "@patternfly/react-core";

import type CertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/certificateRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import type { ClientForm } from "../ClientDetails";
import { SamlKeysDialog } from "./SamlKeysDialog";
import { FormPanel } from "../../components/scroll-form/FormPanel";
import { Certificate } from "./Certificate";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../../components/alert/Alerts";
import { SamlImportKeyDialog } from "./SamlImportKeyDialog";

type SamlKeysProps = {
  clientId: string;
  save: () => void;
};

const KEYS = ["saml.signing", "saml.encryption"] as const;
export type KeyTypes = typeof KEYS[number];

const KEYS_MAPPING: { [key in KeyTypes]: { [index: string]: string } } = {
  "saml.signing": {
    name: "attributes.saml.client.signature",
    title: "signingKeysConfig",
    key: "clientSignature",
  },
  "saml.encryption": {
    name: "attributes.saml.encrypt",
    title: "encryptionKeysConfig",
    key: "encryptAssertions",
  },
};

type KeySectionProps = {
  keyInfo?: CertificateRepresentation;
  attr: KeyTypes;
  onChanged: (key: KeyTypes) => void;
  onGenerate: (key: KeyTypes, regenerate: boolean) => void;
  onImport: (key: KeyTypes) => void;
};

const KeySection = ({
  keyInfo,
  attr,
  onChanged,
  onGenerate,
  onImport,
}: KeySectionProps) => {
  const { t } = useTranslation("clients");
  const { control, watch } = useFormContext<ClientForm>();
  const title = KEYS_MAPPING[attr].title;
  const key = KEYS_MAPPING[attr].key;
  const name = KEYS_MAPPING[attr].name;

  const section = watch(name);
  return (
    <>
      <FormPanel title={t(title)} className="kc-form-panel__panel">
        <TextContent className="pf-u-pb-lg">
          <Text>{t(`${title}Explain`)}</Text>
        </TextContent>
        <FormAccess role="manage-clients" isHorizontal>
          <FormGroup
            labelIcon={
              <HelpItem
                helpText={`clients-help:${key}`}
                fieldLabelId={`clients:${key}`}
              />
            }
            label={t(key)}
            fieldId={key}
            hasNoPaddingTop
          >
            <Controller
              name={name}
              control={control}
              defaultValue="false"
              render={({ onChange, value }) => (
                <Switch
                  data-testid={key}
                  id={key}
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => {
                    const v = value.toString();
                    if (v === "true") {
                      onChanged(attr);
                      onChange(v);
                    } else {
                      onGenerate(attr, false);
                    }
                  }}
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
                <Button variant="tertiary">{t("common:export")}</Button>
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
  const [openImport, setImportOpen] = useState(false);
  const [refresh, setRefresh] = useState(0);

  const { setValue } = useFormContext();

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  useFetch(
    () =>
      Promise.all(
        KEYS.map((attr) =>
          adminClient.clients.getKeyInfo({ id: clientId, attr })
        )
      ),
    (info) => setKeyInfo(info),
    [refresh]
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
      FileSaver.saveAs(
        new Blob([info[index].privateKey!], {
          type: "application/octet-stream",
        }),
        "private.key"
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
          {openImport && (
            <SamlImportKeyDialog
              id={clientId}
              attr={attr}
              onClose={() => setImportOpen(false)}
            />
          )}
          <KeySection
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
            onImport={() => setImportOpen(true)}
          />
        </Fragment>
      ))}
    </PageSection>
  );
};
