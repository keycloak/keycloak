import { useTranslation } from "react-i18next";
import {
  Button,
  Modal,
  ModalVariant,
  Popover,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { CheckCircleIcon } from "@patternfly/react-icons";

import type { AuthenticationType } from "../AuthenticationSection";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import useToggle from "../../utils/useToggle";

import "./used-by.css";

type UsedByProps = {
  authType: AuthenticationType;
};

const Label = ({ label }: { label: string }) => (
  <>
    <CheckCircleIcon className="keycloak_authentication-section__usedby" />{" "}
    {label}
  </>
);

type UsedByModalProps = {
  values: string[];
  onClose: () => void;
  isSpecificClient: boolean;
};

const UsedByModal = ({
  values,
  isSpecificClient,
  onClose,
}: UsedByModalProps) => {
  const { t } = useTranslation("authentication");
  return (
    <Modal
      header={
        <TextContent>
          <Text component={TextVariants.h1}>{t("flowUsedBy")}</Text>
          <Text>
            {t("flowUsedByDescription", {
              value: isSpecificClient ? t("clients") : t("identiyProviders"),
            })}
          </Text>
        </TextContent>
      }
      variant={ModalVariant.medium}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="cancel"
          id="modal-cancel"
          key="cancel"
          onClick={onClose}
        >
          {t("common:close")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        loader={values.map((value) => ({ name: value }))}
        ariaLabelKey="authentication:usedBy"
        searchPlaceholderKey="common:search"
        columns={[
          {
            name: "name",
          },
        ]}
      />
    </Modal>
  );
};

export const UsedBy = ({
  authType: {
    id,
    usedBy: { type, values },
  },
}: UsedByProps) => {
  const { t } = useTranslation("authentication");
  const [open, toggle] = useToggle();

  return (
    <>
      {open && (
        <UsedByModal
          values={values}
          onClose={toggle}
          isSpecificClient={type === "specificClients"}
        />
      )}
      {(type === "specificProviders" || type === "specificClients") &&
        (values.length < 8 ? (
          <Popover
            key={id}
            aria-label={t("usedBy")}
            bodyContent={
              <div key={`usedBy-${id}-${values}`}>
                {t(
                  "appliedBy" +
                    (type === "specificClients" ? "Clients" : "Providers")
                )}{" "}
                {values.map((used, index) => (
                  <>
                    <strong>{used}</strong>
                    {index < values.length - 1 ? ", " : ""}
                  </>
                ))}
              </div>
            }
          >
            <Button
              variant="link"
              className="keycloak__used-by__popover-button"
            >
              <Label label={t(type!)} />
            </Button>
          </Popover>
        ) : (
          <Button
            variant="link"
            className="keycloak__used-by__popover-button"
            onClick={toggle}
          >
            <Label label={t(type!)} />
          </Button>
        ))}
      {type === "default" && <Label label={t(`flow.${values[0]}`)} />}
      {!type && t("notInUse")}
    </>
  );
};
