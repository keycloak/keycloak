import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../../admin-client";
import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../../components/form/FormAccess";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const { addAlert, addError } = useAlerts();

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });
  const { handleSubmit, reset } = form;

  const save = async (component: ComponentRepresentation) => {
    if (component.config)
      Object.entries(component.config).forEach(
        ([key, value]) =>
          (component.config![key] = Array.isArray(value) ? value : [value]),
      );
    try {
      if (id) {
        await adminClient.components.update(
          { id },
          {
            ...component,
            providerType: KEY_PROVIDER_TYPE,
          },
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
      addError("saveProviderError", error);
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
    [],
  );

  return (
    <FormAccess isHorizontal role="manage-realm" onSubmit={handleSubmit(save)}>
      <FormProvider {...form}>
        {id && (
          <TextControl
            name="id"
            label={t("providerId")}
            labelIcon={t("providerIdHelp")}
            rules={{
              required: t("required"),
            }}
            readOnly
          />
        )}
        <TextControl
          name="name"
          defaultValue={providerType}
          label={t("name")}
          labelIcon={t("keyProviderMapperNameHelp")}
          rules={{
            required: t("required"),
          }}
        />
        <DynamicComponents
          properties={
            allComponentTypes.find((type) => type.id === providerType)
              ?.properties || []
          }
        />
        <ActionGroup>
          <Button
            data-testid="add-provider-button"
            variant="primary"
            type="submit"
          >
            {t("save")}
          </Button>
          <Button onClick={() => onClose?.()} variant="link">
            {t("cancel")}
          </Button>
        </ActionGroup>
      </FormProvider>
    </FormAccess>
  );
};

export default function KeyProviderFormPage() {
  const { t } = useTranslation();
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
