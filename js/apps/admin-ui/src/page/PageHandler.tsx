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
import { Link, useLocation } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { useRealm } from "../context/realm-context/RealmContext";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";
import { joinPath } from "../utils/joinPath";
import { useParams } from "../utils/useParams";
import { PAGE_PROVIDER, TAB_PROVIDER } from "./constants";
import {
  getEntityId,
  interpolateEndpoint,
  isEntityStorageType,
  normalizeConfig,
  resolveTabParams,
  type StorageType,
} from "./pageHandlerStorage";
import { toPage } from "./routes";

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
  const form = useForm<ComponentRepresentation>();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [id, setId] = useState(idAttribute);
  const routeParams = useParams();
  const { pathname } = useLocation();
  const tabParams = resolveTabParams(
    pathname,
    page.metadata.path as string | undefined,
    routeParams,
  );

  const [isLoading, setIsLoading] = useState(true);

  const storageType: StorageType =
    (page.metadata.storageType as StorageType | undefined) || "COMPONENT";

  useFetch(
    async () => {
      const entityId = getEntityId(storageType, tabParams);

      switch (storageType) {
        case "CLIENT":
          if (entityId) {
            const attributes = (
              await adminClient.clients.findOne({ id: entityId })
            )?.attributes;
            return {
              config: normalizeConfig(
                attributes as Record<string, unknown>,
                page.properties,
                "load",
                "string-map",
              ),
            };
          }
          return undefined;
        case "USER":
          if (entityId) {
            const attributes = (
              await adminClient.users.findOne({ id: entityId })
            )?.attributes;
            return {
              config: normalizeConfig(
                attributes as Record<string, unknown>,
                page.properties,
                "load",
                "list-map",
              ),
            };
          }
          return undefined;
        case "IDENTITY_PROVIDER":
          if (entityId) {
            const config = (
              await adminClient.identityProviders.findOne({
                alias: entityId,
              })
            )?.config;
            return {
              config: normalizeConfig(
                config as Record<string, unknown>,
                page.properties,
                "load",
                "string-map",
              ),
            };
          }
          return undefined;
        case "CUSTOM": {
          if (page.metadata.endpoint) {
            const endpoint = interpolateEndpoint(
              page.metadata.endpoint as string,
              tabParams,
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
      const entityId = getEntityId(storageType, tabParams);

      if (
        (isEntityStorageType(storageType) && !entityId) ||
        (storageType === "CUSTOM" && !page.metadata.endpoint)
      ) {
        throw new Error(
          `Missing required parameters for storageType: ${storageType}`,
        );
      }

      switch (storageType) {
        case "CLIENT":
          if (entityId) {
            const client = await adminClient.clients.findOne({
              id: entityId,
            });
            await adminClient.clients.update(
              { id: entityId },
              {
                ...client,
                attributes: {
                  ...client?.attributes,
                  ...normalizeConfig(
                    formData.config as Record<string, unknown>,
                    page.properties,
                    "save",
                    "string-map",
                  ),
                },
              },
            );
          }
          break;
        case "USER":
          if (entityId) {
            const user = await adminClient.users.findOne({ id: entityId });
            await adminClient.users.update(
              { id: entityId },
              {
                ...user,
                attributes: {
                  ...user?.attributes,
                  ...normalizeConfig(
                    formData.config as Record<string, unknown>,
                    page.properties,
                    "save",
                    "list-map",
                  ),
                },
              },
            );
          }
          break;
        case "GROUP":
          if (entityId) {
            const group = await adminClient.groups.findOne({
              id: entityId,
            });
            await adminClient.groups.update(
              { id: entityId },
              {
                ...group,
                attributes: {
                  ...group?.attributes,
                  ...normalizeConfig(
                    formData.config as Record<string, unknown>,
                    page.properties,
                    "save",
                    "list-map",
                  ),
                },
              },
            );
          }
          break;
        case "IDENTITY_PROVIDER":
          if (entityId) {
            const idp = await adminClient.identityProviders.findOne({
              alias: entityId,
            });
            await adminClient.identityProviders.update(
              { alias: entityId },
              {
                ...idp,
                config: {
                  ...idp?.config,
                  ...normalizeConfig(
                    formData.config as Record<string, unknown>,
                    page.properties,
                    "save",
                    "string-map",
                  ),
                },
              },
            );
          }
          break;
        case "CUSTOM": {
          if (page.metadata.endpoint) {
            const endpoint = interpolateEndpoint(
              page.metadata.endpoint as string,
              tabParams,
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
                body: JSON.stringify({ ...formData, ...tabParams }),
              },
            );
          }
          break;
        }
        case "COMPONENT":
        default: {
          const component = formData as ComponentRepresentation;
          component.config = Object.assign(component.config || {}, tabParams);
          Object.entries(component.config).forEach(
            ([key, value]) =>
              (component.config![key] = Array.isArray(value) ? value : [value]),
          );
          const updatedComponent = {
            ...component,
            providerId,
            providerType,
            parentId: realm.id,
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
          {providerType === PAGE_PROVIDER ? (
            <Button
              data-testid="cancel"
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
          ) : (
            <Button
              data-testid="cancel"
              variant="link"
              onClick={() => form.reset()}
            >
              {t("revert")}
            </Button>
          )}
        </ActionGroup>
      </Form>
    </PageSection>
  );
};
