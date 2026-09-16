import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  ActionList,
  ActionListItem,
  Button,
  Chip,
  ChipGroup,
  EmptyState,
  EmptyStateBody,
  EmptyStateFooter,
  Flex,
  FormGroup,
} from "@patternfly/react-core";
import { useContext, useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import {
  GroupsResourceContext,
  GroupResourceContext,
} from "../../context/group-resource/GroupResourceContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { GroupPickerDialog } from "../group/GroupPickerDialog";
import type { ComponentProps } from "./components";
import { PlusCircleIcon } from "@patternfly/react-icons";

export const GroupListComponent = ({
  name,
  label,
  helpText,
  required,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [openOrgGroups, setOpenOrgGroups] = useState(false);
  const [groups, setGroups] = useState<GroupRepresentation[]>([]);
  const { control, setValue } = useFormContext();
  const { adminClient } = useAdminClient();
  const serverInfo = useServerInfo();
  const groupResource = useContext(GroupsResourceContext);
  const hasLinkedOrganization = groupResource?.isOrgGroups() ?? false;
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
    groupResource && (hasLinkedOrganization || groupType == GROUP_TYPE_ORG);

  const toPathString = (groups: GroupRepresentation[]) => {
    return groups
      .map((g) => g.path)
      .filter(Boolean)
      .join(",");
  };

  const removeGroupFromField = (path: string, fieldValue: string) => {
    const paths: string[] = fieldValue.split(",");
    return paths.filter((p) => p !== path).join(",");
  };

  const addGroups = (selected: GroupRepresentation[]) => {
    setGroups((prevState) => [...prevState, ...selected]);
  };

  const removeGroup = (path: string) => {
    setGroups((prevState) => {
      return prevState.filter((g) => g.path !== path);
    });
  };

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <Controller
        name={convertToName(name!)}
        defaultValue=""
        control={control}
        render={({ field }) => (
          <>
            {open && (
              <GroupResourceContext value={adminClient.groups}>
                <GroupPickerDialog
                  type="selectMany"
                  text={{
                    title: "selectGroup",
                    ok: "select",
                  }}
                  onConfirm={(selected) => {
                    if (selected) {
                      field.onChange(toPathString([...groups, ...selected]));
                      setValue(groupTypeFieldName, GROUP_TYPE_REALM);
                      addGroups(selected);
                    }

                    setOpen(false);
                  }}
                  onClose={() => setOpen(false)}
                  filterGroups={groups}
                />
              </GroupResourceContext>
            )}

            {openOrgGroups && (
              <GroupPickerDialog
                type="selectMany"
                text={{
                  title: "selectOrgGroup",
                  ok: "select",
                }}
                onConfirm={(selected) => {
                  if (selected) {
                    field.onChange(toPathString([...groups, ...selected]));
                    setValue(groupTypeFieldName, GROUP_TYPE_ORG);
                    addGroups(selected);
                  }

                  setOpenOrgGroups(false);
                }}
                onClose={() => setOpenOrgGroups(false)}
                filterGroups={groups}
              />
            )}

            <Flex
              direction={{ default: "row" }}
              justifyContent={{ default: "justifyContentFlexStart" }}
              alignItems={{ default: "alignItemsFlexStart" }}
              flexWrap={{ default: "wrap" }}
              gap={{ default: "gapSm" }}
            >
              {field.value.split(",").map((path: string) => {
                if (path.length > 0) {
                  return (
                    <ActionList key={path}>
                      <ActionListItem>
                        <ChipGroup>
                          <Chip
                            onClick={() => {
                              field.onChange(
                                removeGroupFromField(path, field.value),
                              );
                              removeGroup(path);
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
                            {path}
                          </Chip>
                        </ChipGroup>
                      </ActionListItem>
                    </ActionList>
                  );
                }
              })}
            </Flex>

            {field.value ? (
              <ActionList>
                {groupType == GROUP_TYPE_REALM && (
                  <ActionListItem>
                    <Button
                      data-testid={`group-add-row`}
                      className="pf-v5-u-px-0 pf-v5-u-mt-sm"
                      variant="link"
                      icon={<PlusCircleIcon />}
                      onClick={() => setOpen(true)}
                    >
                      {t("addGroupsEntry")}
                    </Button>
                  </ActionListItem>
                )}

                {groupType == GROUP_TYPE_ORG && (
                  <ActionListItem>
                    <Button
                      data-testid={`group-add-row`}
                      className="pf-v5-u-px-0 pf-v5-u-mt-sm"
                      variant="link"
                      icon={<PlusCircleIcon />}
                      onClick={() => setOpenOrgGroups(true)}
                    >
                      {t("selectOrgGroup")}
                    </Button>
                  </ActionListItem>
                )}
              </ActionList>
            ) : (
              <EmptyState
                data-testid={`addGroup-empty-state`}
                className="pf-v5-u-p-0"
                variant="xs"
              >
                <EmptyStateBody>{t("noGroupEntries")}</EmptyStateBody>
                <EmptyStateFooter>
                  <ActionList>
                    <ActionListItem>
                      <Button
                        data-testid={`addGroup-add-row`}
                        variant="link"
                        icon={<PlusCircleIcon />}
                        size="sm"
                        onClick={() => setOpen(true)}
                        isDisabled={false}
                      >
                        {t("addGroupsEntry")}
                      </Button>
                    </ActionListItem>

                    {shouldRenderOrgField && (
                      <ActionListItem>
                        <Button
                          data-testid={`addOrgGroup-add-row`}
                          variant="link"
                          icon={<PlusCircleIcon />}
                          size="sm"
                          onClick={() => setOpenOrgGroups(true)}
                          isDisabled={false}
                        >
                          {t("selectOrgGroup")}
                        </Button>
                      </ActionListItem>
                    )}
                  </ActionList>
                </EmptyStateFooter>
              </EmptyState>
            )}
          </>
        )}
      />
    </FormGroup>
  );
};
