import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { ActionGroup, Button, Form, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useFetch } from "../utils/useFetch";
import { PAGE_PROVIDER } from "./PageList";
import { PageParams, toPage } from "./routes";

export default function Page() {
  const { t } = useTranslation();
  const { componentTypes } = useServerInfo();
  const pages = componentTypes?.[PAGE_PROVIDER];
  const { id, providerId } = useParams<PageParams>();

  const page = pages?.find((p) => p.id === providerId);
  const form = useForm<ComponentTypeRepresentation>();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const { addAlert, addError } = useAlerts();

  useFetch(
    async () =>
      await Promise.all([
        adminClient.realms.findOne({ realm: realmName }),
        id ? adminClient.components.findOne({ id }) : Promise.resolve(),
      ]),
    ([realm, data]) => {
      setRealm(realm);
      form.reset(data || {});
    },
    [],
  );

  const onSubmit = async (component: ComponentRepresentation) => {
    if (component.config)
      Object.entries(component.config).forEach(
        ([key, value]) =>
          (component.config![key] = Array.isArray(value) ? value : [value]),
      );
    try {
      const updatedComponent = {
        ...component,
        providerId,
        providerType: PAGE_PROVIDER,
        parentId: realm?.id,
      };
      if (id) {
        await adminClient.components.update({ id }, updatedComponent);
      } else {
        await adminClient.components.create(updatedComponent);
      }
      addAlert("Successful saved / updated");
    } catch (error) {
      addError("Error: {{error}}", error);
    }
  };

  return (
    <PageSection variant="light">
      <ViewHeader titleKey={id || t("createItem")} />
      <PageSection variant="light">
        <Form isHorizontal onSubmit={form.handleSubmit(onSubmit)}>
          <FormProvider {...form}>
            <DynamicComponents properties={page?.properties || []} />
          </FormProvider>

          <ActionGroup>
            <Button data-testid="save" type="submit">
              {t("save")}
            </Button>
            <Button
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={toPage({ realm: realmName, providerId: providerId! })}
                ></Link>
              )}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </PageSection>
  );
}
