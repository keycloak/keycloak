import type AuthenticatorConfigInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type AuthenticatorConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
  Tooltip,
} from "@patternfly/react-core";
import { CogIcon, TrashIcon } from "@patternfly/react-icons";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import type { ExpandableExecution } from "../execution-model";

type ExecutionConfigModalForm = {
  alias: string;
  config: { [index: string]: string };
};

type ExecutionConfigModalProps = {
  execution: ExpandableExecution;
};

export const ExecutionConfigModal = ({
  execution,
}: ExecutionConfigModalProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [show, setShow] = useState(false);
  const [config, setConfig] = useState<AuthenticatorConfigRepresentation>();
  const [configDescription, setConfigDescription] =
    useState<AuthenticatorConfigInfoRepresentation>();

  const form = useForm<ExecutionConfigModalForm>();
  const { setValue, handleSubmit } = form;

  // default config all executions should have
  const defaultConfigProperties = execution.authenticationFlow
    ? []
    : [
        {
          helpText: t("authenticatorRefConfig.value.help"),
          label: t("authenticatorRefConfig.value.label"),
          name: "default.reference.value",
          readOnly: false,
          secret: false,
          type: "String",
        },
        {
          helpText: t("authenticatorRefConfig.maxAge.help"),
          label: t("authenticatorRefConfig.maxAge.label"),
          name: "default.reference.maxAge",
          readOnly: false,
          secret: false,
          type: "String",
        },
      ];

  const setupForm = (config?: AuthenticatorConfigRepresentation) => {
    convertToFormValues(config || {}, setValue);
  };

  useFetch(
    async () => {
      let config: AuthenticatorConfigRepresentation | undefined;

      const configDescription = execution.configurable
        ? await adminClient.authenticationManagement.getConfigDescription({
            providerId: execution.providerId!,
          })
        : {
            name: execution.displayName,
            properties: [],
          };

      if (execution.authenticationConfig) {
        config = await adminClient.authenticationManagement.getConfig({
          id: execution.authenticationConfig,
        });
      }

      // merge default and fetched config properties
      configDescription.properties = [
        ...defaultConfigProperties!,
        ...configDescription.properties!,
      ];

      return { configDescription, config };
    },
    ({ configDescription, config }) => {
      setConfigDescription(configDescription);
      setConfig(config);
    },
    [],
  );

  useEffect(() => {
    if (config) setupForm(config);
  }, [config]);

  const save = async (saved: ExecutionConfigModalForm) => {
    const changedConfig = convertFormValuesToObject(saved);
    try {
      if (config) {
        const newConfig = {
          id: config.id,
          alias: config.alias,
          config: changedConfig.config,
        };
        await adminClient.authenticationManagement.updateConfig(newConfig);
        setConfig({ ...newConfig });
      } else {
        const newConfig = {
          id: execution.id!,
          alias: changedConfig.alias,
          config: changedConfig.config,
        };
        const { id } =
          await adminClient.authenticationManagement.createConfig(newConfig);
        setConfig({ ...newConfig.config, id, alias: newConfig.alias });
      }
      addAlert(t("configSaveSuccess"), AlertVariant.success);
      setShow(false);
    } catch (error) {
      addError("configSaveError", error);
    }
  };

  return (
    <>
      <Tooltip content={t("settings")}>
        <Button
          variant="plain"
          aria-label={t("settings")}
          onClick={() => setShow(true)}
        >
          <CogIcon />
        </Button>
      </Tooltip>
      {configDescription && (
        <Modal
          variant={ModalVariant.small}
          isOpen={show}
          title={t("executionConfig", { name: configDescription.name })}
          onClose={() => setShow(false)}
        >
          <Form id="execution-config-form" onSubmit={handleSubmit(save)}>
            <FormProvider {...form}>
              <TextControl
                name="alias"
                label={t("alias")}
                labelIcon={t("authenticationAliasHelp")}
                rules={{ required: t("required") }}
                isDisabled={!!config}
              />
              <DynamicComponents
                stringify
                properties={configDescription.properties || []}
              />
            </FormProvider>
            <ActionGroup>
              <Button data-testid="save" variant="primary" type="submit">
                {t("save")}
              </Button>
              <Button
                data-testid="cancel"
                variant={ButtonVariant.link}
                onClick={() => {
                  setShow(false);
                }}
              >
                {t("cancel")}
              </Button>
              {config && (
                <Button
                  className="pf-v5-u-ml-4xl"
                  data-testid="clear"
                  variant={ButtonVariant.link}
                  onClick={async () => {
                    await adminClient.authenticationManagement.delConfig({
                      id: config.id!,
                    });
                    setConfig(undefined);
                    setShow(false);
                  }}
                >
                  {t("clear")} <TrashIcon />
                </Button>
              )}
            </ActionGroup>
          </Form>
        </Modal>
      )}
    </>
  );
};
