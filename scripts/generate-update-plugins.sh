#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <version> <tag> <github-repository>" >&2
  exit 2
fi

version="$1"
tag="$2"
repository="$3"
asset="ideavim-strict-ime-${version}.zip"

cat <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<plugins>
  <plugin id="dev.local.ideavim.strictime"
          url="https://github.com/${repository}/releases/download/${tag}/${asset}"
          version="${version}" />
</plugins>
EOF
