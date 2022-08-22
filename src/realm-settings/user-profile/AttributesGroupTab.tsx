import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import {
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom-v5-compat";
import { useNavigate } from "react-router-dom-v5-compat";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toEditAttributesGroup } from "../routes/EditAttributesGroup";
import { toNewAttributesGroup } from "../routes/NewAttributesGroup";
import { useUserProfile } from "./UserProfileContext";

export const AttributesGroupTab = () => {
  const { config, save } = useUserProfile();
  const { t } = useTranslation("attributes-group");
  const navigate = useNavigate();
  const { realm } = useRealm();
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

      save(
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
            <Button
              component={(props) => (
                <Link {...props} to={toNewAttributesGroup({ realm })} />
              )}
            >
              {t("createGroupText")}
            </Button>
          </ToolbarItem>
        }
        columns={[
          {
            name: "name",
            displayKey: "attributes-group:columnName",
            cellRenderer: (group) => (
              <Link to={toEditAttributesGroup({ realm, name: group.name! })}>
                {group.name}
              </Link>
            ),
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
            title: t("common:delete"),
            onRowClick: deleteAttributeGroup,
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyStateMessage")}
            instructions={t("emptyStateInstructions")}
            primaryActionText={t("createGroupText")}
            onPrimaryAction={() => navigate(toNewAttributesGroup({ realm }))}
          />
        }
      />
    </PageSection>
  );
};
