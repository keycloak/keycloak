#!/bin/bash -e

awk '/:project_version:/ { print $2 }' topics/templates/document-attributes.adoc
