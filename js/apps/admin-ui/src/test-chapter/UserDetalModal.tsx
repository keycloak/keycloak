import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { KeycloakSpinner, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Label,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
} from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import useFormatDate from "../utils/useFormatDate";

const withFallback = (value?: string) => value || "—";

type UserDetailModalProps = {
  userId: string;
  onClose: () => void;
};

export const UserDetailModal = ({ userId, onClose }: UserDetailModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const formatDate = useFormatDate();
  const [user, setUser] = useState<UserRepresentation>();

  useFetch(
    () => adminClient.users.findOne({ id: userId }),
    (user) => setUser(user),
    [userId],
  );

  const attributeEntries = Object.entries(user?.attributes ?? {});

  return (
    <Modal
      data-testid="user-detail-modal"
      variant={ModalVariant.medium}
      title={t("userDetails")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="user-detail-modal-close"
          id="modal-close"
          key="close"
          onClick={onClose}
        >
          {t("close")}
        </Button>,
      ]}
    >
      {!user ? (
        <KeycloakSpinner />
      ) : (
        <DescriptionList isHorizontal>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("id")}</DescriptionListTerm>
            <DescriptionListDescription>{user.id}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("username")}</DescriptionListTerm>
            <DescriptionListDescription>
              {withFallback(user.username)}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("email")}</DescriptionListTerm>
            <DescriptionListDescription>
              {withFallback(user.email)}
              {user.email && (
                <Label
                  className="pf-v5-u-ml-sm"
                  color={user.emailVerified ? "green" : "orange"}
                  icon={
                    user.emailVerified ? (
                      <CheckCircleIcon />
                    ) : (
                      <ExclamationCircleIcon />
                    )
                  }
                >
                  {user.emailVerified ? t("emailVerified") : t("notVerified")}
                </Label>
              )}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("firstName")}</DescriptionListTerm>
            <DescriptionListDescription>
              {withFallback(user.firstName)}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("lastName")}</DescriptionListTerm>
            <DescriptionListDescription>
              {withFallback(user.lastName)}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("status")}</DescriptionListTerm>
            <DescriptionListDescription>
              <Label color={user.enabled ? "green" : "red"}>
                {user.enabled ? t("enabled") : t("disabled")}
              </Label>
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("created")}</DescriptionListTerm>
            <DescriptionListDescription>
              {user.createdTimestamp
                ? formatDate(new Date(user.createdTimestamp))
                : "—"}
            </DescriptionListDescription>
          </DescriptionListGroup>
          {attributeEntries.length > 0 && (
            <DescriptionListGroup>
              <DescriptionListTerm>{t("attributes")}</DescriptionListTerm>
              <DescriptionListDescription>
                {attributeEntries.map(([key, values]) => (
                  <div key={key}>
                    <strong>{key}:</strong> {values.join(", ")}
                  </div>
                ))}
              </DescriptionListDescription>
            </DescriptionListGroup>
          )}
        </DescriptionList>
      )}
    </Modal>
  );
};
