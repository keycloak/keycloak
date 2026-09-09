import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormSubmitButton, HelpItem } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  ToggleGroup,
  ToggleGroupItem,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { IdentityProviderSelect } from "./IdentityProviderSelect";

type LinkIdentityProviderModalProps = {
  orgId: string;
  identityProvider?: IdentityProviderRepresentation;
  onClose: () => void;
};

type LinkRepresentation = {
  alias: string[] | string;
  autoMembership: boolean;
  membershipType: string;
};

export const LinkIdentityProviderModal = ({
  orgId,
  identityProvider,
  onClose,
}: LinkIdentityProviderModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const form = useForm<LinkRepresentation>({ mode: "onChange" });
  const { handleSubmit, formState, setValue, control } = form;

  const autoMembership = useWatch({ control, name: "autoMembership" });
  const [managedClaimed, setManagedClaimed] = useState(false);

  const hasManagedByOtherOrg = (idp: IdentityProviderRepresentation) =>
    idp.organizationLinks?.some(
      (l) => l.organizationId !== orgId && l.membershipType === "MANAGED",
    ) ?? false;

  useEffect(() => {
    if (!autoMembership || managedClaimed) {
      setValue("membershipType", "UNMANAGED");
    }
  }, [autoMembership, managedClaimed]);

  useEffect(() => {
    if (identityProvider) {
      setValue("alias", [identityProvider.alias!]);
      setManagedClaimed(hasManagedByOtherOrg(identityProvider));
      const link = identityProvider.organizationLinks?.find(
        (l) => l.organizationId === orgId,
      );
      if (link) {
        setValue("autoMembership", link.autoMembership ?? true);
        setValue("membershipType", link.membershipType ?? "UNMANAGED");
      }
    }
  }, []);

  const submitForm = async (data: LinkRepresentation) => {
    try {
      const alias = Array.isArray(data.alias) ? data.alias[0] : data.alias;

      if (!identityProvider) {
        await adminClient.organizations.linkIdp({
          orgId,
          alias,
        });
      }

      await adminClient.organizations.updateIdentityProviderLink(
        { orgId, alias },
        {
          autoMembership: data.autoMembership,
          membershipType: data.membershipType,
        },
      );

      addAlert(
        t(!identityProvider ? "linkSuccessful" : "linkUpdatedSuccessful"),
      );
      onClose();
    } catch (error) {
      addError(!identityProvider ? "linkError" : "linkUpdatedError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("linkIdentityProvider")}
      isOpen
      onClose={onClose}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid="confirm"
          key="confirm"
          form="form"
          allowInvalid
          allowNonDirty
        >
          {t("save")}
        </FormSubmitButton>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="form" onSubmit={handleSubmit(submitForm)}>
          {identityProvider ? (
            <FormGroup
              label={t("identityProvider")}
              fieldId="identityProvider"
            >
              {identityProvider.alias}
            </FormGroup>
          ) : (
            <IdentityProviderSelect
              name="alias"
              label={t("identityProvider")}
              helpText={t("linkIdentityProviderHelp")}
              defaultValue={[]}
              isRequired
              orgId={orgId}
              onIdpSelected={(idp) => setManagedClaimed(hasManagedByOtherOrg(idp))}
            />
          )}
          <DefaultSwitchControl
            name="autoMembership"
            label={t("autoMembership")}
            labelIcon={t("autoMembershipHelp")}
            defaultValue={true}
          />
          <FormGroup
            label={t("membershipType")}
            labelIcon={
              <HelpItem
                helpText={t("membershipTypeHelp")}
                fieldLabelId="membershipType"
              />
            }
            fieldId="membershipType"
          >
            <Controller
              name="membershipType"
              defaultValue="UNMANAGED"
              control={control}
              render={({ field }) => (
                <ToggleGroup aria-label={t("membershipType")}>
                  <ToggleGroupItem
                    text={t("UNMANAGED")}
                    buttonId="unmanaged"
                    isSelected={field.value === "UNMANAGED"}
                    onChange={() => field.onChange("UNMANAGED")}
                  />
                  {autoMembership && !managedClaimed && (
                    <ToggleGroupItem
                      text={t("MANAGED")}
                      buttonId="managed"
                      isSelected={field.value === "MANAGED"}
                      onChange={() => field.onChange("MANAGED")}
                    />
                  )}
                </ToggleGroup>
              )}
            />
          </FormGroup>
        </Form>
      </FormProvider>
    </Modal>
  );
};
