import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { FilterIcon } from "@patternfly/react-icons";

import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../../util";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toKeysTab } from "../routes/KeysTab";

import "../realm-settings-section.css";

const FILTER_OPTIONS = ["ACTIVE", "PASSIVE", "DISABLED"] as const;

type KeyData = KeyMetadataRepresentation & {
  provider?: string;
};

type KeysListTabProps = {
  realmComponents: ComponentRepresentation[];
};

export const KeysListTab = ({ realmComponents }: KeysListTabProps) => {
  const { t } = useTranslation("realm-settings");
  const history = useHistory();

  const [key, setKey] = useState(0);
  const [publicKey, setPublicKey] = useState("");
  const [certificate, setCertificate] = useState("");
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState<typeof FILTER_OPTIONS[number]>(
    FILTER_OPTIONS[0]
  );

  const refresh = () => {
    setKey(key + 1);
  };

  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();

  const loader = async () => {
    const keysMetaData = await adminClient.realms.getKeys({
      realm: realmName,
    });

    const keys = keysMetaData.keys;
    const filtered =
      filterType !== FILTER_OPTIONS[0]
        ? keys?.filter(({ status }) => status === filterType)
        : keys;

    return filtered?.map((key) => {
      const provider = realmComponents.find(
        (component: ComponentRepresentation) => component.id === key.providerId
      );
      return { ...key, provider: provider?.name } as KeyData;
    })!;
  };

  const [togglePublicKeyDialog, PublicKeyDialog] = useConfirmDialog({
    titleKey: t("publicKeys").slice(0, -1),
    messageKey: publicKey,
    continueButtonLabel: "common:close",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: () => Promise.resolve(),
  });

  const [toggleCertificateDialog, CertificateDialog] = useConfirmDialog({
    titleKey: t("certificate"),
    messageKey: certificate,
    continueButtonLabel: "common:close",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: () => Promise.resolve(),
  });

  const ProviderRenderer = ({ provider }: KeyData) => provider;

  const ButtonRenderer = ({ type, publicKey, certificate }: KeyData) => {
    if (type === "EC") {
      return (
        <Button
          onClick={() => {
            togglePublicKeyDialog();
            setPublicKey(publicKey!);
          }}
          variant="secondary"
          id="kc-public-key"
        >
          {t("publicKeys").slice(0, -1)}
        </Button>
      );
    } else if (type === "RSA") {
      return (
        <div className="button-wrapper">
          <Button
            onClick={() => {
              togglePublicKeyDialog();
              setPublicKey(publicKey!);
            }}
            variant="secondary"
            id="kc-rsa-public-key"
          >
            {t("publicKeys").slice(0, -1)}
          </Button>
          <Button
            onClick={() => {
              toggleCertificateDialog();
              setCertificate(certificate!);
            }}
            variant="secondary"
            id="kc-certificate"
          >
            {t("certificate")}
          </Button>
        </div>
      );
    }
  };

  return (
    <PageSection variant="light" padding={{ default: "noPadding" }}>
      <PublicKeyDialog />
      <CertificateDialog />
      <KeycloakDataTable
        isNotCompact={true}
        key={key}
        loader={loader}
        ariaLabelKey="keysList"
        searchPlaceholderKey="realm-settings:searchKey"
        searchTypeComponent={
          <Select
            width={300}
            data-testid="filter-type-select"
            isOpen={filterDropdownOpen}
            className="kc-filter-type-select"
            variant={SelectVariant.single}
            onToggle={() => setFilterDropdownOpen(!filterDropdownOpen)}
            toggleIcon={<FilterIcon />}
            onSelect={(_, value) => {
              setFilterType(
                FILTER_OPTIONS.find((o) => o === value.toString()) ||
                  FILTER_OPTIONS[0]
              );
              refresh();
              setFilterDropdownOpen(false);
            }}
            selections={filterType}
          >
            {FILTER_OPTIONS.map((option) => (
              <SelectOption
                key={option}
                data-testid={`${option}-option`}
                value={option}
              >
                {t(`keysFilter.${option}`)}
              </SelectOption>
            ))}
          </Select>
        }
        canSelectAll
        columns={[
          {
            name: "algorithm",
            displayKey: "algorithm",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(15)],
          },
          {
            name: "type",
            displayKey: "type",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "kid",
            displayKey: "kid",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "provider",
            displayKey: "provider",
            cellRenderer: ProviderRenderer,
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "publicKeys",
            displayKey: "publicKeys",
            cellRenderer: ButtonRenderer,
            cellFormatters: [],
            transforms: [cellWidth(20)],
          },
        ]}
        isSearching={filterType !== FILTER_OPTIONS[0]}
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noKeys")}
            instructions={t("noKeysDescription")}
            primaryActionText={t("addProvider")}
            onPrimaryAction={() =>
              history.push(toKeysTab({ realm: realmName, tab: "providers" }))
            }
          />
        }
      />
    </PageSection>
  );
};
