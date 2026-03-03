import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  ActionList,
  ActionListItem,
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import {
  useGroupResource,
  GroupResourceContext,
} from "../../context/group-resource/GroupResourceContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { GroupPickerDialog } from "../group/GroupPickerDialog";
import type { ComponentProps } from "./components";

export const GroupComponent = ({
  name,
  label,
  helpText,
  required,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [openOrgGroups, setOpenOrgGroups] = useState(false);
  const [groups, setGroups] = useState<GroupRepresentation[]>();
  const { control, getValues, setValue, watch } = useFormContext();
  const { adminClient } = useAdminClient();
  const serverInfo = useServerInfo();
  const hasLinkedOrganization = useGroupResource().isOrgGroups();
  const groupTypeFieldName = convertToName("groupType");

  // Get group type enum values from server
  const groupTypes = serverInfo.enums?.["type"] || [];
  const GROUP_TYPE_REALM =
    groupTypes.find((t: string) => t === "REALM") || "REALM";
  const GROUP_TYPE_ORG =
    groupTypes.find((t: string) => t === "ORGANIZATION") || "ORGANIZATION";

  // Watch the groupType field value from the form
  const groupTypeValue = watch(groupTypeFieldName);

  // Set default groupType on mount if needed
  useEffect(() => {
    const groupValue = getValues(convertToName(name!));
    const existingGroupType = getValues(groupTypeFieldName);

    if (!existingGroupType && hasLinkedOrganization && groupValue) {
      // No groupType in loaded data, but there's a group - default to REALM
      setValue(groupTypeFieldName, GROUP_TYPE_REALM);
    }
  }, []); // Run only once on mount

  return (
    <Controller
      name={convertToName(name!)}
      defaultValue=""
      control={control}
      render={({ field }) => (
        <>
          {open && (
            <GroupResourceContext value={adminClient.groups}>
              <GroupPickerDialog
                type="selectOne"
                text={{
                  title: "selectGroup",
                  ok: "select",
                }}
                onConfirm={(groups) => {
                  field.onChange(groups?.[0].path);
                  setValue(groupTypeFieldName, GROUP_TYPE_REALM);
                  setGroups(groups);
                  setOpen(false);
                }}
                onClose={() => setOpen(false)}
                filterGroups={groups}
              />
            </GroupResourceContext>
          )}
          {openOrgGroups && (
            <GroupPickerDialog
              type="selectOne"
              text={{
                title: "selectOrgGroup",
                ok: "select",
              }}
              onConfirm={(groups) => {
                field.onChange(groups?.[0].path);
                setValue(groupTypeFieldName, GROUP_TYPE_ORG);
                setGroups(groups);
                setOpenOrgGroups(false);
              }}
              onClose={() => setOpenOrgGroups(false)}
              filterGroups={groups}
            />
          )}

          <FormGroup
            label={t(label!)}
            labelIcon={
              <HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />
            }
            fieldId={name!}
            isRequired={required}
          >
            <ActionList>
              <ActionListItem>
                <ChipGroup>
                  {field.value && (
                    <Chip
                      onClick={() => {
                        field.onChange(undefined);
                        setValue(groupTypeFieldName, undefined);
                      }}
                    >
                      {field.value}
                    </Chip>
                  )}
                </ChipGroup>
              </ActionListItem>
              <ActionListItem>
                <Button
                  id="kc-join-groups-button"
                  onClick={() => setOpen(true)}
                  variant="secondary"
                  data-testid="join-groups-button"
                >
                  {t("selectGroup")}
                </Button>
              </ActionListItem>
              {hasLinkedOrganization && (
                <ActionListItem>
                  <Button
                    id="kc-join-org-groups-button"
                    onClick={() => setOpenOrgGroups(true)}
                    variant="secondary"
                    data-testid="join-org-groups-button"
                  >
                    {t("selectOrgGroup")}
                  </Button>
                </ActionListItem>
              )}
            </ActionList>
          </FormGroup>
          {field.value &&
            (hasLinkedOrganization || groupTypeValue === GROUP_TYPE_ORG) && (
              <FormGroup
                label={t("groupType")}
                fieldId="groupType"
                labelIcon={
                  <HelpItem
                    helpText={t("groupTypeHelp")}
                    fieldLabelId="groupType"
                  />
                }
              >
                <TextInput
                  id="groupType"
                  value={groupTypeValue || GROUP_TYPE_REALM}
                  readOnly
                />
              </FormGroup>
            )}
        </>
      )}
    />
  );
};
