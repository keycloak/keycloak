import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useParams } from "../../utils/useParams";
import {
  RegistrationProviderParams,
  toRegistrationProvider,
} from "../routes/AddRegistrationProvider";
import { toClientRegistration } from "../routes/ClientRegistration";

export default function DetailProvider() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { id, providerId, subTab } = useParams<RegistrationProviderParams>();
  const navigate = useNavigate();
  const form = useForm<ComponentRepresentation>({
    defaultValues: { providerId },
  });
  const { control, handleSubmit, reset } = form;

  const { realm, realmRepresentation } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [provider, setProvider] = useState<ComponentTypeRepresentation>();

  useFetch(
    async () =>
      await Promise.all([
        adminClient.realms.getClientRegistrationPolicyProviders({ realm }),
        id ? adminClient.components.findOne({ id }) : Promise.resolve(),
      ]),
    ([providers, data]) => {
      setProvider(providers.find((p) => p.id === providerId));
      reset(data || { providerId });
    },
    [],
  );

  const providerName = useWatch({ control, defaultValue: "", name: "name" });

  const onSubmit = async (component: ComponentRepresentation) => {
    if (component.config)
      Object.entries(component.config).forEach(
        ([key, value]) =>
          (component.config![key] = Array.isArray(value) ? value : [value]),
      );
    try {
      const updatedComponent = {
        ...component,
        subType: subTab,
        parentId: realmRepresentation?.id,
        providerType:
          "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy",
        providerId,
      };
      if (id) {
        await adminClient.components.update({ id }, updatedComponent);
      } else {
        const { id } = await adminClient.components.create(updatedComponent);
        navigate(toRegistrationProvider({ id, realm, subTab, providerId }));
      }
      addAlert(t(`provider${id ? "Updated" : "Create"}Success`));
    } catch (error) {
      addError(`provider${id ? "Updated" : "Create"}Error`, error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clientRegisterPolicyDeleteConfirmTitle",
    messageKey: t("clientRegisterPolicyDeleteConfirm", {
      name: providerName,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          realm,
          id: id!,
        });
        addAlert(t("clientRegisterPolicyDeleteSuccess"));
        navigate(toClientRegistration({ realm, subTab }));
      } catch (error) {
        addError("clientRegisterPolicyDeleteError", error);
      }
    },
  });

  if (!provider) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader
        titleKey={id ? providerName! : "createPolicy"}
        subKey={id}
        dropdownItems={
          id
            ? [
                <DropdownItem
                  data-testid="delete"
                  key="delete"
                  onClick={toggleDeleteDialog}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <DeleteConfirm />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            role="manage-clients"
            isHorizontal
            onSubmit={handleSubmit(onSubmit)}
          >
            <TextControl name="providerId" label={t("provider")} readOnly />
            <TextControl
              name="name"
              label={t("name")}
              labelIcon={t("clientPolicyNameHelp")}
              rules={{ required: t("required") }}
            />
            <DynamicComponents properties={provider.properties} />
            <ActionGroup>
              <Button data-testid="save" type="submit">
                {t("save")}
              </Button>
              <Button
                data-testid="cancel"
                variant="link"
                component={(props) => (
                  <Link
                    {...props}
                    to={toClientRegistration({ realm, subTab })}
                  ></Link>
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
