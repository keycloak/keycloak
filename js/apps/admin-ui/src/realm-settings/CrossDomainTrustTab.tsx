import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import {
  TextContent,
  Text,
  TextVariants,
  ButtonVariant,
  ActionGroup,
  Button,
  ToolbarItem,
} from "@patternfly/react-core";

import type CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

import { FormAccess } from "../components/form/FormAccess";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  KeycloakDataTable,
  Action,
} from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { convertAttributeNameToForm } from "../util";

import {
  deserializeCrossDomainTrustConfig,
  serializeCrossDomainTrustConfig,
} from "./utils/crossDomainTrust";
import { AddCrossDomainTrustDialig } from "./AddCrossDomainTrustDialog";

type CrossDomainTrustTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

type FormFields = {
  attributes: Record<string, any>;
};

export const CrossDomainTrust = ({ realm, save }: CrossDomainTrustTabProps) => {
  const { t } = useTranslation();
  const { addError } = useAlerts();

  const { handleSubmit, setValue } = useForm<FormFields>();

  const [configs, setConfigs] = useState<CrossDomainTrustConfig[]>([]);
  const [addConfigOpen, setAddConfigOpen] = useState(false);

  const [selectedConfig, setSelectedConfig] = useState<CrossDomainTrustConfig>(
    {} as CrossDomainTrustConfig,
  );
  const [editConfig, setEditConfig] = useState<
    CrossDomainTrustConfig | undefined
  >();

  // deserialize attribute value on load
  useEffect(() => setupForm(), [realm]);

  // serialize attribute value on change
  useEffect(() => {
    onChange(configs);
  }, [configs]);

  const setupForm = () => {
    setConfigs(onLoad(realm.attributes?.["crossDomainTrust"]));
  };

  const onLoad = (config: string): CrossDomainTrustConfig[] => {
    return deserializeCrossDomainTrustConfig(config);
  };

  const onChange = (config: CrossDomainTrustConfig[]) => {
    setValue(
      convertAttributeNameToForm("attributes.crossDomainTrust"),
      // @ts-ignore
      serializeCrossDomainTrustConfig(config),
    );
  };

  const addConfig = (config: CrossDomainTrustConfig, edit: boolean) => {
    if (!edit && configs.find((c) => c.issuer == config.issuer)) {
      addError(t("crossDomainTrustConfigAddFail"), "issuer must be unique");
      return;
    }

    if (edit) {
      setConfigs([...configs.filter((c) => c.issuer != config.issuer), config]);
    } else {
      setConfigs([...configs, config]);
    }
  };

  const removeConfig = (config: CrossDomainTrustConfig) => {
    setConfigs(configs.filter((c) => c.issuer != config.issuer));
  };

  const [toggleCertificateDialog, CertificateDialog] = useConfirmDialog({
    titleKey: t("certificate"),
    messageKey: selectedConfig.certificate,
    continueButtonLabel: "close",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: () => Promise.resolve(),
  });

  const [toggleDeleteConfigConfirm, DeleteConfigConfirm] = useConfirmDialog({
    titleKey: "crossDomainTrustConfigDeleteTitle",
    children: (
      <TextContent>
        <Text component={TextVariants.h4}>
          {t("crossDomainTrustConfigIssuer")}
        </Text>
        <Text component={TextVariants.p}>{selectedConfig.issuer}</Text>
        <Text component={TextVariants.h4}>
          {t("crossDomainTrustConfigAudience")}
        </Text>
        <Text component={TextVariants.p}>{selectedConfig.audience}</Text>
        <Text component={TextVariants.h4}>
          {t("crossDomainTrustConfigCert")}
        </Text>
        <Text component={TextVariants.p}>{selectedConfig.certificate}</Text>
      </TextContent>
    ),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: () => {
      removeConfig(selectedConfig);
    },
  });

  return (
    <FormAccess role="manage-realm" isHorizontal onSubmit={handleSubmit(save)}>
      <DeleteConfigConfirm />
      <CertificateDialog />
      <AddCrossDomainTrustDialig
        isOpen={addConfigOpen}
        onAdded={(config: CrossDomainTrustConfig, edit: boolean) => {
          addConfig(config, edit);
        }}
        onClose={() => {
          setAddConfigOpen(false);
          setEditConfig(undefined);
        }}
        edit={editConfig}
      />
      <KeycloakDataTable
        ariaLabelKey="assertionGrantConfigs"
        loader={configs}
        toolbarItem={
          <ToolbarItem>
            <Button
              onClick={() => setAddConfigOpen(true)}
              variant={ButtonVariant.primary}
            >
              {t("add")}
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("delete"),
            onRowClick: (config: CrossDomainTrustConfig) => {
              setSelectedConfig(config);
              toggleDeleteConfigConfirm();
            },
          } as Action<CrossDomainTrustConfig>,
          {
            title: t("edit"),
            onRowClick: (config: CrossDomainTrustConfig) => {
              setEditConfig(config);
              setAddConfigOpen(true);
            },
          } as Action<CrossDomainTrustConfig>,
        ]}
        columns={[
          {
            name: "issuer",
            displayKey: "crossDomainTrustConfigIssuer",
          },
          {
            name: "audience",
            displayKey: "crossDomainTrustConfigAudience",
          },
          {
            name: "certificate",
            displayKey: "crossDomainTrustConfigCert",
            cellRenderer: (row: CrossDomainTrustConfig) => (
              <Button
                onClick={() => {
                  toggleCertificateDialog();
                  setSelectedConfig(row);
                }}
                variant="secondary"
              >
                {t("certificate")}
              </Button>
            ),
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("crossDomainTrustConfigNoneMessage")}
            instructions={t("crossDomainTrustConfigNoneInstructions")}
            primaryActionText={t("add")}
            onPrimaryAction={() => setAddConfigOpen(true)}
          />
        }
      />
      <ActionGroup>
        <Button variant="primary" type="submit" data-testid="general-tab-save">
          {t("save")}
        </Button>
        <Button
          data-testid="general-tab-revert"
          variant="link"
          onClick={setupForm}
        >
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
