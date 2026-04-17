import { createSearchParams } from "react-router-dom";
import type { Path } from "react-router-dom";
import { toEvents } from "../routes/Events";

type EventHookLogsParams = {
  realm: string;
  sourceType?: string;
  targetId?: string;
  targetType?: string;
  event?: string;
  status?: string;
  messageStatus?: string;
  executionId?: string;
  messageId?: string;
  search?: string;
};

type EventHookTargetsParams = {
  realm: string;
  targetId?: string;
};

export const toEventHookLogs = (
  params: EventHookLogsParams,
): Partial<Path> => {
  const searchParams = new URLSearchParams();

  Object.entries({
    sourceType: params.sourceType,
    targetId: params.targetId,
    targetType: params.targetType,
    event: params.event,
    status: params.status,
    messageStatus: params.messageStatus,
    executionId: params.executionId,
    messageId: params.messageId,
    search: params.search,
  }).forEach(([key, value]) => {
    if (value) {
      searchParams.set(key, value);
    }
  });

  const query = createSearchParams(searchParams).toString();

  return {
    pathname: toEvents({ realm: params.realm, tab: "hooks", subTab: "logs" })
      .pathname,
    search: query ? `?${query}` : "",
  };
};

export const toEventHookTargets = (
  params: EventHookTargetsParams,
): Partial<Path> => {
  const searchParams = new URLSearchParams();

  if (params.targetId) {
    searchParams.set("targetId", params.targetId);
  }

  const query = createSearchParams(searchParams).toString();

  return {
    pathname: toEvents({ realm: params.realm, tab: "hooks", subTab: "targets" })
      .pathname,
    search: query ? `?${query}` : "",
  };
};