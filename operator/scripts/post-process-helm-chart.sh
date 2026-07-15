#!/usr/bin/env bash
# Post-processes the chart emitted by quarkus-helm so it covers things the
# extension cannot express today.
#
# CRDs: quarkus-operator-sdk-helm writes them into the chart's crds/ directory
# verbatim, which Helm installs unconditionally. Move them into templates/
# wrapped in a {{ if .Values.crds.enabled }} guard so users can manage CRDs
# out-of-band.
#
# RBAC subject namespaces: the (Cluster)RoleBinding subjects reference the
# operator ServiceAccount without a namespace (the non-Helm install relies on
# kustomize to fill these in). A ClusterRoleBinding ServiceAccount subject with
# no namespace is rejected by the API server, and the base binding hardcodes
# "keycloak"; both are wrong for a Helm release installed into an arbitrary
# namespace. Rewrite every ServiceAccount subject to the release namespace.
#
# NOTE: quarkus-helm is pinned to 1.2.7 because quarkus-operator-sdk-helm is
# pinned to 7.4.0 (the last version before 7.5.0's template-injection breaks the
# chart). quarkus-helm 1.3.0+ populates empty subject namespaces on its own. Once
# a newer quarkus-operator-sdk-helm is released, upgrade it ASAP and bump
# quarkus-helm past 1.3.0, then delete the subject-namespace rewriting below.
set -euo pipefail

CHART_DIR="${1:?usage: post-process-helm-chart.sh <chart-dir>}"
CRDS_DIR="${CHART_DIR}/crds"
TEMPLATES_CRDS_DIR="${CHART_DIR}/templates/crds"
VALUES_FILE="${CHART_DIR}/values.yaml"
TEMPLATES_DIR="${CHART_DIR}/templates"

if [[ -d "${CRDS_DIR}" ]]; then
  mkdir -p "${TEMPLATES_CRDS_DIR}"
  shopt -s nullglob
  for crd in "${CRDS_DIR}"/*.yml "${CRDS_DIR}"/*.yaml; do
    dest="${TEMPLATES_CRDS_DIR}/$(basename "${crd}")"
    {
      echo '{{- if .Values.crds.enabled }}'
      cat "${crd}"
      echo '{{- end }}'
    } > "${dest}"
    rm "${crd}"
  done
  rmdir "${CRDS_DIR}" 2>/dev/null || true
  echo "post-process-helm-chart: wrapped CRDs into ${TEMPLATES_CRDS_DIR}"
fi

if ! grep -q '^crds:' "${VALUES_FILE}" 2>/dev/null; then
  printf '\ncrds:\n  enabled: true\n' >> "${VALUES_FILE}"
fi

# Set every ServiceAccount subject's namespace to the release namespace. For
# each subject, replace an existing namespace line or inject one right after the
# "name:" line, preserving indentation.
for binding in "${TEMPLATES_DIR}/clusterrolebinding.yaml" "${TEMPLATES_DIR}/rolebinding.yaml"; do
  [[ -f "${binding}" ]] || continue
  tmp="${binding}.tmp"
  awk '
    function flush_pending() {
      if (pending) { print name_line; print indent "namespace: {{ .Release.Namespace }}"; pending=0 }
    }
    {
      if (pending) {
        if ($0 ~ /^[[:space:]]*namespace:[[:space:]]/) {
          match($0, /^[[:space:]]*/); ind = substr($0, 1, RLENGTH)
          print name_line; print ind "namespace: {{ .Release.Namespace }}"; pending=0; next
        } else { flush_pending() }
      }
      if ($0 ~ /kind:[[:space:]]*ServiceAccount[[:space:]]*$/) { sa=1; print; next }
      if (sa && $0 ~ /^[[:space:]]*name:[[:space:]]/) {
        match($0, /^[[:space:]]*/); indent = substr($0, 1, RLENGTH)
        name_line = $0; pending=1; sa=0; next
      }
      print
    }
    END { flush_pending() }
  ' "${binding}" > "${tmp}"
  mv "${tmp}" "${binding}"
  echo "post-process-helm-chart: set ServiceAccount subject namespaces in $(basename "${binding}")"
done
