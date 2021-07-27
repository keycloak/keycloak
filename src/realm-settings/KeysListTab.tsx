import React, { useState } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
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

import type { KeyMetadataRepresentation } from "keycloak-admin/lib/defs/keyMetadataRepresentation";
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";

import "./RealmSettingsSection.css";
import { FilterIcon } from "@patternfly/react-icons";

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

  const [key, setKey] = useState(0);
  const [publicKey, setPublicKey] = useState("");
  const [certificate, setCertificate] = useState("");
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState("Active keys");

  const refresh = () => {
    setKey(new Date().getTime());
  };

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

  const activeKeysLoader = async () => {
    const keysMetaData = await adminClient.realms.getKeys({
      realm: realmName,
    });
    const keys = keysMetaData.keys;

    const activeKeysCopy = keys!.filter((i) => i.status === "ACTIVE");

    return activeKeysCopy?.map((key) => {
      const provider = realmComponents!.find(
        (component: ComponentRepresentation) => component.id === key.providerId
      );
      return { ...key, provider: provider?.name } as KeyData;
    })!;
  };

  const passiveKeysLoader = async () => {
    const keysMetaData = await adminClient.realms.getKeys({
      realm: realmName,
    });
    const keys = keysMetaData.keys;

    const passiveKeys = keys!.filter((i) => i.status === "PASSIVE");

    return passiveKeys?.map((key) => {
      const provider = realmComponents!.find(
        (component: ComponentRepresentation) => component.id === key.providerId
      );
      return { ...key, provider: provider?.name } as KeyData;
    })!;
  };

  const disabledKeysLoader = async () => {
    const keysMetaData = await adminClient.realms.getKeys({
      realm: realmName,
    });
    const keys = keysMetaData.keys;

    const disabledKeys = keys!.filter((i) => i.status === "DISABLED");

    return disabledKeys?.map((key) => {
      const provider = realmComponents!.find(
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
    onConfirm: () => Promise.resolve(),
  });

  const [toggleCertificateDialog, CertificateDialog] = useConfirmDialog({
    titleKey: t("realm-settings:certificate"),
    messageKey: certificate,
    continueButtonLabel: "common:close",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: () => Promise.resolve(),
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

  const options = [
    <SelectOption
      key={1}
      data-testid="active-keys-option"
      value={t("realm-settings:activeKeys")}
      isPlaceholder
    />,
    <SelectOption
      data-testid="passive-keys-option"
      key={2}
      value={t("realm-settings:passiveKeys")}
    />,
    <SelectOption
      data-testid="disabled-keys-option"
      key={3}
      value={t("realm-settings:disabledKeys")}
    />,
  ];

  return (
    <>
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <PublicKeyDialog />
        <CertificateDialog />
        <KeycloakDataTable
          isNotCompact={true}
          key={key}
          loader={
            filterType === "Active keys"
              ? activeKeysLoader
              : filterType === "Passive keys"
              ? passiveKeysLoader
              : filterType === "Disabled keys"
              ? disabledKeysLoader
              : loader
          }
          ariaLabelKey="realm-settings:keysList"
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
                setFilterType(value as string);
                refresh();
                setFilterDropdownOpen(false);
              }}
              selections={filterType}
            >
              {options}
            </Select>
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
              displayKey: "realm-settings:type",
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
              name: "provider",
              displayKey: "realm-settings:provider",
              cellRenderer: ProviderRenderer,
              cellFormatters: [emptyFormatter()],
              transforms: [cellWidth(10)],
            },
            {
              name: "publicKeys",
              displayKey: "realm-settings:publicKeys",
              cellRenderer: ButtonRenderer,
              cellFormatters: [],
              transforms: [cellWidth(20)],
            },
          ]}
          isSearching={!!filterType}
          emptyState={
            <ListEmptyState
              hasIcon={true}
              message={t("realm-settings:noKeys")}
              instructions={
                t(`realm-settings:noKeysDescription`) +
                `${filterType.toLocaleLowerCase()}.`
              }
              primaryActionText={t("createRole")}
              onPrimaryAction={goToCreate}
            />
          }
        />
      </PageSection>
    </>
  );
};
