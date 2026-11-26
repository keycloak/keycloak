import { NetworkError } from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { HelpItem, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  Label,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  InfoCircleIcon,
  TimesCircleIcon,
} from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../../admin-client";
import { FormAccess } from "../../../components/form/FormAccess";

type SubjectType = "user-id" | "user-email" | "user-username" | "org-alias";
type SubjectState =
  | "notified"
  | "ignored"
  | "notified_via_org"
  | "ignored_via_org"
  | "implicitly_included"
  | "not_notified";

export type SubjectsTabProps = {
  client: ClientRepresentation;
};

export const SubjectsTab = ({ client }: SubjectsTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [subjectType, setSubjectType] = useState<SubjectType>("user-email");
  const [subjectValue, setSubjectValue] = useState("");
  const [subjectStatus, setSubjectStatus] = useState<{
    variant: "success" | "danger" | "info";
    message: string;
  } | null>(null);
  const [subjectLoading, setSubjectLoading] = useState(false);
  const [subjectValueError, setSubjectValueError] = useState<string | null>(
    null,
  );

  const subjectRequest = () => ({
    type: subjectType,
    value: subjectValue,
  });

  const clientQuery = () => ({
    clientId: client.clientId!,
  });

  const checkSubjectViaAdminApi = async (): Promise<{
    state: SubjectState;
    sourceOrgAlias?: string;
  } | null> => {
    // Authoritative read: the server runs the same gate logic the
    // dispatcher uses (per-user / per-org notify, default_subjects
    // fallback, org-membership inheritance) so the displayed status
    // matches what the dispatcher would actually decide.
    const body = await adminClient.ssf.checkSubject(
      clientQuery(),
      subjectRequest(),
    );
    if (body === null) {
      return null;
    }
    return {
      state: body.status as SubjectState,
      sourceOrgAlias: body.source_org_alias,
    };
  };

  const applySubjectState = (state: SubjectState, sourceOrgAlias?: string) => {
    if (state === "notified") {
      setSubjectStatus({
        variant: "success",
        message: t("ssfSubjectIsNotified"),
      });
    } else if (state === "notified_via_org") {
      setSubjectStatus({
        variant: "success",
        message: sourceOrgAlias
          ? t("ssfSubjectIsNotifiedViaOrgNamed", { org: sourceOrgAlias })
          : t("ssfSubjectIsNotifiedViaOrg"),
      });
    } else if (state === "implicitly_included") {
      setSubjectStatus({
        variant: "success",
        message: t("ssfSubjectIsImplicitlyIncluded"),
      });
    } else if (state === "ignored") {
      setSubjectStatus({
        variant: "danger",
        message: t("ssfSubjectIsIgnored"),
      });
    } else if (state === "ignored_via_org") {
      setSubjectStatus({
        variant: "danger",
        message: sourceOrgAlias
          ? t("ssfSubjectIsIgnoredViaOrgNamed", { org: sourceOrgAlias })
          : t("ssfSubjectIsIgnoredViaOrg"),
      });
    } else {
      setSubjectStatus({
        variant: "info",
        message: t("ssfSubjectIsNotNotified"),
      });
    }
  };

  const handleSubjectAction = async (
    action: "add" | "remove" | "ignore" | "check",
  ) => {
    if (!subjectValue.trim()) {
      setSubjectValueError(t("ssfSubjectValueRequired"));
      return;
    }
    setSubjectValueError(null);
    setSubjectLoading(true);
    setSubjectStatus(null);
    try {
      if (action === "check") {
        const result = await checkSubjectViaAdminApi();
        if (result === null) {
          setSubjectValueError(t("ssfSubjectNotFound"));
        } else {
          applySubjectState(result.state, result.sourceOrgAlias);
        }
        return;
      }

      let data;
      try {
        data =
          action === "add"
            ? await adminClient.ssf.addSubject(clientQuery(), subjectRequest())
            : action === "ignore"
              ? await adminClient.ssf.ignoreSubject(
                  clientQuery(),
                  subjectRequest(),
                )
              : await adminClient.ssf.removeSubject(
                  clientQuery(),
                  subjectRequest(),
                );
      } catch (error) {
        if (error instanceof NetworkError && error.response.status === 404) {
          setSubjectValueError(t("ssfSubjectNotFound"));
          return;
        }
        throw error;
      }

      const state = data.status as SubjectState | undefined;
      if (state) {
        applySubjectState(state);
      }

      addAlert(
        action === "add"
          ? t("ssfSubjectAdded")
          : action === "ignore"
            ? t("ssfSubjectIgnored")
            : t("ssfSubjectRemoved"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("ssfSubjectActionError", error);
    } finally {
      setSubjectLoading(false);
    }
  };

  return (
    <Card isFlat className="pf-v5-u-mt-md">
      <CardHeader>
        <CardTitle>{t("ssfTabSubjects")}</CardTitle>
      </CardHeader>
      <CardBody>
        <TextContent>
          <Text>{t("ssfSubjectsHelp")}</Text>
        </TextContent>
      </CardBody>
      <CardBody>
        <FormAccess
          role="manage-clients"
          fineGrainedAccess={client.access?.configure}
          isHorizontal
          onSubmit={(e) => {
            e.preventDefault();
            void handleSubjectAction("check");
          }}
        >
          <FormGroup
            label={t("ssfSubjectType")}
            fieldId="ssfSubjectType"
            labelIcon={
              <HelpItem
                helpText={t("ssfSubjectTypeHelp")}
                fieldLabelId="ssfSubjectType"
              />
            }
          >
            <select
              id="ssfSubjectType"
              data-testid="ssfSubjectType"
              value={subjectType}
              onChange={(e) => setSubjectType(e.target.value as SubjectType)}
              className="pf-v5-c-form-control"
            >
              <option value="user-email">
                {t("ssfSubjectType.userEmail")}
              </option>
              <option value="user-id">{t("ssfSubjectType.userId")}</option>
              <option value="user-username">
                {t("ssfSubjectType.userUsername")}
              </option>
              <option value="org-alias">{t("ssfSubjectType.orgAlias")}</option>
            </select>
          </FormGroup>
          <FormGroup
            label={t("ssfSubjectValue")}
            fieldId="ssfSubjectValue"
            isRequired
          >
            <TextInput
              id="ssfSubjectValue"
              data-testid="ssfSubjectValue"
              value={subjectValue}
              validated={subjectValueError ? "error" : "default"}
              onChange={(_e, value) => {
                setSubjectValue(value);
                if (subjectValueError) {
                  setSubjectValueError(null);
                }
              }}
              placeholder={
                subjectType === "user-email"
                  ? "user@example.com"
                  : subjectType === "user-id"
                    ? "user-uuid"
                    : subjectType === "user-username"
                      ? "username"
                      : "org-alias"
              }
            />

            {subjectValueError && (
              <FormHelperText>
                <HelperText>
                  <HelperTextItem
                    variant="error"
                    data-testid="ssfSubjectValueError"
                  >
                    {subjectValueError}
                  </HelperTextItem>
                </HelperText>
              </FormHelperText>
            )}
          </FormGroup>
          {subjectStatus && (
            <FormGroup
              label={t("ssfSubjectStatusLabel")}
              fieldId="ssfSubjectStatus"
            >
              <Label
                color={
                  subjectStatus.variant === "success"
                    ? "green"
                    : subjectStatus.variant === "danger"
                      ? "red"
                      : "grey"
                }
                icon={
                  subjectStatus.variant === "success" ? (
                    <CheckCircleIcon />
                  ) : subjectStatus.variant === "danger" ? (
                    <TimesCircleIcon />
                  ) : (
                    <InfoCircleIcon />
                  )
                }
                data-testid="ssfSubjectStatus"
              >
                {subjectStatus.message}
              </Label>
            </FormGroup>
          )}
          <ActionGroup>
            <Button
              type="button"
              variant="primary"
              onClick={() => handleSubjectAction("add")}
              isDisabled={subjectLoading}
              data-testid="ssfSubjectAdd"
            >
              {t("ssfSubjectAdd")}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => handleSubjectAction("ignore")}
              isDisabled={subjectLoading}
              data-testid="ssfSubjectIgnore"
            >
              {t("ssfSubjectIgnore")}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => handleSubjectAction("remove")}
              isDisabled={subjectLoading}
              data-testid="ssfSubjectRemove"
            >
              {t("ssfSubjectRemove")}
            </Button>
            <Button
              type="button"
              variant="tertiary"
              onClick={() => handleSubjectAction("check")}
              isDisabled={subjectLoading}
              data-testid="ssfSubjectCheck"
            >
              {t("ssfSubjectCheck")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </CardBody>
    </Card>
  );
};
