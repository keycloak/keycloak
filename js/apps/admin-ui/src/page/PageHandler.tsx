import { fetchWithError } from "@keycloak/keycloak-admin-client";
import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button, Form, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { useRealm } from "../context/realm-context/RealmContext";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";
import { joinPath } from "../utils/joinPath";
import { useParams } from "../utils/useParams";
import { type PAGE_PROVIDER, TAB_PROVIDER } from "./constants";
import { toPage } from "./routes";

type StorageType =
  | "COMPONENT"
  | "CLIENT"
  | "USER"
  | "GROUP"
  | "IDENTITY_PROVIDER"
  | "CUSTOM";

type PageHandlerProps = {
  id?: string;
  providerType: typeof TAB_PROVIDER | typeof PAGE_PROVIDER;
  page: ComponentTypeRepresentation;
};

export const PageHandler = ({
  id: idAttribute,
  providerType,
  page: { id: providerId, ...page },
}: PageHandlerProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<Record<string, unknown>>();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [id, setId] = useState(idAttribute);
  const params = useParams();

  const [isLoading, setIsLoading] = useState(true);

  const storageType: StorageType =
    (page.metadata?.storageType as StorageType) || "COMPONENT";

  useFetch(
    async () => {
      switch (storageType) {
        case "CLIENT":
          if (params.clientId) {
            return {
              config: (
                await adminClient.clients.findOne({ id: params.clientId })
              )?.attributes,
            };
          }
          return undefined;
        case "USER":
          if (params.userId) {
            return {
              config: (await adminClient.users.findOne({ id: params.userId }))
                ?.attributes,
            };
          }
          return undefined;
        case "GROUP":
          if (params.groupId) {
            return {
              config: (await adminClient.groups.findOne({ id: params.groupId }))
                ?.attributes,
            };
          }
          return undefined;
        case "IDENTITY_PROVIDER":
          if (params.providerId) {
            return {
              config: (
                await adminClient.identityProviders.findOne({
                  alias: params.providerId,
                })
              )?.config,
            };
          }
          return undefined;
        case "CUSTOM": {
          if (page.metadata?.endpoint) {
            const endpoint = interpolateEndpoint(
              page.metadata.endpoint as string,
              params,
            );
            const response = await fetchWithError(
              joinPath(
                adminClient.baseUrl,
                "admin/realms",
                realmName,
                endpoint,
              ),
              {
                method: "GET",
                headers: {
                  ...getAuthorizationHeaders(
                    await adminClient.getAccessToken(),
                  ),
                  Accept: "application/json",
                },
              },
            );
            return response.json();
          }
          return undefined;
        }
        case "COMPONENT":
        default: {
          const [data, tabs] = await Promise.all([
            id ? adminClient.components.findOne({ id }) : Promise.resolve(),
            providerType === TAB_PROVIDER
              ? adminClient.components.find({ type: TAB_PROVIDER })
              : Promise.resolve(),
          ]);
          const tab = (tabs || []).find((t) => t.providerId === providerId);
          return data || tab;
        }
      }
    },
    (data) => {
      form.reset(data || {});
      setId(data?.id);
      setIsLoading(false);
    },
    [],
  );

  const onSubmit = async (formData: ComponentRepresentation) => {
    try {
      switch (storageType) {
        case "CLIENT":
          if (params.clientId) {
            const client = await adminClient.clients.findOne({
              id: params.clientId,
            });
            await adminClient.clients.update(
              { id: params.clientId },
              {
                ...client,
                attributes: {
                  ...client?.attributes,
                  ...formData.config,
                },
              },
            );
          }
          break;
        case "USER":
          if (params.userId) {
            const user = await adminClient.users.findOne({ id: params.userId });
            await adminClient.users.update(
              { id: params.userId },
              {
                ...user,
                attributes: {
                  ...user?.attributes,
                  ...formData.config,
                },
              },
            );
          }
          break;
        case "GROUP":
          if (params.groupId) {
            const group = await adminClient.groups.findOne({
              id: params.groupId,
            });
            await adminClient.groups.update(
              { id: params.groupId },
              {
                ...group,
                attributes: {
                  ...group?.attributes,
                  ...formData.config,
                },
              },
            );
          }
          break;
        case "IDENTITY_PROVIDER":
          if (params.providerId) {
            const idp = await adminClient.identityProviders.findOne({
              alias: params.providerId,
            });
            await adminClient.identityProviders.update(
              { alias: params.providerId },
              {
                ...idp,
                config: {
                  ...idp?.config,
                  ...formData.config,
                },
              },
            );
          }
          break;
        case "CUSTOM": {
          if (page.metadata?.endpoint) {
            const endpoint = interpolateEndpoint(
              page.metadata.endpoint as string,
              params,
            );
            await fetchWithError(
              joinPath(
                adminClient.baseUrl,
                "admin/realms",
                realmName,
                endpoint,
              ),
              {
                method: "PUT",
                headers: {
                  ...getAuthorizationHeaders(
                    await adminClient.getAccessToken(),
                  ),
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ ...formData, ...params }),
              },
            );
          }
          break;
        }
        case "COMPONENT":
        default: {
          const component = formData as ComponentRepresentation;
          if (component.config || params) {
            component.config = Object.assign(component.config || {}, params);
            Object.entries(component.config).forEach(
              ([key, value]) =>
                (component.config![key] = Array.isArray(value)
                  ? value
                  : [value]),
            );
          }
          const updatedComponent = {
            ...component,
            providerId,
            providerType,
            parentId: realm?.id,
          };
          if (id) {
            await adminClient.components.update({ id }, updatedComponent);
          } else {
            const { id: newId } =
              await adminClient.components.create(updatedComponent);
            setId(newId);
          }
          break;
        }
      }
      addAlert(t("itemSaveSuccessful"));
    } catch (error) {
      addError("itemSaveError", error);
    }
  };

  if (isLoading) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <Form
        isHorizontal
        onSubmit={form.handleSubmit(onSubmit)}
        className="keycloak__form"
      >
        <FormProvider {...form}>
          <DynamicComponents properties={page.properties} />
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
              />
            )}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
      </Form>
    </PageSection>
  );
};

function interpolateEndpoint(
  endpoint: string,
  params: Record<string, string>,
): string {
  return endpoint.replace(/\{(\w+)\}/g, (_, key) => params[key] || `{${key}}`);
}
