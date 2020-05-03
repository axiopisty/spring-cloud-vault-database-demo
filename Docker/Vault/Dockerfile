FROM vault:1.1.3

ENV VAULT_ADDR="http://localhost:8200/"

COPY vault-functions.sh /vault-functions.sh
COPY create-wrapped-token.sh /bin/create-wrapped-token
COPY init-vault.sh /init-vault.sh

CMD /init-vault.sh
