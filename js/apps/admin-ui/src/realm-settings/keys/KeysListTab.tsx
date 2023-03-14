import { useState } from "react";
import { useNavigate } from "react-router-dom";
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
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { toKeysTab } from "../routes/KeysTab";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";

import "../realm-settings-section.css";

const FILTER_OPTIONS = ["ACTIVE", "PASSIVE", "DISABLED"] as const;
type FilterType = (typeof FILTER_OPTIONS)[number];

type KeyData = KeyMetadataRepresentation & {
  provider?: string;
};

type KeysListTabProps = {
  realmComponents: ComponentRepresentation[];
};

type SelectFilterProps = {
  onFilter: (filter: FilterType) => void;
};

const SelectFilter = ({ onFilter }: SelectFilterProps) => {
  const { t } = useTranslation("realm-settings");
  const [filterType, setFilterType] = useState<FilterType>(FILTER_OPTIONS[0]);

  const [filterDropdownOpen, toggleFilter] = useToggle();
  return (
    <Select
      width={300}
      data-testid="filter-type-select"
      isOpen={filterDropdownOpen}
      className="kc-filter-type-select"
      variant={SelectVariant.single}
      onToggle={toggleFilter}
      toggleIcon={<FilterIcon />}
      onSelect={(_, value) => {
        const filter =
          FILTER_OPTIONS.find((o) => o === value.toString()) ||
          FILTER_OPTIONS[0];
        setFilterType(filter);
        onFilter(filter);
        toggleFilter();
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
  );
};

export const KeysListTab = ({ realmComponents }: KeysListTabProps) => {
  const { t } = useTranslation("realm-settings");
  const navigate = useNavigate();

  const [publicKey, setPublicKey] = useState("");
  const [certificate, setCertificate] = useState("");

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const [keyData, setKeyData] = useState<KeyData[]>();
  const [filteredKeyData, setFilteredKeyData] = useState<KeyData[]>();

  useFetch(
    async () => {
      const keysMetaData = await adminClient.realms.getKeys({ realm });
      return keysMetaData.keys?.map((key) => {
        const provider = realmComponents.find(
          (component: ComponentRepresentation) =>
            component.id === key.providerId
        );
        return { ...key, provider: provider?.name } as KeyData;
      })!;
    },
    setKeyData,
    []
  );

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

  if (!keyData) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light" padding={{ default: "noPadding" }}>
      <PublicKeyDialog />
      <CertificateDialog />
      <KeycloakDataTable
        isNotCompact
        className="kc-keys-list"
        loader={filteredKeyData || keyData}
        ariaLabelKey="realm-settings:keysList"
        searchPlaceholderKey="realm-settings:searchKey"
        searchTypeComponent={
          <SelectFilter
            onFilter={(filterType) =>
              setFilteredKeyData(
                filterType !== FILTER_OPTIONS[0]
                  ? keyData!.filter(({ status }) => status === filterType)
                  : undefined
              )
            }
          />
        }
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
            displayKey: "type",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "kid",
            displayKey: "realm-settings:kid",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "use",
            displayKey: "realm-settings:use",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "provider",
            displayKey: "realm-settings:provider",
            cellRenderer: ({ provider }: KeyData) => provider || "",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(10)],
          },
          {
            name: "publicKeys",
            displayKey: "realm-settings:publicKeys",
            cellRenderer: ({ type, publicKey, certificate }: KeyData) => {
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
                      id={publicKey}
                    >
                      {t("publicKeys").slice(0, -1)}
                    </Button>
                    <Button
                      onClick={() => {
                        toggleCertificateDialog();
                        setCertificate(certificate!);
                      }}
                      variant="secondary"
                      id={certificate}
                      className="kc-certificate"
                    >
                      {t("certificate")}
                    </Button>
                  </div>
                );
              } else return "";
            },
            cellFormatters: [],
            transforms: [cellWidth(20)],
          },
        ]}
        isSearching={!!filteredKeyData}
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noKeys")}
            instructions={t("noKeysDescription")}
            primaryActionText={t("addProvider")}
            onPrimaryAction={() =>
              navigate(toKeysTab({ realm, tab: "providers" }))
            }
          />
        }
      />
    </PageSection>
  );
};
