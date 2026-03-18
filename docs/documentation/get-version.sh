#!/usr/bin/env bash
set -e

awk '/:project_version:/ { print $2 }' topics/templates/document-attributes.adoc
