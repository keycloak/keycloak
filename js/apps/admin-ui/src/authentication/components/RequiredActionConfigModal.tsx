import RequiredActionConfigInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionConfigInfoRepresentation";
import RequiredActionConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionConfigRepresentation";
import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import {
  isUserProfileError,
  setUserProfileServerError,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { TrashIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

type RequiredActionConfigModalForm = {
  // alias: string;
  config: { [index: string]: string };
};

type RequiredActionConfigModalProps = {
  requiredAction: RequiredActionProviderRepresentation;
  onClose: () => void;
};

export const RequiredActionConfigModal = ({
  requiredAction,
  onClose,
}: RequiredActionConfigModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [configDescription, setConfigDescription] =
    useState<RequiredActionConfigInfoRepresentation>();

  const form = useForm<RequiredActionConfigModalForm>();
  const { setValue, handleSubmit } = form;

  // // default config all required actions should have
  // const defaultConfigProperties = [];

  const setupForm = (config?: RequiredActionConfigRepresentation) => {
    convertToFormValues(config || {}, setValue);
  };

  useFetch(
    async () => {
      const configDescription =
        await adminClient.authenticationManagement.getRequiredActionConfigDescription(
          {
            alias: requiredAction.alias!,
          },
        );

      const config =
        await adminClient.authenticationManagement.getRequiredActionConfig({
          alias: requiredAction.alias!,
        });

      // merge default and fetched config properties
      configDescription.properties = [
        //...defaultConfigProperties!,
        ...configDescription.properties!,
      ];

      return { configDescription, config };
    },
    ({ configDescription, config }) => {
      setConfigDescription(configDescription);
      setupForm(config);
    },
    [],
  );

  const save = async (saved: RequiredActionConfigModalForm) => {
    const newConfig = convertFormValuesToObject(saved);
    try {
      await adminClient.authenticationManagement.updateRequiredActionConfig(
        { alias: requiredAction.alias! },
        newConfig,
      );
      setupForm(newConfig);
      addAlert(t("configSaveSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      if (isUserProfileError(error)) {
        setUserProfileServerError(
          error,
          (name: string | number, error: unknown) => {
            // TODO: Does not set set the error message to the field, yet.
            // Still, this will do all front end replacement and translation of keys.
            addError("configSaveError", (error as any).message);
          },
          t,
        );
      } else {
        addError("configSaveError", error);
      }
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen
      title={t("requiredActionConfig", { name: requiredAction.name })}
      onClose={onClose}
    >
      <Form id="required-action-config-form" onSubmit={handleSubmit(save)}>
        <FormProvider {...form}>
          <DynamicComponents
            stringify
            properties={configDescription?.properties || []}
          />
        </FormProvider>
        <ActionGroup>
          <Button data-testid="save" variant="primary" type="submit">
            {t("save")}
          </Button>
          <Button
            data-testid="cancel"
            variant={ButtonVariant.link}
            onClick={onClose}
          >
            {t("cancel")}
          </Button>
          <Button
            className="pf-v5-u-ml-3xl"
            data-testid="clear"
            variant={ButtonVariant.link}
            onClick={async () => {
              await adminClient.authenticationManagement.removeRequiredActionConfig(
                {
                  alias: requiredAction.alias!,
                },
              );
              form.reset({});
              onClose();
            }}
          >
            {t("clear")} <TrashIcon />
          </Button>
        </ActionGroup>
      </Form>
    </Modal>
  );
};
