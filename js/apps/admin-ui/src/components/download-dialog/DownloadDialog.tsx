import { fetchWithError } from "@keycloak/keycloak-admin-client";
import { HelpItem, useFetch, useHelp } from "@keycloak/keycloak-ui-shared";
import {
  Form,
  FormGroup,
  MenuToggle,
  ModalVariant,
  Select,
  SelectList,
  SelectOption,
  Stack,
  StackItem,
  TextArea,
} from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { addTrailingSlash, prettyPrintJSON } from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { ConfirmDialogModal } from "../confirm-dialog/ConfirmDialog";

type DownloadDialogProps = {
  id: string;
  protocol?: string;
  open: boolean;
  toggleDialog: () => void;
};

export const DownloadDialog = ({
  id,
  open,
  toggleDialog,
  protocol = "openid-connect",
}: DownloadDialogProps) => {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { t } = useTranslation();
  const { enabled } = useHelp();
  const serverInfo = useServerInfo();

  const configFormats = serverInfo.clientInstallations![protocol];
  const [selected, setSelected] = useState(
    configFormats[configFormats.length - 1].id,
  );
  const [snippet, setSnippet] = useState<string | ArrayBuffer>();
  const [openType, setOpenType] = useState(false);

  const selectedConfig = useMemo(
    () => configFormats.find((config) => config.id === selected) ?? null,
    [selected],
  );

  const sanitizeSnippet = (snippet: string) =>
    snippet.replace(
      /<PrivateKeyPem>.*<\/PrivateKeyPem>/gs,
      `<PrivateKeyPem>${t("privateKeyMask")}</PrivateKeyPem>`,
    );

  useFetch(
    async () => {
      if (selectedConfig?.mediaType === "application/zip") {
        const response = await fetchWithError(
          `${addTrailingSlash(
            adminClient.baseUrl,
          )}admin/realms/${realm}/clients/${id}/installation/providers/${selected}`,
          {
            method: "GET",
            headers: getAuthorizationHeaders(
              await adminClient.getAccessToken(),
            ),
          },
        );

        return response.arrayBuffer();
      } else {
        const snippet = await adminClient.clients.getInstallationProviders({
          id,
          providerId: selected,
        });
        if (typeof snippet === "string") {
          return sanitizeSnippet(snippet);
        } else {
          return prettyPrintJSON(snippet);
        }
      }
    },
    (snippet) => setSnippet(snippet),
    [id, selected],
  );

  // Clear snippet when selected config changes, this prevents old snippets from being displayed during fetch.
  useEffect(() => setSnippet(""), [id, selected]);

  return (
    <ConfirmDialogModal
      titleKey={t("downloadAdaptorTitle")}
      continueButtonLabel={t("download")}
      onConfirm={() => {
        saveAs(
          new Blob([snippet!], { type: selectedConfig?.mediaType }),
          selectedConfig?.filename,
        );
      }}
      open={open}
      toggleDialog={toggleDialog}
      variant={ModalVariant.medium}
    >
      <Form>
        <Stack hasGutter>
          <StackItem>
            <FormGroup
              fieldId="type"
              label={t("formatOption")}
              labelIcon={
                <HelpItem
                  helpText={t("downloadType")}
                  fieldLabelId="formatOption"
                />
              }
            >
              <Select
                isOpen={openType}
                onOpenChange={(isOpen) => setOpenType(isOpen)}
                toggle={(ref) => (
                  <MenuToggle
                    id="type"
                    ref={ref}
                    onClick={() => setOpenType(!openType)}
                    isExpanded={openType}
                  >
                    {selected}
                  </MenuToggle>
                )}
                selected={selected}
                onSelect={(_, value) => {
                  setSelected(value?.toString() || "");
                  setOpenType(false);
                }}
                aria-label={t("selectOne")}
                popperProps={{
                  appendTo: document.body,
                }}
              >
                <SelectList>
                  {configFormats.map((configFormat) => (
                    <SelectOption
                      key={configFormat.id}
                      value={configFormat.id}
                      isSelected={selected === configFormat.id}
                      description={enabled ? configFormat.helpText : undefined}
                    >
                      {configFormat.displayType}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
            </FormGroup>
          </StackItem>
          {!selectedConfig?.downloadOnly && (
            <StackItem isFilled>
              <FormGroup
                fieldId="details"
                label={t("details")}
                labelIcon={
                  <HelpItem
                    helpText={t("detailsHelp")}
                    fieldLabelId="details"
                  />
                }
              >
                <TextArea
                  id="details"
                  readOnly
                  rows={12}
                  resizeOrientation="vertical"
                  value={snippet && typeof snippet === "string" ? snippet : ""}
                  aria-label="text area example"
                />
              </FormGroup>
            </StackItem>
          )}
        </Stack>
      </Form>
    </ConfirmDialogModal>
  );
};
