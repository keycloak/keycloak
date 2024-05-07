import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

import { ActionGroup, AlertVariant, Button } from "@patternfly/react-core";
import { useState } from "react";
import { FieldErrors, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";

import { convertFormValuesToObject, convertToFormValues } from "../../../util";
import { adminClient } from "../../../admin-client";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { useFetch } from "../../../utils/useFetch";
import { toUsers } from "../../../user/routes/Users";
import { useAlerts } from "../../alert/Alerts";

import { DynamicComponents } from "../../dynamic/DynamicComponents";
import { FormAccess } from "../../form/FormAccess";

import { CustomAttributeStoreInstanceRouteParams } from "../routes/CustomInstance";

export function CustomInstanceSettingsTab() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const { id, providerId } =
    useParams<CustomAttributeStoreInstanceRouteParams>();
  const providers =
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.attributes.AttributeStoreProvider"
    ] || [];
  const { realm: realmName } = useRealm();

  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });
  const {
    reset,
    setValue,
    handleSubmit,
    formState: { isDirty },
  } = form;

  const { addAlert, addError } = useAlerts();

  const [parentId, setParentId] = useState("");
  const [provider, setProvider] = useState<ComponentTypeRepresentation>();

  useFetch(
    async () => {
      if (id) {
        return await adminClient.components.findOne({ id: id });
      }
      return undefined;
    },
    (fetchedComponent) => {
      if (fetchedComponent) {
        convertToFormValues(fetchedComponent, setValue);
      } else if (id) {
        throw new Error(t("notFound"));
      }

      // set provider here so we can be sure the form is loaded before rendering dynamic components
      const p = providers.find(
        (p: ComponentTypeRepresentation) => p.id === providerId,
      );
      if (p) setProvider(p);
    },
    [],
  );

  useFetch(
    () =>
      adminClient.realms.findOne({
        realm: realmName,
      }),
    (realm?: RealmRepresentation) => setParentId(realm?.id!),
    [],
  );

  const saveError = (errors: FieldErrors<ComponentRepresentation>) => {
    addError(
      `${
        !id
          ? "attributeStore.providers.custom.createError"
          : "attributeStore.providers.custom.updateError"
      }`,
      errors.config
        ? Object.entries(errors.config)
            .map(([k, v]) => `${k} is ${v?.type}`)
            .join(",")
        : "invlaid form",
    );
  };

  const save = async (component: ComponentRepresentation) => {
    console.log(component);
    const map = {
      ...component,
      config: Object.fromEntries(
        Object.entries(component.config || {}).map(([key, value]) => [
          key,
          Array.isArray(value) ? value : [value],
        ]),
      ),
      providerId: providerId,
      providerType: "org.keycloak.storage.attributes.AttributeStoreProvider",
      parentId,
    };

    const saveComponent = convertFormValuesToObject(map);

    try {
      if (!id) {
        await adminClient.components.create(saveComponent);
        navigate(toUsers({ realm: realmName, tab: "attributeStore" }));
      } else {
        await adminClient.components.update({ id: id }, saveComponent);
      }
      reset({ ...component });
      addAlert(
        t(
          !id
            ? "attributeStore.providers.custom.createSuccess"
            : "attributeStore.providers.custom.updateSuccess",
        ),
        AlertVariant.success,
      );
    } catch (error) {
      addError(
        `${
          !id
            ? "attributeStore.providers.custom.createError"
            : "attributeStore.providers.custom.updateError"
        }`,
        error,
      );
    }
  };

  return (
    <FormAccess
      role="manage-realm"
      isHorizontal
      onSubmit={handleSubmit(save, saveError)}
    >
      <FormProvider {...form}>
        {provider && (
          <DynamicComponents properties={provider.properties || []} />
        )}
      </FormProvider>
      <ActionGroup>
        <Button
          isDisabled={!isDirty}
          variant="primary"
          type="submit"
          data-testid="custom-save"
        >
          {t("save")}
        </Button>
        <Button
          variant="link"
          component={(props) => (
            <Link
              {...props}
              to={toUsers({ realm: realmName, tab: "attributeStore" })}
            />
          )}
          data-testid="custom-cancel"
        >
          {t("cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
}
