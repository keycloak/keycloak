#!/bin/bash -e

if mvn dependency:tree -Dverbose | grep -E '(-dev:|-devtools-)'; then
  echo "[WARNING] Detected development dependencies in the build tree."
fi