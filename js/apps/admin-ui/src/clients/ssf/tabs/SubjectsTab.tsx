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
  InputGroup,
  InputGroupItem,
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
import { useRealm } from "../../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../../util";
import { getAuthorizationHeaders } from "../../../utils/getAuthorizationHeaders";

type SubjectType = "user-id" | "user-email" | "user-username" | "org-alias";
type SubjectState = "notified" | "ignored" | "not_notified";

export type SubjectsTabProps = {
  client: ClientRepresentation;
};

export const SubjectsTab = ({ client }: SubjectsTabProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
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

  const ssfNotifyKey = `ssf.notify.${client.clientId}`;

  const callSubjectAdminEndpoint = async (
    action: "subjects/add" | "subjects/remove" | "subjects/ignore",
  ) => {
    const baseUrl = addTrailingSlash(adminClient.baseUrl);
    const res = await fetch(
      `${baseUrl}admin/realms/${realm}/ssf/clients/${client.clientId}/${action}`,
      {
        method: "POST",
        headers: {
          ...getAuthorizationHeaders(await adminClient.getAccessToken()),
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ type: subjectType, value: subjectValue }),
      },
    );
    return res;
  };

  const checkSubjectViaAdminApi = async () => {
    const baseUrl = addTrailingSlash(adminClient.baseUrl);
    const headers = getAuthorizationHeaders(await adminClient.getAccessToken());

    let attrs: Record<string, string[]> = {};

    if (subjectType === "user-id") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/users/${encodeURIComponent(subjectValue)}`,
        { headers },
      );
      if (!res.ok) return null;
      const user = await res.json();
      attrs = user.attributes ?? {};
    } else if (subjectType === "user-email") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/users?email=${encodeURIComponent(subjectValue)}&exact=true&max=1&briefRepresentation=false`,
        { headers },
      );
      if (!res.ok) return null;
      const users = await res.json();
      if (!Array.isArray(users) || users.length === 0) return null;
      attrs = users[0].attributes ?? {};
    } else if (subjectType === "user-username") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/users?username=${encodeURIComponent(subjectValue)}&exact=true&max=1&briefRepresentation=false`,
        { headers },
      );
      if (!res.ok) return null;
      const users = await res.json();
      if (!Array.isArray(users) || users.length === 0) return null;
      attrs = users[0].attributes ?? {};
    } else if (subjectType === "org-alias") {
      const res = await fetch(
        `${baseUrl}admin/realms/${realm}/organizations?search=${encodeURIComponent(subjectValue)}&exact=true&first=0&max=1`,
        { headers },
      );
      if (!res.ok) return null;
      const orgs = await res.json();
      if (!Array.isArray(orgs) || orgs.length === 0) return null;
      attrs = orgs[0].attributes ?? {};
    }

    const values = attrs[ssfNotifyKey];
    if (!values) return "not_notified";
    if (values.includes("true")) return "notified";
    if (values.includes("false")) return "ignored";
    return "not_notified";
  };

  const applySubjectState = (state: SubjectState) => {
    if (state === "notified") {
      setSubjectStatus({
        variant: "success",
        message: t("ssfSubjectIsNotified"),
      });
    } else if (state === "ignored") {
      setSubjectStatus({
        variant: "danger",
        message: t("ssfSubjectIsIgnored"),
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
        const state = await checkSubjectViaAdminApi();
        if (state === null) {
          setSubjectValueError(t("ssfSubjectNotFound"));
        } else {
          applySubjectState(state);
        }
        return;
      }

      const endpoint =
        action === "add"
          ? "subjects/add"
          : action === "ignore"
            ? "subjects/ignore"
            : "subjects/remove";
      const res = await callSubjectAdminEndpoint(endpoint);

      if (res.status === 404) {
        setSubjectValueError(t("ssfSubjectNotFound"));
        return;
      }
      if (!res.ok) {
        const body = await res.text();
        throw new Error(`${res.status} ${body || res.statusText}`);
      }

      const data = await res.json();
      const state = data?.status as SubjectState | undefined;
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
            <InputGroup>
              <InputGroupItem isFill>
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
              </InputGroupItem>
            </InputGroup>
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
