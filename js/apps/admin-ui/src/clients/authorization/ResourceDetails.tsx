import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import {
  HelpItem,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAccess } from "../../context/access/Access";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { useParams } from "../../utils/useParams";
import { toAuthorizationTab } from "../routes/AuthenticationTab";
import { ResourceDetailsParams, toResourceDetails } from "../routes/Resource";
import { ScopePicker } from "./ScopePicker";
import "./resource-details.css";

type SubmittedResource = Omit<
  ResourceRepresentation,
  "attributes" | "scopes"
> & {
  attributes: KeyValueType[];
};

export default function ResourceDetails() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const [client, setClient] = useState<ClientRepresentation>();
  const [resource, setResource] = useState<ResourceRepresentation>();

  const [permissions, setPermission] =
    useState<ResourceServerRepresentation[]>();

  const { addAlert, addError } = useAlerts();
  const form = useForm<SubmittedResource>({
    mode: "onChange",
  });
  const { setValue, handleSubmit } = form;

  const { id, resourceId, realm } = useParams<ResourceDetailsParams>();
  const navigate = useNavigate();

  const setupForm = (resource: ResourceRepresentation = {}) => {
    convertToFormValues(resource, setValue);
  };

  const { hasAccess } = useAccess();

  const isDisabled = !hasAccess("manage-authorization");

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.findOne({ id }),
        resourceId
          ? adminClient.clients.getResource({ id, resourceId })
          : Promise.resolve(undefined),
        resourceId
          ? adminClient.clients.listPermissionsByResource({ id, resourceId })
          : Promise.resolve(undefined),
      ]),
    ([client, resource, permissions]) => {
      if (!client) {
        throw new Error(t("notFound"));
      }
      setClient(client);
      setPermission(permissions);
      setResource(resource);
      setupForm(resource);
    },
    [],
  );

  const submit = async (submitted: SubmittedResource) => {
    const resource = convertFormValuesToObject<
      SubmittedResource,
      ResourceRepresentation
    >(submitted);

    try {
      if (resourceId) {
        await adminClient.clients.updateResource({ id, resourceId }, resource);
      } else {
        const result = await adminClient.clients.createResource(
          { id },
          resource,
        );
        setResource(resource);
        navigate(toResourceDetails({ realm, id, resourceId: result._id! }));
      }
      addAlert(
        t((resourceId ? "update" : "create") + "ResourceSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("resourceSaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteResource",
    children: (
      <>
        {t("deleteResourceConfirm")}
        {permissions?.length !== 0 && (
          <Alert
            variant="warning"
            isInline
            isPlain
            title={t("deleteResourceWarning")}
            className="pf-v5-u-pt-lg"
          >
            <p className="pf-v5-u-pt-xs">
              {permissions?.map((permission) => (
                <strong key={permission.id} className="pf-v5-u-pr-md">
                  {permission.name}
                </strong>
              ))}
            </p>
          </Alert>
        )}
      </>
    ),
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delResource({
          id,
          resourceId: resourceId!,
        });
        addAlert(t("resourceDeletedSuccess"), AlertVariant.success);
        navigate(toAuthorizationTab({ realm, clientId: id, tab: "resources" }));
      } catch (error) {
        addError("resourceDeletedError", error);
      }
    },
  });

  if (!client) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={resourceId ? resource?.name! : "createResource"}
        dropdownItems={
          resourceId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-resource"
                  isDisabled={isDisabled}
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            role="manage-authorization"
            className="keycloak__resource-details__form"
            onSubmit={handleSubmit(submit)}
          >
            <TextControl
              name={resourceId ? "owner.name" : ""}
              label={t("owner")}
              labelIcon={t("ownerHelp")}
              defaultValue={client.clientId}
              readOnly
            />
            <TextControl
              name={"name"}
              label={t("name")}
              labelIcon={t("resourceNameHelp")}
              rules={{ required: t("required") }}
            />
            <TextControl
              name="displayName"
              label={t("displayName")}
              labelIcon={t("displayNameHelp")}
              rules={{ required: t("required") }}
            />
            <TextControl
              name="type"
              label={t("type")}
              labelIcon={t("resourceDetailsTypeHelp")}
            />
            <FormGroup
              label={t("uris")}
              fieldId="uris"
              labelIcon={
                <HelpItem helpText={t("urisHelp")} fieldLabelId="uris" />
              }
            >
              <MultiLineInput
                name="uris"
                type="url"
                aria-label={t("uris")}
                addButtonLabel="addUri"
              />
            </FormGroup>
            <ScopePicker clientId={id} />
            <TextControl
              name="icon_uri"
              label={t("iconUri")}
              labelIcon={t("iconUriHelp")}
              type="url"
            />
            <DefaultSwitchControl
              name="ownerManagedAccess"
              label={t("ownerManagedAccess")}
              labelIcon={t("ownerManagedAccessHelp")}
            />
            <FormGroup
              hasNoPaddingTop
              label={t("resourceAttribute")}
              labelIcon={
                <HelpItem
                  helpText={t("resourceAttributeHelp")}
                  fieldLabelId="resourceAttribute"
                />
              }
              fieldId="resourceAttribute"
            >
              <KeyValueInput name="attributes" isDisabled={isDisabled} />
            </FormGroup>
            <ActionGroup>
              <div className="pf-v5-u-mt-md">
                <Button
                  variant={ButtonVariant.primary}
                  type="submit"
                  data-testid="save"
                >
                  {t("save")}
                </Button>

                <Button
                  variant="link"
                  data-testid="cancel"
                  component={(props) => (
                    <Link
                      {...props}
                      to={toAuthorizationTab({
                        realm,
                        clientId: id,
                        tab: "resources",
                      })}
                    ></Link>
                  )}
                >
                  {t("cancel")}
                </Button>
              </div>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
