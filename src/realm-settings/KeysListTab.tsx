import React, { useState } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, ButtonVariant, PageSection } from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";

import type { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";

import "./RealmSettingsSection.css";

type KeyData = KeyMetadataRepresentation & {
  provider?: string;
};

type KeysListTabProps = {
  realmComponents: ComponentRepresentation[];
};

export const KeysListTab = ({ realmComponents }: KeysListTabProps) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const { url } = useRouteMatch();

  const [publicKey, setPublicKey] = useState("");
  const [certificate, setCertificate] = useState("");

  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();

  const loader = async () => {
    const keysMetaData = await adminClient.realms.getKeys({
      realm: realmName,
    });
    const keys = keysMetaData.keys;

    return keys?.map((key) => {
      const provider = realmComponents.find(
        (component: ComponentRepresentation) => component.id === key.providerId
      );
      return { ...key, provider: provider?.name } as KeyData;
    })!;
  };

  const [togglePublicKeyDialog, PublicKeyDialog] = useConfirmDialog({
    titleKey: t("realm-settings:publicKeys").slice(0, -1),
    messageKey: publicKey,
    continueButtonLabel: "common:close",
    continueButtonVariant: ButtonVariant.primary,
    noCancelButton: true,
    onConfirm: async () => {},
  });

  const [toggleCertificateDialog, CertificateDialog] = useConfirmDialog({
    titleKey: t("realm-settings:certificate"),
    messageKey: certificate,
    continueButtonLabel: "common:close",
    continueButtonVariant: ButtonVariant.primary,
    noCancelButton: true,
    onConfirm: async () => {},
  });

  const goToCreate = () => history.push(`${url}/add-role`);

  const ProviderRenderer = ({ provider }: KeyData) => {
    return <>{provider}</>;
  };

  const ButtonRenderer = ({ type, publicKey, certificate }: KeyData) => {
    if (type === "EC") {
      return (
        <>
          <Button
            onClick={() => {
              togglePublicKeyDialog();
              setPublicKey(publicKey!);
            }}
            variant="secondary"
            id="kc-public-key"
          >
            {t("realm-settings:publicKeys").slice(0, -1)}
          </Button>
        </>
      );
    } else if (type === "RSA") {
      return (
        <>
          <div className="button-wrapper">
            <Button
              onClick={() => {
                togglePublicKeyDialog();
                setPublicKey(publicKey!);
              }}
              variant="secondary"
              id="kc-rsa-public-key"
            >
              {t("realm-settings:publicKeys").slice(0, -1)}
            </Button>
            <Button
              onClick={() => {
                toggleCertificateDialog();
                setCertificate(certificate!);
              }}
              variant="secondary"
              id="kc-certificate"
            >
              {t("realm-settings:certificate")}
            </Button>
          </div>
        </>
      );
    }
  };

  return (
    <>
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <PublicKeyDialog />
        <CertificateDialog />
        <KeycloakDataTable
          isNotCompact={true}
          loader={loader}
          ariaLabelKey="realm-settings:keysList"
          searchPlaceholderKey="realm-settings:searchKey"
          canSelectAll
          columns={[
            {
              name: "algorithm",
              displayKey: "realm-settings:algorithm",
              cellFormatters: [emptyFormatter()],
              transforms: [cellWidth(15)],
            },
            {
              name: "type",
              displayKey: "realm-settings:type",
              cellFormatters: [emptyFormatter()],
              transforms: [cellWidth(10)],
            },
            {
              name: "kid",
              displayKey: "realm-settings:kid",
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "provider",
              displayKey: "realm-settings:provider",
              cellRenderer: ProviderRenderer,
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "publicKeys",
              displayKey: "realm-settings:publicKeys",
              cellRenderer: ButtonRenderer,
              cellFormatters: [],
            },
          ]}
          emptyState={
            <ListEmptyState
              hasIcon={true}
              message={t("noRoles")}
              instructions={t("noRolesInstructions")}
              primaryActionText={t("createRole")}
              onPrimaryAction={goToCreate}
            />
          }
        />
      </PageSection>
    </>
  );
};
