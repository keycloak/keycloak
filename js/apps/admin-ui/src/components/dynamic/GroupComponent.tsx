import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  ActionList,
  ActionListItem,
  Button,
  Chip,
  ChipGroup,
  FormGroup,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem, useFetch } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { GroupResourceContext } from "../../context/group-resource/GroupResourceContext";
import { useIdentityProvider } from "../../context/identity-provider/IdentityProviderContext";
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
  const [organization, setOrganization] =
    useState<OrganizationRepresentation>();
  const { control, setValue } = useFormContext();
  const { adminClient } = useAdminClient();
  const serverInfo = useServerInfo();
  const identityProvider = useIdentityProvider();
  const groupTypeFieldName = convertToName("groupType");
  const orgIdFieldName = convertToName("orgId");

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
  const orgId = useWatch({ name: orgIdFieldName, control });

  // a mapper targets one organization, so the picker is only offered when the identity provider is linked to some
  const canSelectOrgGroup =
    !!identityProvider?.alias && !!identityProvider.organizationLinks?.length;
  const shouldRenderOrgField =
    canSelectOrgGroup || groupType === GROUP_TYPE_ORG;

  useFetch(
    async () =>
      orgId
        ? await adminClient.organizations
            .findOne({ id: orgId })
            .catch(() => undefined)
        : undefined,
    setOrganization,
    [orgId],
  );

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
                  setValue(orgIdFieldName, undefined);
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
              identityProviderAlias={identityProvider!.alias}
              onConfirm={(groups, organization) => {
                field.onChange(groups?.[0].path);
                setValue(groupTypeFieldName, GROUP_TYPE_ORG);
                setValue(orgIdFieldName, organization?.id);
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
                        setValue(orgIdFieldName, undefined);
                      }}
                    >
                      {shouldRenderOrgField && (
                        <>
                          {groupType === GROUP_TYPE_REALM
                            ? t("realm")
                            : (organization?.name ?? t("organization"))}
                          :&nbsp;
                        </>
                      )}
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
              {canSelectOrgGroup && (
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
        </>
      )}
    />
  );
};
