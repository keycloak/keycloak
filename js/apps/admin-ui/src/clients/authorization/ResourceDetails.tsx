import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { useFetch } from "../../utils/useFetch";
import { useParams } from "../../utils/useParams";
import { toAuthorizationTab } from "../routes/AuthenticationTab";
import { ResourceDetailsParams, toResourceDetails } from "../routes/Resource";
import { ScopePicker } from "./ScopePicker";

import "./resource-details.css";
import { useAccess } from "../../context/access/Access";

type SubmittedResource = Omit<
  ResourceRepresentation,
  "attributes" | "scopes"
> & {
  attributes: KeyValueType[];
};

export default function ResourceDetails() {
  const { t } = useTranslation();
  const [client, setClient] = useState<ClientRepresentation>();
  const [resource, setResource] = useState<ResourceRepresentation>();

  const [permissions, setPermission] =
    useState<ResourceServerRepresentation[]>();

  const { addAlert, addError } = useAlerts();
  const form = useForm<SubmittedResource>({
    mode: "onChange",
  });
  const {
    register,
    formState: { errors },
    control,
    setValue,
    handleSubmit,
  } = form;

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
            className="pf-u-pt-lg"
          >
            <p className="pf-u-pt-xs">
              {permissions?.map((permission) => (
                <strong key={permission.id} className="pf-u-pr-md">
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
            <FormGroup
              label={t("owner")}
              fieldId="owner"
              labelIcon={
                <HelpItem helpText={t("ownerHelp")} fieldLabelId="owner" />
              }
            >
              <KeycloakTextInput
                id="owner"
                value={client.clientId}
                isReadOnly
              />
            </FormGroup>
            <FormGroup
              label={t("name")}
              fieldId="name"
              labelIcon={
                <HelpItem
                  helpText={t("resourceNameHelp")}
                  fieldLabelId="name"
                />
              }
              helperTextInvalid={t("required")}
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
              isRequired
            >
              <KeycloakTextInput
                id="name"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                {...register("name", { required: true })}
              />
            </FormGroup>
            <FormGroup
              label={t("displayName")}
              fieldId="displayName"
              labelIcon={
                <HelpItem helpText={t("displayNameHelp")} fieldLabelId="name" />
              }
            >
              <KeycloakTextInput
                id="displayName"
                {...register("displayName")}
              />
            </FormGroup>
            <FormGroup
              label={t("type")}
              fieldId="type"
              labelIcon={
                <HelpItem
                  helpText={t("resourceDetailsTypeHelp")}
                  fieldLabelId="type"
                />
              }
            >
              <KeycloakTextInput id="type" {...register("type")} />
            </FormGroup>
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
            <FormGroup
              label={t("iconUri")}
              fieldId="iconUri"
              labelIcon={
                <HelpItem helpText={t("iconUriHelp")} fieldLabelId="iconUri" />
              }
            >
              <KeycloakTextInput
                id="iconUri"
                type="url"
                {...register("icon_uri")}
              />
            </FormGroup>
            <FormGroup
              hasNoPaddingTop
              label={t("ownerManagedAccess")}
              labelIcon={
                <HelpItem
                  helpText={t("ownerManagedAccessHelp")}
                  fieldLabelId="ownerManagedAccess"
                />
              }
              fieldId="ownerManagedAccess"
            >
              <Controller
                name="ownerManagedAccess"
                control={control}
                defaultValue={false}
                render={({ field }) => (
                  <Switch
                    id="ownerManagedAccess"
                    label={t("on")}
                    labelOff={t("off")}
                    isChecked={field.value}
                    onChange={field.onChange}
                    aria-label={t("ownerManagedAccess")}
                  />
                )}
              />
            </FormGroup>

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
              <div className="pf-u-mt-md">
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
