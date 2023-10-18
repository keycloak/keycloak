import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { ActionGroup, Button, Form, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { useAlerts } from "../components/alert/Alerts";
import { adminClient } from "../admin-client";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useFetch } from "../utils/useFetch";

export default function Page() {
  const { t } = useTranslation();
  const { componentTypes } = useServerInfo();
  const pages =
    componentTypes?.["org.keycloak.services.ui.extend.UiPageProvider"];

  // Here the pageId should be used instead
  const page = pages?.[0];
  const form = useForm<ComponentTypeRepresentation>();
  const { realm: realmName } = useRealm();
  const { id } = useParams<{ id: string }>();
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
        providerId: "admin-ui-page",
        providerType: "org.keycloak.services.ui.extend.UiPageProvider",
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
      <ViewHeader titleKey={page?.id || "page"} subKey={page?.helpText} />
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
              component={(props) => <Link {...props} to=""></Link>}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </PageSection>
  );
}
