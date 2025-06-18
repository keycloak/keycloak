import { ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Td } from "@patternfly/react-table";
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
} from "@patternfly/react-core";
import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import useToggle from "../../utils/useToggle";
import useLocaleSort from "../../utils/useLocaleSort";
import { CredentialDataDialog } from "./CredentialDataDialog";
import useFormatDate from "../../utils/useFormatDate";
import { EllipsisVIcon } from "@patternfly/react-icons";

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
  const formatDate = useFormatDate();
  const { t } = useTranslation();
  const [showData, toggleShow] = useToggle();
  const [kebabOpen, toggleKebab] = useToggle();
  const localeSort = useLocaleSort();

  const rows = useMemo(() => {
    if (!credential.credentialData) {
      return [];
    }

    const credentialData: Record<string, unknown> = JSON.parse(
      credential.credentialData,
    );
    return localeSort(Object.entries(credentialData), ([key]) => key).map<
      [string, string]
    >(([key, value]) => {
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
          title={credential.userLabel || t("passwordDataTitle")}
          credentialData={rows}
          onClose={() => {
            toggleShow();
          }}
        />
      )}

      <Td>{children}</Td>
      <Td>{formatDate(new Date(credential.createdDate!))}</Td>
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
        <Td isActionCell>
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
      <Td isActionCell>
        <Dropdown
          popperProps={{
            position: "right",
          }}
          onOpenChange={toggleKebab}
          toggle={(ref) => (
            <MenuToggle
              ref={ref}
              isExpanded={kebabOpen}
              onClick={toggleKebab}
              variant="plain"
              aria-label="Kebab toggle"
            >
              <EllipsisVIcon />
            </MenuToggle>
          )}
          isOpen={kebabOpen}
        >
          <DropdownList>
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
            </DropdownItem>
          </DropdownList>
        </Dropdown>
      </Td>
    </>
  );
};
