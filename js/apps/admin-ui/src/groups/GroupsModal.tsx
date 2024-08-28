import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormSubmitButton, TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type GroupsModalProps = {
  id?: string;
  rename?: GroupRepresentation;
  duplicate?: GroupRepresentation;
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
  duplicate,
  handleModalToggle,
  refresh,
}: GroupsModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const defaultName = duplicate ? `Copy of ${duplicate.name}` : rename?.name;

  const form = useForm({
    defaultValues: { name: defaultName },
  });
  const { handleSubmit, formState } = form;

  const fetchClientRoleMappings = async (groupId: string) => {
    try {
      const clientRoleMappings: ClientRoleMapping[] = [];
      const clients = await adminClient.clients.find();

      for (const client of clients) {
        const roles = await adminClient.groups.listClientRoleMappings({
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
    parentId?: string,
  ) => {
    try {
      const newGroup = {
        ...sourceGroup,
        name: `Copy of ${sourceGroup.name}`,
      };
      delete newGroup.id;
      const createdGroup = parentId
        ? await adminClient.groups.createChildGroup({ id: parentId }, newGroup)
        : await adminClient.groups.create(newGroup);

      const members = await adminClient.groups.listMembers({
        id: sourceGroup.id!,
      });

      for (const member of members) {
        await adminClient.users.addToGroup({
          id: member.id!,
          groupId: createdGroup.id,
        });
      }

      const permissions = await adminClient.groups.listPermissions({
        id: sourceGroup.id!,
      });

      if (permissions) {
        await adminClient.groups.updatePermission(
          { id: createdGroup.id },
          permissions,
        );
      }

      const realmRoles = await adminClient.groups.listRealmRoleMappings({
        id: sourceGroup.id!,
      });

      const clientRoleMappings = await fetchClientRoleMappings(sourceGroup.id!);

      const realmRolesPayload: RoleMappingPayload[] = realmRoles.map(
        (role) => ({
          id: role.id!,
          name: role.name!,
        }),
      );

      const clientRolesPayload: RoleMappingPayload[] =
        clientRoleMappings.flatMap((clientRoleMapping) =>
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

      const subGroups = await adminClient.groups.listSubGroups({
        parentId: sourceGroup.id!,
      });
      if (subGroups.length > 0) {
        for (const childGroup of subGroups) {
          await duplicateGroup(childGroup, createdGroup.id);
        }
      }

      return createdGroup;
    } catch (error) {
      addError("couldNotDuplicateGroup", error);
      throw error;
    }
  };

  const assignRoles = async (roles: RoleMappingPayload[], groupId: string) => {
    try {
      const realmRoles = roles
        .filter((role) => !role.clientUniqueId && role.name !== undefined)
        .map((role) => ({
          id: role.id,
          name: role.name!,
        }));

      const clientRoles = roles
        .filter((role) => role.clientUniqueId && role.name !== undefined)
        .map((role) => ({
          clientUniqueId: role.clientUniqueId!,
          roles: [{ id: role.id, name: role.name! }],
        }));

      await adminClient.groups.addRealmRoleMappings({
        id: groupId,
        roles: realmRoles,
      });

      await Promise.all(
        clientRoles.map((clientRole) =>
          adminClient.groups.addClientRoleMappings({
            id: groupId,
            clientUniqueId: clientRole.clientUniqueId!,
            roles: clientRole.roles,
          }),
        ),
      );
    } catch (error) {
      addError("roleMappingUpdatedError", error);
    }
  };

  const submitForm = async (group: GroupRepresentation) => {
    group.name = group.name?.trim();

    try {
      if (duplicate) {
        await duplicateGroup(duplicate);
      } else if (!id) {
        await adminClient.groups.create(group);
      } else if (rename) {
        await adminClient.groups.update(
          { id },
          { ...rename, name: group.name },
        );
      } else {
        await (group.id
          ? adminClient.groups.updateChildGroup({ id }, group)
          : adminClient.groups.createChildGroup({ id }, group));
      }

      refresh(rename ? { ...rename, name: group.name } : undefined);
      handleModalToggle();
      addAlert(
        t(
          rename
            ? "groupUpdated"
            : duplicate
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
      title={t(
        rename
          ? "renameAGroup"
          : duplicate
            ? "duplicateAGroup"
            : "createAGroup",
      )}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid={`${rename ? "rename" : duplicate ? "duplicate" : "create"}Group`}
          key="confirm"
          form="group-form"
          allowInvalid
          allowNonDirty
        >
          {t(rename ? "rename" : duplicate ? "duplicate" : "create")}
        </FormSubmitButton>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="group-form" isHorizontal onSubmit={handleSubmit(submitForm)}>
          {duplicate && (
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
        </Form>
      </FormProvider>
    </Modal>
  );
};
