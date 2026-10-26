import type ClientCertificateRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientCertificateRepresentation";
import {
  SelectControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ClipboardCopy,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  FormGroup,
  Modal,
  ModalVariant,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import type { TFunction } from "i18next";
import { useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAccess } from "../../context/access/Access";
import useFormatDate from "../../utils/useFormatDate";
import useToggle from "../../utils/useToggle";

const FIELD = "config.clientCertificateKeyProviderId";

type Status = "valid" | "expired" | "notYetValid" | "disabled";

const isExpired = (certificate: ClientCertificateRepresentation) =>
  !!certificate.validTo && certificate.validTo < Date.now();

const statusOf = (certificate: ClientCertificateRepresentation): Status => {
  if (certificate.status === "DISABLED") {
    return "disabled";
  }
  if (isExpired(certificate)) {
    return "expired";
  }
  if (certificate.validFrom && certificate.validFrom > Date.now()) {
    return "notYetValid";
  }
  return "valid";
};

const label = (
  certificate: ClientCertificateRepresentation,
  status: Status,
  t: TFunction,
) => {
  const name = certificate.providerName ?? certificate.providerId ?? "";
  return status === "valid"
    ? name
    : `${name} (${t(`clientCertificateStatus.${status}`)})`;
};

const describe = (
  certificate: ClientCertificateRepresentation,
  t: TFunction,
  formatDate: ReturnType<typeof useFormatDate>,
) => {
  return (
    <>
      {certificate.subjectDn && (
        <div>
          {t("clientCertificateSubject")}: {certificate.subjectDn}
        </div>
      )}
      {certificate.issuerDn && (
        <div>
          {t("clientCertificateIssuer")}: {certificate.issuerDn}
        </div>
      )}
      {certificate.validTo && (
        <div>
          {t(
            isExpired(certificate)
              ? "clientCertificateExpired"
              : "clientCertificateExpires",
            { date: formatDate(new Date(certificate.validTo)) },
          )}
        </div>
      )}
    </>
  );
};

const CertificateDetailsModal = ({
  certificate,
  onClose,
}: {
  certificate: ClientCertificateRepresentation;
  onClose: () => void;
}) => {
  const { t } = useTranslation();
  const formatDate = useFormatDate();
  const rows: { label: string; value?: string; copyable?: boolean }[] = [
    { label: "clientCertificateSubject", value: certificate.subjectDn },
    { label: "clientCertificateIssuer", value: certificate.issuerDn },
    {
      label: "clientCertificateValidFrom",
      value: certificate.validFrom
        ? formatDate(new Date(certificate.validFrom))
        : undefined,
    },
    {
      label: "clientCertificateValidTo",
      value: certificate.validTo
        ? formatDate(new Date(certificate.validTo))
        : undefined,
    },
    { label: "clientCertificateSerialNumber", value: certificate.serialNumber },
    {
      label: "clientCertificateFingerprint",
      value: certificate.fingerprintSha256,
      copyable: true,
    },
    { label: "clientCertificateKeyType", value: certificate.type },
    { label: "clientCertificateKeyAlgorithm", value: certificate.algorithm },
  ];

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("clientCertificateDetails")}
      description={certificate.providerName}
      isOpen
      onClose={onClose}
      actions={[
        <Button key="close" data-testid="close" onClick={onClose}>
          {t("close")}
        </Button>,
      ]}
    >
      <DescriptionList isCompact isHorizontal>
        {rows
          .filter(({ value }) => value)
          .map(({ label, value, copyable }) => (
            <DescriptionListGroup key={label}>
              <DescriptionListTerm>{t(label)}</DescriptionListTerm>
              <DescriptionListDescription>
                {copyable ? (
                  <ClipboardCopy isReadOnly hoverTip="" clickTip="">
                    {value}
                  </ClipboardCopy>
                ) : (
                  value
                )}
              </DescriptionListDescription>
            </DescriptionListGroup>
          ))}
      </DescriptionList>
    </Modal>
  );
};

export const ClientCertificateKeyProvider = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const formatDate = useFormatDate();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const { control, getValues } = useFormContext();
  const selected = useWatch({ control, name: FIELD });
  const [detailsOpen, toggleDetails] = useToggle();
  const [certificates, setCertificates] =
    useState<ClientCertificateRepresentation[]>();

  useFetch(
    async () => {
      try {
        return await adminClient.identityProviders.listClientCertificates();
      } catch (error) {
        addError("clientCertificateLoadError", error);
        return [];
      }
    },
    setCertificates,
    [],
  );

  const test = async () => {
    try {
      const result = await adminClient.identityProviders.testClientCertificate(
        {},
        {
          keyProviderId: getValues(FIELD),
          clientId: getValues("config.clientId"),
          tokenUrl: getValues("config.tokenUrl"),
          mtlsTokenUrl: getValues("config.mtlsTokenUrl"),
        },
      );
      addAlert(
        t(
          result
            ? "clientCertificateTestInconclusive"
            : "clientCertificateTestSuccess",
        ),
        result ? AlertVariant.warning : AlertVariant.success,
      );
    } catch (error) {
      addError("clientCertificateTestError", error);
    }
  };

  if (!certificates) {
    return null;
  }

  const selectedCertificate = certificates.find(
    (certificate) => certificate.providerId === selected,
  );

  const options = [
    ...certificates.map((certificate) => ({
      key: certificate.providerId!,
      value: label(certificate, statusOf(certificate), t),
      description: describe(certificate, t, formatDate),
    })),
    ...(selected && !selectedCertificate
      ? [
          {
            key: selected,
            value: t("clientCertificateUnavailable", { id: selected }),
            description: t("clientCertificateUnavailableHelp"),
          },
        ]
      : []),
  ];

  return (
    <>
      <SelectControl
        name={FIELD}
        label={t("clientCertificate")}
        labelIcon={
          certificates.length === 0
            ? t("clientCertificateNone")
            : t("clientCertificateHelp")
        }
        options={options}
        controller={{ defaultValue: "", rules: { required: t("required") } }}
      />
      <FormGroup fieldId="clientCertificateTest">
        <Split hasGutter>
          {hasAccess("manage-identity-providers") && (
            <SplitItem>
              <Button
                variant="secondary"
                data-testid="testClientCertificate"
                onClick={test}
                isDisabled={!selected}
              >
                {t("clientCertificateTest")}
              </Button>
            </SplitItem>
          )}
          {selectedCertificate && (
            <SplitItem>
              <Button
                variant="link"
                data-testid="viewClientCertificateDetails"
                onClick={toggleDetails}
              >
                {t("clientCertificateView")}
              </Button>
            </SplitItem>
          )}
        </Split>
      </FormGroup>
      {detailsOpen && selectedCertificate && (
        <CertificateDetailsModal
          certificate={selectedCertificate}
          onClose={toggleDetails}
        />
      )}
    </>
  );
};
