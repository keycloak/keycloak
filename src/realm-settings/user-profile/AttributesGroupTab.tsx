import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import {
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import React, { useEffect, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import type { OnSaveCallback } from "./UserProfileTab";

type AttributesGroupTabProps = {
  config?: UserProfileConfig;
  onSave: OnSaveCallback;
};

export const AttributesGroupTab = ({
  config,
  onSave,
}: AttributesGroupTabProps) => {
  const { t } = useTranslation();
  const history = useHistory();
  const [key, setKey] = useState(0);
  const [groupToDelete, setGroupToDelete] = useState<UserProfileGroup>();

  // Refresh data in table when config changes.
  useEffect(() => setKey((value) => value + 1), [config]);

  async function loader() {
    return config?.groups ?? [];
  }

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "attributes-group:deleteDialogTitle",
    children: (
      <Trans i18nKey="attributes-group:deleteDialogDescription">
        {" "}
        <strong>{{ group: groupToDelete?.name }}</strong>.
      </Trans>
    ),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm() {
      const groups = (config?.groups ?? []).filter(
        (group) => group !== groupToDelete
      );

      onSave(
        { ...config, groups },
        {
          successMessageKey: "attributes-group:deleteSuccess",
          errorMessageKey: "attributes-group:deleteError",
        }
      );
    },
  });

  function deleteAttributeGroup(group: UserProfileGroup) {
    setGroupToDelete(group);
    toggleDeleteDialog();
  }

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <DeleteConfirm />
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="attributes-group:tableTitle"
        toolbarItem={
          <ToolbarItem>
            {/* TODO: Add link to page */}
            <Button component={(props) => <Link {...props} to={{}} />}>
              {t("attributes-group:createButtonText")}
            </Button>
          </ToolbarItem>
        }
        columns={[
          {
            name: "name",
            displayKey: "attributes-group:columnName",
          },
          {
            name: "displayHeader",
            displayKey: "attributes-group:columnDisplayName",
          },
          {
            name: "displayDescription",
            displayKey: "attributes-group:columnDisplayDescription",
          },
        ]}
        actions={[
          {
            title: t("common:edit"),
            // TODO: Add link to page.
            onRowClick: () => history.push({}),
          },
          {
            title: t("common:delete"),
            onRowClick: deleteAttributeGroup,
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("attributes-group:emptyStateMessage")}
            instructions={t("attributes-group:emptyStateInstructions")}
            primaryActionText={t("attributes-group:createButtonText")}
            // TODO: Add link to page.
            onPrimaryAction={() => history.push({})}
          />
        }
      />
    </PageSection>
  );
};
