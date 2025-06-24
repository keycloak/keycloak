window.onCookieJsLoad = function ({basePath, screenName, realm}) {
  var c_userUUID = cookie.get("c_userUUID"),
    c_env = cookie.get("c_env");
  document.head.setAttribute(
    "data-request-ids",
    JSON.stringify({
      request_uuid: window.crypto.randomUUID(),
      user_uuid: c_userUUID || window.crypto.randomUUID(),
    })
  );
  document.head.setAttribute(
    "data-trackpoint-meta",
    JSON.stringify({
      type: "page_view",
      environment: c_env,
      event_source: "client",
      screenName,
      app: realm
    })
  );
  var trackpointScript = document.createElement("script");
  trackpointScript.type = "module";
  trackpointScript.src = "https://d2ywvfgjza5nzm.cloudfront.net/trackpoint.js";
  document.head.appendChild(trackpointScript);
};

window.buildTPClickEvent = function ({ ...params }) {
  JSON.stringify({
    type: params.type || "click",
    environment: params.environment,
    event_source: params.event_source || "client",
    ...params,
  });
};

window.buildTrackingData = function ({
  event,
  eventSource,
  sectionName,
  actionType,
  actionValue = "",
  sectionTitle = "",
  subSectionTitle = "",
  componentTitle = "",
  screen = "",
}) {
  return {
    event_source: eventSource,
    screen,
    event,
    action: { type: actionType, value: actionValue },
    section_name: sectionName,
    content_data: {
      section_title: sectionTitle,
      sub_section_title: subSectionTitle,
      component_title: componentTitle,
    },
  };
};
