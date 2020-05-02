#!/bin/sh

source /vault-functions.sh

export VAULT_CLI_NO_COLOR=true

if [[ -z "${1}" ]]; then
  echo "Please specify the APP_NAME for which the wrapped token should be created"
  exit 1
fi

APP="${1}"
POLICY="${APP}-policy"

create_wrapped_token "${APP}" "${POLICY}"
