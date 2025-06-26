import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../../admin-client";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../../components/form/FormAccess";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { KEY_PROVIDER_TYPE } from "../../../util";
import { useParams } from "../../../utils/useParams";
import { KeyProviderParams, ProviderType } from "../../routes/KeyProvider";
import { toKeysTab } from "../../routes/KeysTab";
import { RoutableTabs, useRoutableTab } from "../../../components/routable-tabs/RoutableTabs";
import { TideKeyTab, toTideKey } from "../routes/TideKeys";
import { TideLicensingTab } from "../../../components/tide-licensing-tab/TideLicensingTab";

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
    <FormAccess isHorizontal role="manage-realm" onSubmit={handleSubmit(save)} className="pf-v5-u-p-md">
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
          isTideProvider={providerType === "tide-vendor-key"}
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

/** TIDE IMPLEMENTATION START */
const useTab = ({ realm, id, providerType, tab }: { realm: string; id: string; providerType: ProviderType; tab: TideKeyTab }) => {
  return useRoutableTab(toTideKey({ realm, id, providerType, tab }));
};
/** TIDE IMPLEMENTATION END */


export default function KeyProviderFormPage() {
  const { t } = useTranslation();
  const params = useParams<KeyProviderParams>();
  const navigate = useNavigate();

  /** TIDE IMPLEMENTATION START */
  const settingsTab = useTab({ ...params, tab: "settings" });
  const licenseTab = useTab({ ...params, tab: "license" });
  /** TIDE IMPLEMENTATION END */
  return (
    <>
      <ViewHeader titleKey={t("editProvider")} subKey={params.providerType} />
      <PageSection variant="light" className="pf-v5-u-p-0">
        {params.providerType === "tide-vendor-key" ? (
          <RoutableTabs
            mountOnEnter
            unmountOnExit
            defaultLocation={toTideKey({ ...params, tab: "settings" })}
          >
            <Tab
              title={<TabTitleText>Settings</TabTitleText>}
              {...settingsTab}
            >
              <KeyProviderForm
                {...params}
                onClose={() =>
                  navigate(toKeysTab({ realm: params.realm, tab: "providers" }))
                }
              />
            </Tab>
            <Tab
              title={<TabTitleText>License</TabTitleText>}
              {...licenseTab}
            >
              <TideLicensingTab />
            </Tab>

          </RoutableTabs>
        ) : (
          <KeyProviderForm
            {...params}
            onClose={() =>
              navigate(toKeysTab({ realm: params.realm, tab: "providers" }))
            }
          />
        )}

      </PageSection>
    </>
  );
}
