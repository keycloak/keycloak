import React, { ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Td } from "@patternfly/react-table";
import {
  Button,
  Dropdown,
  DropdownPosition,
  KebabToggle,
  DropdownItem,
} from "@patternfly/react-core";

import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import useToggle from "../../utils/useToggle";
import { CredentialDataDialog } from "./CredentialDataDialog";

type CredentialRowProps = {
  credential: CredentialRepresentation;
  resetPassword: () => void;
  toggleDelete: () => void;
  children: ReactNode;
};

export const CredentialRow = ({
  credential,
  resetPassword,
  toggleDelete,
  children,
}: CredentialRowProps) => {
  const { t } = useTranslation("users");
  const [showData, toggleShow] = useToggle();
  const [kebabOpen, toggleKebab] = useToggle();

  const { whoAmI } = useWhoAmI();

  const rows = useMemo(() => {
    if (!credential.credentialData) {
      return [];
    }

    const credentialData: Record<string, unknown> = JSON.parse(
      credential.credentialData
    );
    const locale = whoAmI.getLocale();

    return Object.entries(credentialData)
      .sort(([a], [b]) => a.localeCompare(b, locale))
      .map<[string, string]>(([key, value]) => {
        if (typeof value === "string") {
          return [key, value];
        }

        return [key, JSON.stringify(value)];
      });
  }, [credential.credentialData]);

  return (
    <>
      {showData && Object.keys(credential).length !== 0 && (
        <CredentialDataDialog
          credentialData={rows}
          onClose={() => {
            toggleShow();
          }}
        />
      )}

      <Td>{children}</Td>
      <Td>
        <Button
          className="kc-showData-btn"
          variant="link"
          data-testid="showDataBtn"
          onClick={toggleShow}
        >
          {t("showDataBtn")}
        </Button>
      </Td>
      {credential.type === "password" ? (
        <Td>
          <Button
            variant="secondary"
            data-testid="resetPasswordBtn"
            onClick={resetPassword}
          >
            {t("resetPasswordBtn")}
          </Button>
        </Td>
      ) : (
        <Td />
      )}
      <Td>
        <Dropdown
          isPlain
          position={DropdownPosition.right}
          toggle={<KebabToggle onToggle={toggleKebab} />}
          isOpen={kebabOpen}
          dropdownItems={[
            <DropdownItem
              key={credential.id}
              data-testid="deleteDropdownItem"
              component="button"
              onClick={() => {
                toggleDelete();
                toggleKebab();
              }}
            >
              {t("deleteBtn")}
            </DropdownItem>,
          ]}
        />
      </Td>
    </>
  );
};
