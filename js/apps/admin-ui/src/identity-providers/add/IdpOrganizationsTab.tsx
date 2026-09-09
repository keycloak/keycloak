import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import type OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import type OrganizationIdentityProviderLinkRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationIdentityProviderLinkRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import { ButtonVariant, PageSection } from "@patternfly/react-core";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import { LinkIdentityProviderModal } from "../../organizations/LinkIdentityProviderModal";
import { toEditOrganization } from "../../organizations/routes/EditOrganization";
import useToggle from "../../utils/useToggle";

type OrgWithLink = OrganizationRepresentation &
  OrganizationIdentityProviderLinkRepresentation;

type IdpOrganizationsTabProps = {
  alias: string;
  onLinksChange?: () => void;
};

export const IdpOrganizationsTab = ({ alias, onLinksChange }: IdpOrganizationsTabProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [selectedOrg, setSelectedOrg] = useState<OrgWithLink>();
  const [editOpen, toggleEditOpen] = useToggle();
  const idpRef = useRef<IdentityProviderRepresentation>();

  const loader = async (): Promise<OrgWithLink[]> => {
    const idp = await adminClient.identityProviders.findOne({ alias });
    idpRef.current = idp;
    const links = idp?.organizationLinks ?? [];

    const results = await Promise.all(
      links.map(async (link) => {
        try {
          const org = await adminClient.organizations.findOne({
            id: link.organizationId!,
          });
          return org
            ? {
                ...org,
                ...link,
              }
            : null;
        } catch {
          return null;
        }
      }),
    );
    return results.filter(Boolean) as OrgWithLink[];
  };

  const [toggleUnlinkDialog, UnlinkConfirm] = useConfirmDialog({
    titleKey: "identityProviderUnlink",
    messageKey: "identityProviderUnlinkConfirm",
    continueButtonLabel: "unLinkIdentityProvider",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.organizations.unLinkIdp({
          orgId: selectedOrg!.id!,
          alias,
        });
        setSelectedOrg(undefined);
        addAlert(t("unLinkSuccessful"));
        refresh();
        onLinksChange?.();
      } catch (error) {
        addError("unLinkError", error);
      }
    },
  });

  return (
    <PageSection variant="light">
      <UnlinkConfirm />
      {editOpen && selectedOrg && (
        <LinkIdentityProviderModal
          orgId={selectedOrg.id!}
          identityProvider={idpRef.current}
          onClose={() => {
            toggleEditOpen();
            refresh();
          }}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="organizations"
        searchPlaceholderKey="searchOrganization"
        actions={[
          {
            title: t("edit"),
            onRowClick: (row) => {
              setSelectedOrg(row);
              toggleEditOpen();
            },
          },
          {
            title: t("unLinkIdentityProvider"),
            onRowClick: (row) => {
              setSelectedOrg(row);
              toggleUnlinkDialog();
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: (row) => (
              <Link
                to={toEditOrganization({
                  realm,
                  id: row.id!,
                  tab: "identityProviders",
                })}
              >
                {row.name} <ExternalLinkAltIcon />
              </Link>
            ),
          },
          {
            name: "alias",
            displayKey: "alias",
          },
          {
            name: "autoMembership",
            displayKey: "autoMembership",
            cellRenderer: (row) =>
              row.autoMembership ? "True" : "False",
          },
          {
            name: "membershipType",
            displayKey: "membershipType",
            cellRenderer: (row) =>
              t(row.membershipType || "UNMANAGED"),
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noLinkedOrganizations")}
            instructions={t("noLinkedOrganizationsInstructions")}
          />
        }
      />
    </PageSection>
  );
};
