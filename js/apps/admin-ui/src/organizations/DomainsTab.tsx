import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import OrganizationDomainRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationDomainRepresentation";
import {
  FormSubmitButton,
  KeycloakDataTable,
  ListEmptyState,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { EditOrganizationParams } from "./routes/EditOrganization";

type DomainModalProps = {
  orgId: string;
  domain?: OrganizationDomainRepresentation;
  onClose: () => void;
};

const DomainModal = ({ orgId, domain, onClose }: DomainModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const [linkedIdps, setLinkedIdps] = useState<
    IdentityProviderRepresentation[]
  >([]);

  useFetch(
    () => adminClient.organizations.listIdentityProviders({ orgId }),
    setLinkedIdps,
    [],
  );
  const isEdit = !!domain;
  const form = useForm<OrganizationDomainRepresentation>({
    mode: "onChange",
    defaultValues: {
      name: domain?.name,
      verified: domain?.verified ?? false,
      autoRedirect: domain?.autoRedirect ?? false,
      identityProviderAlias: domain?.identityProviderAlias ?? "",
    },
  });
  const { handleSubmit, formState } = form;

  const submitForm = async (data: OrganizationDomainRepresentation) => {
    try {
      const org = await adminClient.organizations.findOne({ id: orgId });
      const domains = org.domains ?? [];
      const payload = {
        verified: data.verified,
        autoRedirect: data.autoRedirect,
        identityProviderAlias: data.identityProviderAlias || undefined,
      };
      if (isEdit) {
        const target = domains.find((d) => d.name === domain.name);
        if (target) Object.assign(target, payload);
      } else {
        const name = data.name?.trim();
        if (domains.some((d) => d.name === name)) {
          addError("addDomainError", t("duplicateDomain"));
          return;
        }
        domains.push({ name, ...payload });
      }
      await adminClient.organizations.updateById(
        { id: orgId },
        { ...org, domains },
      );
      addAlert(t(isEdit ? "domainUpdatedSuccess" : "addDomainSuccess"));
      onClose();
    } catch (error) {
      addError(isEdit ? "domainUpdatedError" : "addDomainError", error);
    }
  };

  const idpOptions = [
    { key: "", value: t("none") },
    ...linkedIdps.map((idp) => ({
      key: idp.alias!,
      value: idp.alias!,
    })),
  ];

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(isEdit ? "editDomain" : "addDomain")}
      isOpen
      onClose={onClose}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid="confirm"
          key="confirm"
          form="domain-form"
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
        <Form id="domain-form" onSubmit={handleSubmit(submitForm)}>
          {isEdit ? (
            <FormGroup label={t("domain")} fieldId="domain-name">
              <span id="domain-name" data-testid="domain-name">
                {domain.name}
              </span>
            </FormGroup>
          ) : (
            <TextControl
              name="name"
              label={t("domain")}
              labelIcon={t("domainHelp")}
              rules={{ required: t("required") }}
            />
          )}
          <SelectControl
            name="identityProviderAlias"
            label={t("identityProvider")}
            labelIcon={t("domainIdentityProviderHelp")}
            options={idpOptions}
            controller={{ defaultValue: "" }}
          />
          <DefaultSwitchControl
            name="autoRedirect"
            label={t("autoRedirect")}
            labelIcon={t("autoRedirectHelp")}
            defaultValue={false}
          />
        </Form>
      </FormProvider>
    </Modal>
  );
};

export const DomainsTab = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { id: orgId } = useParams<EditOrganizationParams>();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [domainModalOpen, setDomainModalOpen] = useState(false);
  const [editingDomain, setEditingDomain] =
    useState<OrganizationDomainRepresentation>();
  const [selectedDomain, setSelectedDomain] =
    useState<OrganizationDomainRepresentation>();

  const loader = async () => {
    const org = await adminClient.organizations.findOne({ id: orgId! });
    return org.domains ?? [];
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "removeDomain",
    messageKey: "removeDomainConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        const org = await adminClient.organizations.findOne({ id: orgId! });
        const domains = (org.domains ?? []).filter(
          (d) => d.name !== selectedDomain?.name,
        );
        await adminClient.organizations.updateById(
          { id: orgId! },
          { ...org, domains },
        );
        setSelectedDomain(undefined);
        addAlert(t("domainRemovedSuccess"));
        refresh();
      } catch (error) {
        addError("domainRemovedError", error);
      }
    },
  });

  return (
    <PageSection variant="light">
      <DeleteConfirm />
      {domainModalOpen && (
        <DomainModal
          orgId={orgId!}
          domain={editingDomain}
          onClose={() => {
            setDomainModalOpen(false);
            setEditingDomain(undefined);
            refresh();
          }}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="domains"
        searchPlaceholderKey="searchDomain"
        toolbarItem={
          <ToolbarItem>
            <Button onClick={() => setDomainModalOpen(true)}>
              {t("addDomain")}
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("edit"),
            onRowClick: (row) => {
              setEditingDomain(row);
              setDomainModalOpen(true);
            },
          },
          {
            title: t("delete"),
            onRowClick: (row) => {
              setSelectedDomain(row);
              toggleDeleteDialog();
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "domain",
            cellRenderer: (row) => (
              <Button
                variant="link"
                isInline
                onClick={() => {
                  setEditingDomain(row);
                  setDomainModalOpen(true);
                }}
              >
                {row.name}
              </Button>
            ),
          },
          {
            name: "identityProviderAlias",
            displayKey: "identityProvider",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "autoRedirect",
            displayKey: "autoRedirect",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyDomains")}
            instructions={t("emptyDomainsInstructions")}
            primaryActionText={t("addDomain")}
            onPrimaryAction={() => setDomainModalOpen(true)}
          />
        }
      />
    </PageSection>
  );
};
