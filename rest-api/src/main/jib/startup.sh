#!/usr/bin/env bash

if [[ -z "${TOKEN_PATH}" ]]; then
  echo "startup.sh: ERROR: Environment variable TOKEN_PATH is not set. It must specify the file containing the vault cubbyhole token."
  exit 1
fi

export VAULT_CUBBYHOLE_TOKEN="$(cat "${TOKEN_PATH}")"
echo "startup.sh: INFO: VAULT_CUBBYHOLE_TOKEN=${VAULT_CUBBYHOLE_TOKEN}"

JAVA_HEAP_INITIAL=${JAVA_HEAP_INITIAL:=768m}
JAVA_HEAP_MAX=${JAVA_HEAP_MAX:=768m}
JAVA_METASPACE_MAX=${JAVA_METASPACE_MAX:=256m}

JAVA_OPTS=${JAVA_OPTS:=""}
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"
JAVA_OPTS="${JAVA_OPTS} -Xms${JAVA_HEAP_INITIAL}"
JAVA_OPTS="${JAVA_OPTS} -Xmx${JAVA_HEAP_MAX}"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxMetaspaceSize=${JAVA_METASPACE_MAX}"
JAVA_OPTS="${JAVA_OPTS} -cp /app/resources:/app/classes:/app/libs/*"

echo "startup.sh: INFO: java ${JAVA_OPTS} com.github.axiopisty.scvdd.restapi.Main"

java ${JAVA_OPTS} com.github.axiopisty.scvdd.restapi.Main
