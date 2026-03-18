import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  FormSubmitButton,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useGroupResource } from "../context/group-resource/GroupResourceContext";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";

type GroupsModalProps = {
  id?: string;
  rename?: GroupRepresentation;
  duplicateId?: string;
  handleModalToggle: () => void;
  refresh: (group?: GroupRepresentation) => void;
};

type RoleMappingPayload = {
  id: string;
  name?: string;
  clientUniqueId?: string;
};

type ClientRoleMapping = {
  clientId: string;
  roles: RoleMappingPayload[];
};

export const GroupsModal = ({
  id,
  rename,
  duplicateId,
  handleModalToggle,
  refresh,
}: GroupsModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const groups = useGroupResource();
  const { addAlert, addError } = useAlerts();
  const isFeatureEnabled = useIsFeatureEnabled();
  const [duplicateGroupDetails, setDuplicateGroupDetails] =
    useState<GroupRepresentation | null>(null);

  const form = useForm({
    defaultValues: {
      name: rename?.name || "",
      description: rename?.description || "",
    },
  });
  const { handleSubmit, formState } = form;

  useFetch(
    async () => {
      if (duplicateId) {
        return groups.findOne({ id: duplicateId });
      }
    },
    (group) => {
      if (group) {
        setDuplicateGroupDetails(group);
        form.reset({ name: t("copyOf", { name: group.name }) });
      }
    },
    [duplicateId],
  );

  const fetchClientRoleMappings = async (groupId: string) => {
    try {
      const clientRoleMappings: ClientRoleMapping[] = [];
      const clients = await adminClient.clients.find();

      for (const client of clients) {
        const roles = await groups.listClientRoleMappings({
          id: groupId,
          clientUniqueId: client.id!,
        });

        const clientRoles = roles
          .filter((role) => role.id && role.name)
          .map((role) => ({
            id: role.id!,
            name: role.name!,
          }));

        if (clientRoles.length > 0) {
          clientRoleMappings.push({ clientId: client.id!, roles: clientRoles });
        }
      }

      return clientRoleMappings;
    } catch (error) {
      addError("couldNotFetchClientRoleMappings", error);
      throw error;
    }
  };

  const duplicateGroup = async (
    sourceGroup: GroupRepresentation,
    newName?: string,
    parentId?: string,
  ) => {
    try {
      const newGroup: GroupRepresentation = {
        ...sourceGroup,
        name: newName ?? sourceGroup.name!,
        ...(parentId ? {} : { attributes: duplicateGroupDetails?.attributes }),
      };

      delete newGroup.id;

      const createdGroup = parentId
        ? await groups.createChildGroup({ id: parentId }, newGroup)
        : await groups.create(newGroup);

      const members = await groups.listMembers({
        id: sourceGroup.id!,
      });

      for (const member of members) {
        if (!groups.isOrgGroups()) {
          await adminClient.users.addToGroup({
            id: member.id!,
            groupId: createdGroup.id,
          });
        } else {
          await groups.addMemberToOrgGroup({
            groupId: createdGroup.id,
            userId: member.id!,
          });
        }
      }

      if (
        !groups.isOrgGroups() &&
        isFeatureEnabled(Feature.AdminFineGrainedAuthz)
      ) {
        const permissions = await groups.listPermissions({
          id: sourceGroup.id!,
        });

        if (permissions) {
          await groups.updatePermission({ id: createdGroup.id }, permissions);
        }
      }

      if (!groups.isOrgGroups()) {
        const realmRoles = await groups.listRealmRoleMappings({
          id: sourceGroup.id!,
        });

        const realmRolesPayload: RoleMappingPayload[] = realmRoles.map(
          (role) => ({ id: role.id!, name: role.name! }),
        );

        const clientRoleMappings = await fetchClientRoleMappings(
          sourceGroup.id!,
        );

        const clientRolesPayload: RoleMappingPayload[] =
          clientRoleMappings?.flatMap((clientRoleMapping) =>
            clientRoleMapping.roles.map((role) => ({
              id: role.id!,
              name: role.name!,
              clientUniqueId: clientRoleMapping.clientId,
            })),
          );

        const rolesToAssign: RoleMappingPayload[] = [
          ...realmRolesPayload,
          ...clientRolesPayload,
        ];

        await assignRoles(rolesToAssign, createdGroup.id);
      }

      const subGroups = await groups.listSubGroups({
        parentId: sourceGroup.id!,
      });

      for (const childGroup of subGroups) {
        await duplicateGroup(childGroup, undefined, createdGroup.id);
      }

      return createdGroup;
    } catch (error) {
      addError("couldNotDuplicateGroup", error);
      throw error;
    }
  };

  const assignRoles = async (roles: RoleMappingPayload[], groupId: string) => {
    try {
      const realmRoles = roles.filter(
        (role) => !role.clientUniqueId && role.name,
      );
      const clientRoles = roles.filter(
        (role) => role.clientUniqueId && role.name,
      );

      await groups.addRealmRoleMappings({
        id: groupId,
        roles: realmRoles.map(({ id, name }) => ({ id, name: name! })),
      });

      await Promise.all(
        clientRoles.map((clientRole) => {
          if (clientRole.clientUniqueId && clientRole.name) {
            return groups.addClientRoleMappings({
              id: groupId,
              clientUniqueId: clientRole.clientUniqueId,
              roles: [{ id: clientRole.id, name: clientRole.name }],
            });
          }
          return Promise.resolve();
        }),
      );
    } catch (error) {
      addError("roleMappingUpdatedError", error);
    }
  };

  const submitForm = async (group: GroupRepresentation) => {
    group.name = group.name?.trim();

    try {
      if (duplicateId && duplicateGroupDetails) {
        await duplicateGroup(duplicateGroupDetails, group.name);
      } else if (!id) {
        await groups.create(group);
      } else if (rename) {
        await groups.update(
          { id },
          { ...rename, name: group.name, description: group.description },
        );
      } else {
        await groups.updateChildGroup({ id }, group);
      }

      refresh(rename ? { ...rename, ...group } : undefined);
      handleModalToggle();
      addAlert(
        t(
          rename
            ? "groupUpdated"
            : duplicateId
              ? "groupDuplicated"
              : "groupCreated",
        ),
        AlertVariant.success,
      );
    } catch (error) {
      addError("couldNotCreateGroup", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={
        rename
          ? t("editGroup")
          : duplicateId
            ? t("duplicateAGroup")
            : t("createAGroup")
      }
      isOpen
      onClose={handleModalToggle}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid={`${rename ? "rename" : duplicateId ? "duplicate" : "create"}Group`}
          key="confirm"
          form="group-form"
          allowInvalid
          allowNonDirty
        >
          {t(rename ? "edit" : duplicateId ? "duplicate" : "create")}
        </FormSubmitButton>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={handleModalToggle}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="group-form" isHorizontal onSubmit={handleSubmit(submitForm)}>
          {duplicateId && (
            <Alert
              variant="warning"
              component="h2"
              isInline
              title={t("duplicateGroupWarning")}
            />
          )}
          <TextControl
            name="name"
            label={t("name")}
            rules={{ required: t("required") }}
            autoFocus
          />
          <TextControl name="description" label={t("description")} />
        </Form>
      </FormProvider>
    </Modal>
  );
};
