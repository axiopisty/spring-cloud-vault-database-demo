#!/bin/sh

SHARED_DATA_VOLUME=${SHARED_DATA_VOLUME:=""}

create_db() {
  local APP_NAME="${1}"
  local DATABASE_TYPE="${2}"
  local DB_HOST="${3}"
  local DB_PORT="${4}"
  local DB_USERNAME="${5}"
  local DB_PASSWORD="${6}"
  local SSL_MODE="${7}"
  local CONNECTION_URL="${DATABASE_TYPE}://${DB_USERNAME}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${APP_NAME}-db?sslmode=${SSL_MODE}"
  echo "Using DB_HOST of [${DB_HOST}]"
  echo "Using DB_PORT of [${DB_PORT}]"
  echo "Using DB_USERNAME of [${DB_USERNAME}]"
  echo "Using DB_PASSWORD of [${DB_PASSWORD}]"
  echo "Using SSL_MODE of [${SSL_MODE}]"
  echo "Using CONNCTION_URL of [${CONNECTION_URL}]"
  echo "Adding ${DATABASE_TYPE} connection information for ${APP_NAME} - connection string: ${CONNECTION_URL}"
  cat <<EOM | vault write dbs/config/${APP_NAME} -
{
  "plugin_name": "${DATABASE_TYPE}-database-plugin",
  "allowed_roles": ["${APP_NAME}-admin","${APP_NAME}-apps"],
  "connection_url": "${CONNECTION_URL}",
  "username": "${DB_USERNAME}",
  "password": "${DB_PASSWORD}",
  "verify_connection": false
}
EOM
}

create_db_admin_role() {
  local APP_NAME="${1}"
  local TTL="${2}"
  local MAX_TTL="${3}"
  local APP_NAME_LOWERCASE=$(echo ${APP_NAME} | tr '[:upper:]' '[:lower:]')
  local DB_ADMIN_ROLE="${APP_NAME_LOWERCASE}_admin"
  echo "Adding role ${DB_ADMIN_ROLE} to user ${APP_NAME}-admin"
  cat <<EOM | vault write dbs/roles/${APP_NAME}-admin -
{
  "db_name":"${APP_NAME}",
  "default_ttl":"${TTL}",
  "max_ttl":"${MAX_TTL}",
  "creation_statements":"CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' INHERIT IN ROLE \"${DB_ADMIN_ROLE}\"; GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \"{{name}}\""
}
EOM
}

create_db_app_role() {
  local APP_NAME="${1}"
  local TTL="${2}"
  local MAX_TTL="${3}"
  echo "Adding ${APP_NAME}-apps role"
  cat <<EOM | vault write dbs/roles/${APP_NAME}-apps -
{
  "db_name":"${APP_NAME}",
  "default_ttl":"${TTL}",
  "max_ttl":"${MAX_TTL}",
  "creation_statements": "CREATE ROLE \"{{name}}\" WITH SUPERUSER LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\""
}
EOM
}

create_policy_for_app() {
  local APP_NAME="${1}"
  local POLICY_NAME="${2}"
  local USES_DB="${3}"

  if [[ "${USES_DB}" -eq 1 ]]; then
    echo "Adding policy for ${POLICY_NAME} including permissions for db and secrets"
    cat <<EOM | vault policy write ${POLICY_NAME} -
      path "dbs/creds/${APP_NAME}-apps" {
        capabilities = ["read"]
      }

      path "secret/apps/${APP_NAME}/*" {
        capabilities = ["read"]
      }
EOM
  else
    echo "Adding policy for ${POLICY_NAME} including permissions for secrets"
    cat <<EOM | vault policy write ${POLICY_NAME} -
      path "secret/apps/${APP_NAME}/*" {
        capabilities = ["read"]
      }
EOM
  fi

}

