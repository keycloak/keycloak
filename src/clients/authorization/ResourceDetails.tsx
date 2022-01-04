import React, { useState } from "react";
import { Link, useHistory, useParams } from "react-router-dom";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
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
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { ResourceDetailsParams, toResourceDetails } from "../routes/Resource";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import type { MultiLine } from "../../components/multi-line-input/multi-line-convert";
import type { KeyValueType } from "../../components/attribute-form/attribute-convert";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { toClient } from "../routes/Client";
import { ScopePicker } from "./ScopePicker";
import { AttributeInput } from "../../components/attribute-input/AttributeInput";

import "./resource-details.css";

type SubmittedResource = Omit<ResourceRepresentation, "attributes" | "uris"> & {
  attributes: KeyValueType[];
  uris: MultiLine[];
};

export default function ResourceDetails() {
  const { t } = useTranslation("clients");
  const [client, setClient] = useState<ClientRepresentation>();
  const [resource, setResource] = useState<ResourceRepresentation>();

  const [permissions, setPermission] =
    useState<ResourceServerRepresentation[]>();

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const form = useForm<SubmittedResource>({
    shouldUnregister: false,
    mode: "onChange",
  });
  const { register, errors, control, setValue, handleSubmit } = form;

  const { id, resourceId, realm } = useParams<ResourceDetailsParams>();
  const history = useHistory();

  const setupForm = (resource: ResourceRepresentation = {}) => {
    convertToFormValues(resource, setValue, ["uris"]);
  };

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
        throw new Error(t("common:notFound"));
      }
      setClient(client);
      setPermission(permissions);
      setResource(resource);
      setupForm(resource);
    },
    []
  );

  const save = async (submitted: SubmittedResource) => {
    const resource = convertFormValuesToObject(submitted, ["uris"]);

    try {
      if (resourceId) {
        await adminClient.clients.updateResource({ id, resourceId }, resource);
      } else {
        const result = await adminClient.clients.createResource(
          { id },
          resource
        );
        history.push(toResourceDetails({ realm, id, resourceId: result._id! }));
      }
      addAlert(
        t((resourceId ? "update" : "create") + "ResourceSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("clients:resourceSaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:deleteResource",
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
    continueButtonLabel: "clients:confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delResource({
          id,
          resourceId: resourceId!,
        });
        addAlert(t("resourceDeletedSuccess"), AlertVariant.success);
        history.push(toClient({ realm, clientId: id, tab: "authorization" }));
      } catch (error) {
        addError("clients:resourceDeletedError", error);
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
        titleKey={resourceId ? resource?.name! : "clients:createResource"}
        dropdownItems={
          resourceId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-resource"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            role="manage-clients"
            className="keycloak__resource-details__form"
            onSubmit={handleSubmit(save)}
          >
            <FormGroup
              label={t("owner")}
              fieldId="owner"
              labelIcon={
                <HelpItem
                  helpText="clients-help:owner"
                  fieldLabelId="clients:owner"
                />
              }
            >
              <TextInput id="owner" value={client.clientId} isReadOnly />
            </FormGroup>
            <FormGroup
              label={t("common:name")}
              fieldId="name"
              labelIcon={
                <HelpItem
                  helpText="clients-help:resourceName"
                  fieldLabelId="name"
                />
              }
              helperTextInvalid={t("common:required")}
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
              isRequired
            >
              <TextInput
                id="name"
                name="name"
                ref={register({ required: true })}
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
            <FormGroup
              label={t("displayName")}
              fieldId="displayName"
              labelIcon={
                <HelpItem
                  helpText="clients-help:displayName"
                  fieldLabelId="name"
                />
              }
            >
              <TextInput id="displayName" name="name" ref={register} />
            </FormGroup>
            <FormGroup
              label={t("type")}
              fieldId="type"
              labelIcon={
                <HelpItem helpText="clients-help:type" fieldLabelId="type" />
              }
            >
              <TextInput id="type" name="type" ref={register} />
            </FormGroup>
            <FormGroup
              label={t("uris")}
              fieldId="uris"
              labelIcon={
                <HelpItem
                  helpText="clients-help:uris"
                  fieldLabelId="clients:uris"
                />
              }
            >
              <MultiLineInput
                name="uris"
                aria-label={t("uri")}
                addButtonLabel="clients:addUri"
              />
            </FormGroup>
            <ScopePicker clientId={id} />
            <FormGroup
              label={t("iconUri")}
              fieldId="iconUri"
              labelIcon={
                <HelpItem
                  helpText="clients-help:iconUri"
                  fieldLabelId="clients:iconUri"
                />
              }
            >
              <TextInput id="iconUri" name="icon_uri" ref={register} />
            </FormGroup>
            <FormGroup
              hasNoPaddingTop
              label={t("ownerManagedAccess")}
              labelIcon={
                <HelpItem
                  helpText="clients-help:ownerManagedAccess"
                  fieldLabelId="clients:ownerManagedAccess"
                />
              }
              fieldId="ownerManagedAccess"
            >
              <Controller
                name="ownerManagedAccess"
                control={control}
                defaultValue={false}
                render={({ onChange, value }) => (
                  <Switch
                    id="ownerManagedAccess"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormGroup>

            <FormGroup
              hasNoPaddingTop
              label={t("resourceAttribute")}
              labelIcon={
                <HelpItem
                  helpText="clients-help:resourceAttribute"
                  fieldLabelId="clients:resourceAttribute"
                />
              }
              fieldId="resourceAttribute"
            >
              <AttributeInput name="attributes" />
            </FormGroup>
            <ActionGroup>
              <div className="pf-u-mt-md">
                <Button
                  variant={ButtonVariant.primary}
                  type="submit"
                  data-testid="save"
                >
                  {t("common:save")}
                </Button>

                <Button
                  variant="link"
                  data-testid="cancel"
                  component={(props) => (
                    <Link
                      {...props}
                      to={toClient({
                        realm,
                        clientId: id,
                        tab: "authorization",
                      })}
                    ></Link>
                  )}
                >
                  {t("common:cancel")}
                </Button>
              </div>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
