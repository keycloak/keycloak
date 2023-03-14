import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, Controller } from "react-hook-form";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { FormGroup, Button, Checkbox } from "@patternfly/react-core";
import {
  TableComposable,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
} from "@patternfly/react-table";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { HelpItem } from "ui-shared";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { GroupPickerDialog } from "../../../components/group/GroupPickerDialog";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";

type GroupForm = {
  groups?: GroupValue[];
  groupsClaim: string;
};

export type GroupValue = {
  id: string;
  extendChildren: boolean;
};

export const Group = () => {
  const { t } = useTranslation("clients");
  const {
    control,
    register,
    getValues,
    setValue,
    formState: { errors },
  } = useFormContext<GroupForm>();
  const values = getValues("groups");

  const [open, setOpen] = useState(false);
  const [selectedGroups, setSelectedGroups] = useState<GroupRepresentation[]>(
    []
  );

  const { adminClient } = useAdminClient();

  useFetch(
    () => {
      if (values && values.length > 0)
        return Promise.all(
          values.map((g) => adminClient.groups.findOne({ id: g.id }))
        );
      return Promise.resolve([]);
    },
    (groups) => {
      const filteredGroup = groups.filter((g) => g) as GroupRepresentation[];
      setSelectedGroups(filteredGroup);
    },
    []
  );

  return (
    <>
      <FormGroup
        label={t("groupsClaim")}
        labelIcon={
          <HelpItem
            helpText={t("clients-help:groupsClaim")}
            fieldLabelId="clients:groupsClaim"
          />
        }
        fieldId="groups"
      >
        <KeycloakTextInput
          type="text"
          id="groupsClaim"
          data-testid="groupsClaim"
          {...register("groupsClaim")}
        />
      </FormGroup>
      <FormGroup
        label={t("groups")}
        labelIcon={
          <HelpItem
            helpText={t("clients-help:policyGroups")}
            fieldLabelId="clients:groups"
          />
        }
        fieldId="groups"
        helperTextInvalid={t("requiredGroups")}
        validated={errors.groups ? "error" : "default"}
        isRequired
      >
        <Controller
          name="groups"
          control={control}
          defaultValue={[]}
          rules={{
            validate: (value?: GroupValue[]) =>
              value && value.filter(({ id }) => id).length > 0,
          }}
          render={({ field }) => (
            <>
              {open && (
                <GroupPickerDialog
                  type="selectMany"
                  text={{
                    title: "clients:addGroupsToGroupPolicy",
                    ok: "common:add",
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
          <TableComposable variant="compact">
            <Thead>
              <Tr>
                <Th>{t("groups")}</Th>
                <Th>{t("extendToChildren")}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {selectedGroups.map((group, index) => (
                <Tr key={group.id}>
                  <Td>{group.path}</Td>
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
                          isDisabled={group.subGroups?.length === 0}
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
                        setValue("groups", [
                          ...(values || []).filter(({ id }) => id !== group.id),
                        ]);
                        setSelectedGroups([
                          ...selectedGroups.filter(({ id }) => id !== group.id),
                        ]);
                      }}
                    />
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </TableComposable>
        )}
      </FormGroup>
    </>
  );
};
