import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { HelpItem, TextControl, useFetch } from "@keycloak/keycloak-ui-shared";
import { Button, Checkbox, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { MissingPolicyReference } from "../../../components/MissingPolicyReference";
import { GroupPickerDialog } from "../../../components/group/GroupPickerDialog";
import { GroupResourceContext } from "../../../context/group-resource/GroupResourceContext";

type GroupForm = {
  groups?: GroupValue[];
  groupsClaim: string;
};

export type GroupValue = {
  id?: string | null;
  path?: string | null;
  extendChildren: boolean;
};

type SelectedGroup = GroupRepresentation & { unresolvedPath?: string };

export const Group = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { control, getValues, setValue } = useFormContext<GroupForm>();
  const values = getValues("groups");

  const [open, setOpen] = useState(false);
  const [selectedGroups, setSelectedGroups] = useState<SelectedGroup[]>([]);

  useFetch(
    () => {
      if (values && values.length > 0)
        return Promise.all(
          values.map(async ({ id, path }): Promise<SelectedGroup> => {
            if (!id) {
              return { unresolvedPath: path ?? undefined, subGroupCount: 0 };
            }
            return (
              (await adminClient.groups.findOne({ id })) ?? {
                id,
                subGroupCount: 0,
              }
            );
          }),
        );
      return Promise.resolve([]);
    },
    setSelectedGroups,
    [],
  );

  return (
    <>
      <TextControl
        name="groupsClaim"
        label={t("groupsClaim")}
        labelIcon={t("groupsClaimHelp")}
      />
      <FormGroup
        label={t("groups")}
        labelIcon={
          <HelpItem helpText={t("policyGroupsHelp")} fieldLabelId="groups" />
        }
        fieldId="groups"
      >
        <Controller
          name="groups"
          control={control}
          defaultValue={[]}
          render={({ field }) => (
            <>
              {open && (
                <GroupResourceContext value={adminClient.groups}>
                  <GroupPickerDialog
                    type="selectMany"
                    text={{
                      title: "addGroupsToGroupPolicy",
                      ok: "add",
                    }}
                    onConfirm={(groups) => {
                      field.onChange([
                        ...(field.value || []),
                        ...(groups || []).map(({ id }) => ({ id })),
                      ]);
                      setSelectedGroups([...selectedGroups, ...(groups || [])]);
                      setOpen(false);
                    }}
                    onClose={() => {
                      setOpen(false);
                    }}
                    filterGroups={selectedGroups}
                  />
                </GroupResourceContext>
              )}
              <Button
                data-testid="select-group-button"
                variant="secondary"
                onClick={() => {
                  setOpen(true);
                }}
              >
                {t("addGroups")}
              </Button>
            </>
          )}
        />
        {selectedGroups.length > 0 && (
          <Table variant="compact">
            <Thead>
              <Tr>
                <Th>{t("groups")}</Th>
                <Th>{t("extendToChildren")}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {selectedGroups.map((group, index) => (
                <Tr key={group.id ?? group.unresolvedPath}>
                  <Td>
                    {group.path ?? (
                      <MissingPolicyReference
                        id={group.id ?? group.unresolvedPath}
                        label={t("missingGroup")}
                      />
                    )}
                  </Td>
                  <Td>
                    <Controller
                      name={`groups.${index}.extendChildren`}
                      defaultValue={false}
                      control={control}
                      render={({ field }) => (
                        <Checkbox
                          id="extendChildren"
                          data-testid="standard"
                          name="extendChildren"
                          isChecked={field.value}
                          onChange={field.onChange}
                          isDisabled={group.subGroupCount === 0}
                        />
                      )}
                    />
                  </Td>
                  <Td>
                    <Button
                      variant="link"
                      className="keycloak__client-authorization__policy-row-remove"
                      icon={<MinusCircleIcon />}
                      onClick={() => {
                        setValue(
                          "groups",
                          (getValues("groups") || []).filter(
                            (_, position) => position !== index,
                          ),
                        );
                        setSelectedGroups(
                          selectedGroups.filter(
                            (_, position) => position !== index,
                          ),
                        );
                      }}
                    />
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </FormGroup>
    </>
  );
};
