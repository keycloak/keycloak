import React, { useState } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, ButtonVariant, PageSection } from "@patternfly/react-core";
import { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

import "./RealmSettingsSection.css";
import { cellWidth } from "@patternfly/react-table";

type KeyData = KeyMetadataRepresentation & {
  provider?: string;
};

type KeysTabInnerProps = {
  keys: KeyData[];
};

export const KeysTabInner = ({ keys }: KeysTabInnerProps) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [publicKey, setPublicKey] = useState("");
  const [certificate, setCertificate] = useState("");

  const loader = async () => {
    return keys;
  };

  React.useEffect(() => {
    refresh();
  }, [keys]);

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

  const renderPublicKeyButton = (publicKey: string) => {
    return (
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
    );
  };

  const ButtonRenderer = ({ provider, publicKey, certificate }: KeyData) => {
    if (provider === "ecdsa-generated") {
      return <>{renderPublicKeyButton(publicKey!)}</>;
    }
    if (provider === "rsa-generated" || provider === "fallback-RS256") {
      return (
        <>
          <div>
            {renderPublicKeyButton(publicKey!)}
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
          key={key}
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

type KeysProps = {
  keys: KeyMetadataRepresentation[];
  realmComponents: ComponentRepresentation[];
};

export const KeysListTab = ({ keys, realmComponents, ...props }: KeysProps) => {
  return (
    <KeysTabInner
      keys={keys?.map((key) => {
        const provider = realmComponents.find(
          (component: ComponentRepresentation) =>
            component.id === key.providerId
        );
        return { ...key, provider: provider?.name };
      })}
      {...props}
    />
  );
};
