#!/usr/bin/env bash
# Post-processes the chart emitted by quarkus-helm so it covers things the
# extension cannot express today.
#
# CRDs: quarkus-operator-sdk-helm adds them to the chart's crds/ directory
# during the build, which Helm installs unconditionally. Move
# them into templates/ wrapped in a {{ if .Values.crds.enabled }} guard so
# users can manage CRDs out-of-band.
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

# Replace {{ $.Chart.Name }} with the fixed operator name across all templates
for tmpl in "${TEMPLATES_DIR}"/*.yaml "${TEMPLATES_DIR}"/*.yml; do
  [[ -f "${tmpl}" ]] || continue
  tmp="${tmpl}.tmp"
  sed -e 's/{{ \$\.Chart\.Name }}/keycloak-operator/g' \
      -e 's/{{ \.Chart\.Name }}/keycloak-operator/g' "${tmpl}" > "${tmp}"
  mv "${tmp}" "${tmpl}"
done
echo "post-process-helm-chart: replaced Chart.Name references in templates"
