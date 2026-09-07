import { fetchWithError } from "@keycloak/keycloak-admin-client";
import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button, Form, PageSection } from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { DynamicComponents } from "../components/dynamic/DynamicComponents";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";
import { joinPath } from "../utils/joinPath";
import { useParams } from "../utils/useParams";
import { PAGE_PROVIDER, TAB_PROVIDER } from "./constants";
import {
  getEntityId,
  interpolateEndpoint,
  isEntityStorageType,
  mergeEntityConfig,
  normalizeConfig,
  resolveTabParams,
  type StorageType,
} from "./pageHandlerStorage";
import { toPage } from "./routes";
import { canManageUiExtension, canViewUiExtension } from "./uiExtensionAccess";

type PageHandlerProps = {
  id?: string;
  providerType: typeof TAB_PROVIDER | typeof PAGE_PROVIDER;
  page: ComponentTypeRepresentation;
};

export const PageHandler = ({
  id: idAttribute,
  providerType,
  page: pageType,
}: PageHandlerProps) => {
  const { id: providerId, ...page } = pageType;
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<ComponentRepresentation>();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const access = useAccess();
  const [id, setId] = useState(idAttribute);
  const routeParams = useParams();
  const { pathname } = useLocation();
  const tabParams = resolveTabParams(
    pathname,
    page.metadata.path as string | undefined,
    routeParams,
  );

  const [isLoading, setIsLoading] = useState(true);
  const [properties, setProperties] = useState(page.properties);
  const canView = canViewUiExtension(pageType, access);
  const canManage = canManageUiExtension(pageType, access);

  const storageType: StorageType =
    (page.metadata.storageType as StorageType | undefined) || "COMPONENT";
  const resolvedEntityId = getEntityId(storageType, tabParams);
  const componentId = idAttribute ?? id;
  const customEndpointTemplate = page.metadata.endpoint as string | undefined;
  const customEndpointDependency =
    storageType === "CUSTOM"
      ? [
          customEndpointTemplate || "",
          ...Object.entries(tabParams)
            .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey))
            .map(([key, value]) => `${key}=${value}`),
        ].join("|")
      : undefined;

  const resolveCustomEndpoint = () => {
    if (!customEndpointTemplate) {
      return undefined;
    }

    return interpolateEndpoint(customEndpointTemplate, tabParams);
  };

  useEffect(() => {
    setProperties(page.properties);
  }, [page.properties]);

  useFetch(
    async () => {
      const params = new URLSearchParams();
      if (providerType === TAB_PROVIDER) {
        Object.entries(tabParams).forEach(([key, value]) => {
          if (value) {
            params.set(key, value);
          }
        });
      } else if (componentId) {
        params.set("componentId", componentId);
      }

      const query = params.toString();
      const resource =
        providerType === TAB_PROVIDER
          ? `ui-extensions/tabs/${providerId}/config`
          : `ui-extensions/pages/${providerId}/config`;
      const response = await fetchWithError(
        joinPath(
          adminClient.baseUrl,
          "admin/realms",
          realmName,
          `${resource}${query ? `?${query}` : ""}`,
        ),
        {
          method: "GET",
          headers: {
            ...getAuthorizationHeaders(await adminClient.getAccessToken()),
            Accept: "application/json",
          },
        },
      );
      return response.json();
    },
    (runtimeProperties) => {
      if (Array.isArray(runtimeProperties) && runtimeProperties.length > 0) {
        setProperties(runtimeProperties);
      }
    },
    [
      providerId,
      providerType,
      realmName,
      resolvedEntityId,
      componentId,
      customEndpointDependency,
    ],
  );

  useEffect(() => {
    setIsLoading(true);
    form.reset({});
  }, [form, idAttribute, resolvedEntityId, customEndpointDependency]);

  useFetch(
    async () => {
      switch (storageType) {
        case "CLIENT":
          if (resolvedEntityId) {
            const attributes = (
              await adminClient.clients.findOne({ id: resolvedEntityId })
            )?.attributes;
            return {
              config: normalizeConfig(
                attributes as Record<string, unknown>,
                properties,
                "load",
                "string-map",
              ),
            };
          }
          return undefined;
        case "USER":
          if (resolvedEntityId) {
            const attributes = (
              await adminClient.users.findOne({ id: resolvedEntityId })
            )?.attributes;
            return {
              config: normalizeConfig(
                attributes as Record<string, unknown>,
                properties,
                "load",
                "list-map",
              ),
            };
          }
          return undefined;
        case "IDENTITY_PROVIDER":
          if (resolvedEntityId) {
            const config = (
              await adminClient.identityProviders.findOne({
                alias: resolvedEntityId,
              })
            )?.config;
            return {
              config: normalizeConfig(
                config as Record<string, unknown>,
                properties,
                "load",
                "string-map",
              ),
            };
          }
          return undefined;
        case "CUSTOM": {
          if (customEndpointTemplate) {
            const endpoint = resolveCustomEndpoint();
            if (!endpoint) {
              return undefined;
            }
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
            componentId
              ? adminClient.components.findOne({ id: componentId })
              : Promise.resolve(),
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
    [
      storageType,
      idAttribute,
      providerId,
      providerType,
      realmName,
      resolvedEntityId,
      customEndpointDependency,
    ],
  );

  const onSubmit = async (formData: ComponentRepresentation) => {
    try {
      const entityId = resolvedEntityId;

      if (
        (isEntityStorageType(storageType) && !entityId) ||
        (storageType === "CUSTOM" && !customEndpointTemplate)
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
            if (!client) {
              throw new Error(`Client not found: ${entityId}`);
            }
            await adminClient.clients.update(
              { id: entityId },
              {
                ...client,
                attributes: mergeEntityConfig(
                  client.attributes as Record<string, unknown>,
                  formData.config as Record<string, unknown>,
                  properties,
                  "string-map",
                ) as typeof client.attributes,
              },
            );
          }
          break;
        case "USER":
          if (entityId) {
            const user = await adminClient.users.findOne({ id: entityId });
            if (!user) {
              throw new Error(`User not found: ${entityId}`);
            }
            await adminClient.users.update(
              { id: entityId },
              {
                ...user,
                attributes: mergeEntityConfig(
                  user.attributes as Record<string, unknown>,
                  formData.config as Record<string, unknown>,
                  properties,
                  "list-map",
                ) as typeof user.attributes,
              },
            );
          }
          break;
        case "IDENTITY_PROVIDER":
          if (entityId) {
            const idp = await adminClient.identityProviders.findOne({
              alias: entityId,
            });
            if (!idp) {
              throw new Error(`Identity provider not found: ${entityId}`);
            }
            await adminClient.identityProviders.update(
              { alias: entityId },
              {
                ...idp,
                config: mergeEntityConfig(
                  idp.config as Record<string, unknown>,
                  formData.config as Record<string, unknown>,
                  properties,
                  "string-map",
                ) as typeof idp.config,
              },
            );
          }
          break;
        case "CUSTOM": {
          if (customEndpointTemplate) {
            const endpoint = resolveCustomEndpoint();
            if (!endpoint) {
              break;
            }
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
          if (componentId) {
            await adminClient.components.update(
              { id: componentId },
              updatedComponent,
            );
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

  if (!canView) {
    return null;
  }

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
          <DynamicComponents properties={properties} />
        </FormProvider>

        <ActionGroup>
          {canManage && (
            <Button data-testid="save" type="submit">
              {t("save")}
            </Button>
          )}
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
