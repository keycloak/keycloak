import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Tooltip,
  ValidatedOptions,
} from "@patternfly/react-core";
import { CogIcon, TrashIcon } from "@patternfly/react-icons";

import type AuthenticatorConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type AuthenticatorConfigInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type { ExpandableExecution } from "../execution-model";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

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
  const { t } = useTranslation("authentication");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [show, setShow] = useState(false);
  const [config, setConfig] = useState<AuthenticatorConfigRepresentation>();
  const [configDescription, setConfigDescription] =
    useState<AuthenticatorConfigInfoRepresentation>();

  const form = useForm<ExecutionConfigModalForm>({ shouldUnregister: false });
  const {
    register,
    setValue,
    handleSubmit,
    formState: { errors },
  } = form;

  const setupForm = (config?: AuthenticatorConfigRepresentation) => {
    convertToFormValues(config, setValue);
  };

  useFetch(
    async () => {
      const configDescription =
        await adminClient.authenticationManagement.getConfigDescription({
          providerId: execution.providerId!,
        });
      let config: AuthenticatorConfigRepresentation | undefined;
      if (execution.authenticationConfig) {
        config = await adminClient.authenticationManagement.getConfig({
          id: execution.authenticationConfig,
        });
      }
      return { configDescription, config };
    },
    ({ configDescription, config }) => {
      setConfigDescription(configDescription);
      setConfig(config);
    },
    []
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
        const { id } = await adminClient.authenticationManagement.createConfig(
          newConfig
        );
        setConfig({ ...newConfig.config, id, alias: newConfig.alias });
      }
      addAlert(t("configSaveSuccess"), AlertVariant.success);
      setShow(false);
    } catch (error) {
      addError("authentication:configSaveError", error);
    }
  };

  return (
    <>
      <Tooltip content={t("common:settings")}>
        <Button
          variant="plain"
          aria-label={t("common:settings")}
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
            <FormGroup
              label={t("alias")}
              fieldId="alias"
              helperTextInvalid={t("common:required")}
              validated={
                errors.alias ? ValidatedOptions.error : ValidatedOptions.default
              }
              isRequired
              labelIcon={
                <HelpItem
                  helpText="authentication-help:alias"
                  fieldLabelId="authentication:alias"
                />
              }
            >
              <KeycloakTextInput
                isReadOnly={!!config}
                type="text"
                id="alias"
                name="alias"
                data-testid="alias"
                ref={register({ required: true })}
                validated={
                  errors.alias
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
            <FormProvider {...form}>
              <DynamicComponents
                properties={configDescription.properties || []}
              />
            </FormProvider>
            <ActionGroup>
              <Button data-testid="save" variant="primary" type="submit">
                {t("common:save")}
              </Button>
              <Button
                data-testid="cancel"
                variant={ButtonVariant.link}
                onClick={() => {
                  setShow(false);
                }}
              >
                {t("common:cancel")}
              </Button>
              {config && (
                <Button
                  className="pf-u-ml-4xl"
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
                  {t("common:clear")} <TrashIcon />
                </Button>
              )}
            </ActionGroup>
          </Form>
        </Modal>
      )}
    </>
  );
};