create_token() {
# See https://www.vaultproject.io/docs/concepts/policies.html#tokens
  local APP_NAME="${1}"
  local POLICY_NAME="${2}"

  # vault token create -policy=dev-readonly -policy=logs

  vault token create -id="${APP_NAME}-prod-token" -policy="${POLICY_NAME}"
}

create_wrapped_token() {
  local APP_NAME="${1}"
  local POLICY_NAME="${2}"
  vault token create -policy=${POLICY_NAME} -wrap-ttl=42000m -field=wrapping_token
}

initialize_secrets_path() {
  local APP_NAME="${1}"
  local SECRETS_FILE="${SHARED_DATA_VOLUME}/${APP_NAME}/secrets.txt"

  if [[ ! -f "${SECRETS_FILE}" ]]; then
    echo "If you would like to import secrets into vault for ${APP_NAME} mount a file to the container with path ${SECRETS_FILE}."
  else
  	echo ""
    echo "Importing ${APP_NAME} secrets into vault from ${SECRETS_FILE}"
    while read line; do
      key=$(echo "${line}" | cut -f1 -d=)
      value=$(echo "${line}" | cut -f2 -d=)
      echo "vault kv put ""secret/apps/${APP_NAME}/${key}"" value=""${value}"""
      vault kv put "secret/apps/${APP_NAME}/${key}" value="${value}"
    done < "${SECRETS_FILE}"
    echo ""

		echo "Listing secrets stored in vault"
		vault kv list secret/apps/${APP_NAME}
		echo ""
  fi

}

initialize_application() {
  local APP_NAME="${1}"
  local APP_NAME_UPPERCASE_UNDERSCORE=$(echo ${APP_NAME} | tr '[:lower:]' '[:upper:]'| tr '-' '_')
  local APP_DATABASE="${APP_NAME_UPPERCASE_UNDERSCORE}_DATABASE_TYPE"
  local DATABASE_TYPE=$(eval echo "\${$APP_DATABASE}")
  local POLICY_NAME="${APP_NAME}-policy"


  echo ""
  echo "Using APP_NAME of [${APP_NAME}]"
  echo "Using POLICY_NAME of [${POLICY_NAME}]"

  local USES_DB=0
  if [[ -z "${DATABASE_TYPE}" ]]; then
    echo ""
    echo "set environment variable ${APP_DATABASE} if you want ${APP_NAME} to have a database"
    echo ""
    USES_DB=0
  else
    echo "Creating database of type ${DATABASE_TYPE} for ${APP_NAME}"
    local TTL=${TTL:="7200"} # 2 hours
    local MAX_TTL=${MAX_TTL:="2592000"} # 30 days
    local SSL_MODE=${SSL_MODE:="disable"}
    local DB_HOST=${DB_HOST:=database}
    local DB_PORT=${DB_PORT:=5432}
    local DB_USER=${DB_USER:="sqladmin"}
    local DB_PASSWORD=${DB_PASSWORD:="secret"}
    USES_DB=1
    echo "Using TTL of [${TTL}]"
    echo "Using MAX_TTL of [${MAX_TTL}]"
    create_db "${APP_NAME}" "${DATABASE_TYPE}" "${DB_HOST}" "${DB_PORT}" "${DB_USER}" "${DB_PASSWORD}" "${SSL_MODE}"
    create_db_admin_role "${APP_NAME}" "${TTL}" "${MAX_TTL}"
    create_db_app_role "${APP_NAME}" "${TTL}" "${MAX_TTL}"
  fi

  create_policy_for_app "${APP_NAME}" "${POLICY_NAME}" "${USES_DB}"
  create_token "${APP_NAME}" "${POLICY_NAME}"
  initialize_secrets_path "${APP_NAME}"

  local WRAPPED_TOKEN=$(create-wrapped-token "${APP_NAME}")
  echo "${WRAPPED_TOKEN}" > "/cubbyhole/${APP_NAME}.txt"
  echo "Created wrapped token (${WRAPPED_TOKEN}) in /cubbyhole/${APP_NAME}.txt"
}
