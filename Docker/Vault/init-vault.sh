#!/bin/sh

if [[ -z "${APP_NAMES}" ]]; then
  echo "Please declare an APP_NAMES environment variable containing a comma delimited list of applications needing to use Vault"
  exit 1;
fi

echo "Using APP_NAMES=${APP_NAMES}"
echo ""

source ./vault-functions.sh

# Capture the token defined in the environment and make it our dev server token so they match
export VAULT_DEV_ROOT_TOKEN_ID="ROOT_TOKEN"
export VAULT_ADDR="http://127.0.0.1:8200"
export VAULT_DEV_LISTEN_ADDRESS="0.0.0.0:8200"

# Start vault server and place in background
vault server -log-level=trace -dev > /token_volume/server.log 2>&1 &

# Wait for server to start
sleep 2

echo "Enabling audit logs to file: /token_volume/audit.log"
vault audit enable file file_path=/token_volume/audit.log log_raw=true
echo ""

echo "Enabling database plugin"
vault secrets enable -path=dbs database
echo ""

echo "Disabling KV version 2 at secrets/"
vault secrets disable secret/
echo ""

echo "Enabling KV version 1 at secrets/"
vault secrets enable -path="secret" -version=1 kv
echo ""

for APP_NAME in $(echo ${APP_NAMES} | sed "s/,/ /g")
do
    initialize_application "${APP_NAME}"
done

#echo "${VAULT_DEV_ROOT_TOKEN_ID}" > /token_volume/root_token.txt
#echo "ROOT_TOKEN: ${VAULT_DEV_ROOT_TOKEN_ID} written to /token_volume/root_token.txt"

#echo "vault secrets list -detailed"
#vault secrets list -detailed
#echo ""

echo "Vault is initialized and ready to be used."

# Wait for vault server to finish
wait

