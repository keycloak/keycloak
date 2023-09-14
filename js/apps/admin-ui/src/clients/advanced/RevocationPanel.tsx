import {
  ActionGroup,
  Button,
  FormGroup,
  InputGroup,
  Text,
  Tooltip,
} from "@patternfly/react-core";
import { useEffect, useRef } from "react";
import { useFormContext } from "react-hook-form";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useRealm } from "../../context/realm-context/RealmContext";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../../utils/useFormatDate";
import { AdvancedProps, parseResult } from "../AdvancedTab";
import { toClient } from "../routes/Client";

export const RevocationPanel = ({
  save,
  client: { id, adminUrl, access },
}: AdvancedProps) => {
  const revocationFieldName = "notBefore";
  const pushRevocationButtonRef = useRef<HTMLElement>();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const { addAlert } = useAlerts();
  const formatDate = useFormatDate();

  const { getValues, setValue, register } = useFormContext();

  const setNotBefore = (time: number, messageKey: string) => {
    setValue(revocationFieldName, time);
    save({ messageKey });
  };

  useEffect(() => {
    register(revocationFieldName);
  }, [register]);

  const getNotBeforeValue = () => {
    const date = getValues(revocationFieldName);
    if (date > 0) {
      return formatDate(new Date(date * 1000), FORMAT_DATE_AND_TIME);
    } else {
      return t("none");
    }
  };

  const push = async () => {
    const result = await adminClient.clients.pushRevocation({
      id: id!,
    });
    parseResult(result, "notBeforePush", addAlert, t);
  };

  return (
    <>
      <Text className="pf-u-pb-lg">
        <Trans i18nKey="notBeforeIntro">
          In order to successfully push setup url on
          <Link to={toClient({ realm, clientId: id!, tab: "settings" })}>
            {t("settings")}
          </Link>
          tab
        </Trans>
      </Text>
      <FormAccess
        role="manage-clients"
        fineGrainedAccess={access?.configure}
        isHorizontal
      >
        <FormGroup
          label={t("notBefore")}
          fieldId="kc-not-before"
          labelIcon={
            <HelpItem helpText={t("notBeforeHelp")} fieldLabelId="notBefore" />
          }
        >
          <InputGroup>
            <KeycloakTextInput
              type="text"
              id="kc-not-before"
              name="notBefore"
              isReadOnly
              value={getNotBeforeValue()}
            />
            <Button
              id="setToNow"
              variant="control"
              onClick={() => {
                setNotBefore(Date.now() / 1000, "notBeforeSetToNow");
              }}
            >
              {t("setToNow")}
            </Button>
            <Button
              id="clear"
              variant="control"
              onClick={() => {
                setNotBefore(0, "notBeforeNowClear");
              }}
            >
              {t("clear")}
            </Button>
          </InputGroup>
        </FormGroup>
        <ActionGroup>
          {!adminUrl && (
            <Tooltip
              reference={pushRevocationButtonRef}
              content={t("notBeforeTooltip")}
            />
          )}
          <Button
            id="push"
            variant="secondary"
            onClick={push}
            isAriaDisabled={!adminUrl}
            ref={pushRevocationButtonRef}
          >
            {t("push")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </>
  );
};
