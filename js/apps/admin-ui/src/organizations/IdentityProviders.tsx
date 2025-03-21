import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  PageSection,
  Switch,
  ToolbarItem,
} from "@patternfly/react-core";
import { sortBy } from "lodash-es";
import { BellIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ManageOrderDialog } from "../identity-providers/ManageOrderDialog";
import useToggle from "../utils/useToggle";
import { LinkIdentityProviderModal } from "./LinkIdentityProviderModal";
import { EditOrganizationParams } from "./routes/EditOrganization";

type ShownOnLoginPageCheckProps = {
  row: IdentityProviderRepresentation;
  refresh: () => void;
};

const ShownOnLoginPageCheck = ({
  row,
  refresh,
}: ShownOnLoginPageCheckProps) => {
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();

  const toggle = async (value: boolean) => {
    try {
      await adminClient.identityProviders.update(
        { alias: row.alias! },
        {
          ...row,
          hideOnLogin: value,
        },
      );
      addAlert(t("linkUpdatedSuccessful"));

      refresh();
    } catch (error) {
      addError("linkUpdatedError", error);
    }
  };

  return (
    <Switch
      label={t("on")}
      labelOff={t("off")}
      isChecked={row.hideOnLogin}
      onChange={(_, value) => toggle(value)}
    />
  );
};

export const IdentityProviders = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { id: orgId } = useParams<EditOrganizationParams>();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [manageDisplayDialog, setManageDisplayDialog] = useState(false);
  const [hasProviders, setHasProviders] = useState(false);
  const [selectedRow, setSelectedRow] =
    useState<IdentityProviderRepresentation>();
  const [open, toggleOpen] = useToggle();

  useFetch(
    async () => adminClient.identityProviders.find({ max: 1 }),
    (providers) => {
      setHasProviders(providers.length === 1);
    },
    [],
  );

  const loader = async () => {
    const providers = await adminClient.organizations.listIdentityProviders({
      orgId: orgId!,
    });
    return sortBy(providers, "alias");
  };

  const [toggleUnlinkDialog, UnlinkConfirm] = useConfirmDialog({
    titleKey: "identityProviderUnlink",
    messageKey: "identityProviderUnlinkConfirm",
    continueButtonLabel: "unLinkIdentityProvider",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.organizations.unLinkIdp({
          orgId: orgId!,
          alias: selectedRow!.alias! as string,
        });
        setSelectedRow(undefined);
        addAlert(t("unLinkSuccessful"));
        refresh();
      } catch (error) {
        addError("unLinkError", error);
      }
    },
  });

  return (
    <>
      {manageDisplayDialog && (
        <ManageOrderDialog
          orgId={orgId!}
          onClose={() => {
            setManageDisplayDialog(false);
            refresh();
          }}
        />
      )}
      <PageSection variant="light">
        <UnlinkConfirm />
        {open && (
          <LinkIdentityProviderModal
            orgId={orgId!}
            identityProvider={selectedRow}
            onClose={() => {
              toggleOpen();
              refresh();
            }}
          />
        )}
        {!hasProviders ? (
          <ListEmptyState
            icon={BellIcon}
            message={t("noIdentityProvider")}
            instructions={t("noIdentityProviderInstructions")}
          />
        ) : (
          <KeycloakDataTable
            key={key}
            loader={loader}
            ariaLabelKey="identityProviders"
            searchPlaceholderKey="searchProvider"
            toolbarItem={
              <>
                <ToolbarItem>
                  <Button
                    onClick={() => {
                      setSelectedRow(undefined);
                      toggleOpen();
                    }}
                  >
                    {t("linkIdentityProvider")}
                  </Button>
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    data-testid="manageDisplayOrder"
                    variant="link"
                    onClick={() => setManageDisplayDialog(true)}
                  >
                    {t("manageDisplayOrder")}
                  </Button>
                </ToolbarItem>
              </>
            }
            actions={[
              {
                title: t("edit"),
                onRowClick: (row) => {
                  setSelectedRow(row);
                  toggleOpen();
                },
              },
              {
                title: t("unLinkIdentityProvider"),
                onRowClick: (row) => {
                  setSelectedRow(row);
                  toggleUnlinkDialog();
                },
              },
            ]}
            columns={[
              {
                name: "alias",
              },
              {
                name: "config['kc.org.domain']",
                displayKey: "domain",
              },
              {
                name: "providerId",
                displayKey: "providerDetails",
              },
              {
                name: "hideOnLogin",
                displayKey: "hideOnLoginPage",
                cellRenderer: (row) => (
                  <ShownOnLoginPageCheck row={row} refresh={refresh} />
                ),
              },
            ]}
            emptyState={
              <ListEmptyState
                message={t("emptyIdentityProviderLink")}
                instructions={t("emptyIdentityProviderLinkInstructions")}
                primaryActionText={t("linkIdentityProvider")}
                onPrimaryAction={toggleOpen}
              />
            }
          />
        )}
      </PageSection>
    </>
  );
};
