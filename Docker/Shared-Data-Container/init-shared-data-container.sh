#!/bin/sh

if [[ -z "${SHARED_DATA_VOLUME}" ]]; then
  echo "Please specify the SHARED_DATA_VOLUME environment variable"
  exit 1
fi

create_directory_in_shared_volume() {
  local DIR_NAME="${1}"
  if [[ -d "${DIR_NAME}" ]]; then
    echo "rm -rf ${DIR_NAME}"
    rm -rf "${DIR_NAME}"
  fi
  echo "mkdir -p ${DIR_NAME}"
  mkdir -p "${DIR_NAME}"
}

###############################################################################
# Copy secret files from /secret-files into SHARED_DATA_VOLUME
###############################################################################
for file in $(ls /secret-files); do
  SECRET_FILE_DIR="${SHARED_DATA_VOLUME}/${file}"
  create_directory_in_shared_volume "${SECRET_FILE_DIR}"
  echo "cat /secret-files/${file} > ${SECRET_FILE_DIR}/secrets.txt"
  cat "/secret-files/${file}" > "${SECRET_FILE_DIR}/secrets.txt"
done


echo "Shared Data Container initialized and ready to be used"

while true; do
  sleep 1
done
