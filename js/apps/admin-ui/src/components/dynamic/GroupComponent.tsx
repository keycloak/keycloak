import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  ActionList,
  ActionListItem,
  Button,
  Label,
  LabelGroup,
  FormGroup,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
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
  const { control, setValue } = useFormContext();
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

  const groupType = useWatch({
    name: groupTypeFieldName,
    control,
    defaultValue: GROUP_TYPE_REALM,
  });

  const shouldRenderOrgField =
    hasLinkedOrganization || groupType == GROUP_TYPE_ORG;
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
            labelHelp={
              <HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />
            }
            fieldId={name!}
            isRequired={required}
          >
            <ActionList>
              <ActionListItem>
                <LabelGroup>
                  {field.value && (
                    <Label
                      onClick={() => {
                        field.onChange(undefined);
                        setValue(groupTypeFieldName, undefined);
                      }}
                    >
                      {shouldRenderOrgField && (
                        <>
                          {groupType === GROUP_TYPE_REALM
                            ? t("realm")
                            : t("organization")}
                          :&nbsp;
                        </>
                      )}
                      {field.value}
                    </Label>
                  )}
                </LabelGroup>
              </ActionListItem>
              <ActionListItem>
                <Button
                  id="kc-join-groups-button"
                  onClick={() => setOpen(true)}
                  data-testid="join-groups-button"
                >
                  {t("selectGroup")}
                </Button>
              </ActionListItem>
              {shouldRenderOrgField && (
                <ActionListItem>
                  <Button
                    id="kc-join-org-groups-button"
                    onClick={() => setOpenOrgGroups(true)}
                    data-testid="join-org-groups-button"
                  >
                    {t("selectOrgGroup")}
                  </Button>
                </ActionListItem>
              )}
            </ActionList>
          </FormGroup>
        </>
      )}
    />
  );
};
