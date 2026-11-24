import type AuthenticatorConfigInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import type AuthenticatorConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {
  KeycloakSpinner,
  TextControl,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Flex,
  Form,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
  Tooltip,
} from "@patternfly/react-core";
import { CogIcon, TrashIcon } from "@patternfly/react-icons";
import { useState } from "react";
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
  const [loading, setLoading] = useState(false);
  const [config, setConfig] = useState<AuthenticatorConfigRepresentation>();
  const [configDescription, setConfigDescription] =
    useState<AuthenticatorConfigInfoRepresentation>();

  const form = useForm<ExecutionConfigModalForm>({
    defaultValues: { alias: "", config: {} },
  });
  const { setValue, handleSubmit, reset, getValues } = form;

  const clearConfigForm = () => {
    const existing = getValues("config");
    reset({
      alias: "",
      // Beerified keys stay registered on the form; replacing config with {}
      // leaves their previous values in place when the modal remounts.
      config: Object.fromEntries(Object.keys(existing).map((key) => [key, ""])),
    });
  };

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

  const open = async () => {
    setShow(true);
    setLoading(true);
    try {
      let nextConfig: AuthenticatorConfigRepresentation | undefined;

      const nextDescription = execution.configurable
        ? await adminClient.authenticationManagement.getConfigDescription({
            providerId: execution.providerId!,
          })
        : {
            name: execution.displayName,
            properties: [],
          };

      if (execution.authenticationConfig) {
        nextConfig = await adminClient.authenticationManagement.getConfig({
          id: execution.authenticationConfig,
        });
      }

      nextDescription.properties = [
        ...defaultConfigProperties!,
        ...nextDescription.properties!,
      ];

      setConfigDescription(nextDescription);
      setConfig(nextConfig);
      if (nextConfig) {
        setupForm(nextConfig);
      } else {
        clearConfigForm();
      }
    } catch (error) {
      addError("configSaveError", error);
      setShow(false);
    } finally {
      setLoading(false);
    }
  };

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
          onClick={() => void open()}
          icon={<CogIcon />}
        />
      </Tooltip>
      <Modal
        variant={ModalVariant.small}
        isOpen={show}
        onClose={() => setShow(false)}
        aria-label={t("executionConfig", {
          name: configDescription?.name || execution.displayName,
        })}
      >
        <ModalHeader
          title={t("executionConfig", {
            name: configDescription?.name || execution.displayName,
          })}
        />
        <ModalBody>
          {loading || !configDescription ? (
            <KeycloakSpinner />
          ) : (
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
            </Form>
          )}
        </ModalBody>
        <ModalFooter>
          <ActionGroup>
            <Flex
              className="pf-v6-u-flex-grow-1"
              justifyContent={{ default: "justifyContentSpaceBetween" }}
            >
              <div>
                <Button
                  className="pf-v6-u-mr-md"
                  data-testid="save"
                  variant="primary"
                  type="submit"
                  form="execution-config-form"
                  isDisabled={loading || !configDescription}
                >
                  {t("save")}
                </Button>
                <Button
                  data-testid="cancel"
                  variant={ButtonVariant.link}
                  type="button"
                  onClick={() => setShow(false)}
                >
                  {t("cancel")}
                </Button>
              </div>
              {/* Keep Clear outside form submit semantics: PF Button
                  defaults to type=button, but stay explicit and avoid
                  racing form onSubmit when deleting a config. */}
              {config && (
                <Button
                  icon={<TrashIcon />}
                  iconPosition="end"
                  data-testid="clear"
                  variant={ButtonVariant.link}
                  type="button"
                  formNoValidate
                  onClick={async (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    await adminClient.authenticationManagement.delConfig({
                      id: config.id!,
                    });
                    setConfig(undefined);
                    clearConfigForm();
                    setShow(false);
                  }}
                >
                  {t("clear")}
                </Button>
              )}
            </Flex>
          </ActionGroup>
        </ModalFooter>
      </Modal>
    </>
  );
};
