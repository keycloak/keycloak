import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { useAlerts } from "../../../components/alert/Alerts";
import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { KEY_PROVIDER_TYPE } from "../../../util";
import { useParams } from "../../../utils/useParams";
import { KeyProviderParams, ProviderType } from "../../routes/KeyProvider";
import { toKeysTab } from "../../routes/KeysTab";

type KeyProviderFormProps = {
  id?: string;
  providerType: ProviderType;
  onClose?: () => void;
};

export const KeyProviderForm = ({
  providerType,
  onClose,
}: KeyProviderFormProps) => {
  const { t } = useTranslation("realm-settings");
  const { id } = useParams<{ id: string }>();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = form;

  const save = async (component: ComponentRepresentation) => {
    if (component.config)
      Object.entries(component.config).forEach(
        ([key, value]) =>
          (component.config![key] = Array.isArray(value) ? value : [value])
      );
    try {
      if (id) {
        await adminClient.components.update(
          { id },
          {
            ...component,
            providerType: KEY_PROVIDER_TYPE,
          }
        );
        addAlert(t("saveProviderSuccess"), AlertVariant.success);
      } else {
        await adminClient.components.create({
          ...component,
          providerId: providerType,
          providerType: KEY_PROVIDER_TYPE,
        });
        addAlert(t("saveProviderSuccess"), AlertVariant.success);
        onClose?.();
      }
    } catch (error) {
      addError("realm-settings:saveProviderError", error);
    }
  };

  useFetch(
    async () => {
      if (id) return await adminClient.components.findOne({ id });
    },
    (result) => {
      if (result) {
        reset({ ...result });
      }
    },
    []
  );

  return (
    <FormAccess isHorizontal role="manage-realm" onSubmit={handleSubmit(save)}>
      {id && (
        <FormGroup
          label={t("providerId")}
          labelIcon={
            <HelpItem
              helpText={t("client-scopes-help:mapperName")}
              fieldLabelId="providerId"
            />
          }
          fieldId="providerId"
          isRequired
        >
          <KeycloakTextInput
            id="providerId"
            data-testid="providerId-input"
            isReadOnly
            {...register("id")}
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("common:name")}
        labelIcon={
          <HelpItem
            helpText={t("client-scopes-help:mapperName")}
            fieldLabelId="name"
          />
        }
        fieldId="name"
        isRequired
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="name"
          control={control}
          rules={{ required: true }}
          defaultValue={providerType}
          render={({ field }) => (
            <TextInput
              id="name"
              value={field.value}
              onChange={field.onChange}
              data-testid="name-input"
            />
          )}
        />
      </FormGroup>
      <FormProvider {...form}>
        <DynamicComponents
          properties={
            allComponentTypes.find((type) => type.id === providerType)
              ?.properties || []
          }
        />
      </FormProvider>
      <ActionGroup>
        <Button
          data-testid="add-provider-button"
          variant="primary"
          type="submit"
        >
          {t("common:save")}
        </Button>
        <Button onClick={() => onClose?.()} variant="link">
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};

export default function KeyProviderFormPage() {
  const { t } = useTranslation("realm-settings");
  const params = useParams<KeyProviderParams>();
  const navigate = useNavigate();

  return (
    <>
      <ViewHeader titleKey={t("editProvider")} subKey={params.providerType} />
      <PageSection variant="light">
        <KeyProviderForm
          {...params}
          onClose={() =>
            navigate(toKeysTab({ realm: params.realm, tab: "providers" }))
          }
        />
      </PageSection>
    </>
  );
}
