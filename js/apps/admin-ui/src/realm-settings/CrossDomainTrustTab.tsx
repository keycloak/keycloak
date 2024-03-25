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
import { convertAttributeNameToForm, convertToFormValues } from "../util";

import { deserializeCrossDomainTrustConfig } from "./utils/crossDomainTrust";
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
  const { watch, handleSubmit, setValue } = useForm<FormFields>();
  const [addConfigOpen, setAddConfigOpen] = useState(false);
  const [selectedConfig, setSelectedConfig] = useState(
    {} as CrossDomainTrustConfig,
  );

  // initialize form
  useEffect(() => setupForm(), [realm]);

  const configs: CrossDomainTrustConfig[] = watch(
    convertAttributeNameToForm<FormFields>("attributes.crossDomainTrust"),
    [],
  );

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    if (realm.attributes?.["crossDomainTrust"]) {
      setValue(
        convertAttributeNameToForm("attributes.crossDomainTrust"),
        // @ts-ignore
        deserializeCrossDomainTrustConfig(realm.attributes),
      );
    }
  };

  const addConfig = (config: CrossDomainTrustConfig) => {
    if (configs.find((c) => c.issuer == config.issuer)) {
      addError(t("crossDomainTrustConfigAddFail"), "issuer must be unique");
      return;
    }

    setValue(
      convertAttributeNameToForm<FormFields>("attributes.crossDomainTrust"),
      [...configs, config],
    );
  };

  const removeConfig = (config: CrossDomainTrustConfig) => {
    setValue(
      convertAttributeNameToForm<FormFields>("attributes.crossDomainTrust"),
      configs.filter((c) => c.issuer != config.issuer),
    );
  };

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
      <AddCrossDomainTrustDialig
        isOpen={addConfigOpen}
        onAdded={(config: CrossDomainTrustConfig) => {
          addConfig(config);
        }}
        onClose={() => setAddConfigOpen(false)}
      />
      <KeycloakDataTable
        ariaLabelKey="assertionGrantConfigs"
        loader={configs}
        toolbarItem={
          <ToolbarItem>
            <Button
              id="registerConfigManually"
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
            cellFormatters: [
              (value) =>
                value ? value.toString().substring(0, 20) + "..." : "",
            ],
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
