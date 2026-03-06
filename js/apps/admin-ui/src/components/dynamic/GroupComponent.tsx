import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  ActionList,
  ActionListItem,
  Button,
  Chip,
  ChipGroup,
  FormGroup,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import {
  useGroupResource,
  GroupResourceContext,
} from "../../context/group-resource/GroupResourceContext";
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
  const { control } = useFormContext();
  const { adminClient } = useAdminClient();
  const hasLinkedOrganization = useGroupResource().isOrgGroups();

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
                    <Chip onClick={() => field.onChange(undefined)}>
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
        </>
      )}
    />
  );
};
