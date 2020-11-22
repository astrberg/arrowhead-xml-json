#!/bin/bash

# This script uses the Java `keytool` and the `openssl` command line utilities
# to create PKCS12 key stores and trust stores for the complete Arrowhead cloud
# required to run this example. Please refer to the following website for more
# details: https://arkalix.se/javadocs/kalix-base/se/arkalix/security/package-summary.html

cd "$(dirname "$0")" || exit
source "../../scripts/lib_certs.sh"
cd ..

# Root

create_root_keystore \
  "crypto/master.p12" "arrowhead.eu"

# Cloud

create_cloud_keystore \
  "crypto/master.p12" "arrowhead.eu" \
  "crypto/cloud.p12" "example-cloud.my-org.arrowhead.eu"

# Cloud Systems

## All systems are given X.509 Subject Alternative Names with both IP-addresses
## and DNS names. This is intended to make these certificates useful for
## being deployed using e.g. Docker.

create_cloud_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "crypto/master.p12" "arrowhead.eu" \
    "crypto/cloud.p12" "example-cloud.my-org.arrowhead.eu" \
    "crypto/system.${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.example-cloud.my-org.arrowhead.eu" \
    "${SYSTEM_SAN},dns:localhost,ip:127.0.0.1"
}

create_cloud_system_keystore "authorization"     "dns:authorization.local,ip:192.168.1.10"
create_cloud_system_keystore "orchestrator"      "dns:orchestrator.local,ip:192.168.1.11"
create_cloud_system_keystore "service_registry"  "dns:service-registry.local,ip:192.168.1.12"

create_cloud_system_keystore "echo_consumer"     "dns:echo-consumer.local,ip:192.168.1.20"
create_cloud_system_keystore "echo_provider"     "dns:echo-provider.local,ip:192.168.1.21"

# This certificate must be imported into your browser before being able to
# access the authorization and orchestration systems to add authorization and
# orchestration rules.
create_sysop_keystore \
  "crypto/master.p12" "arrowhead.eu" \
  "crypto/cloud.p12" "example-cloud.my-org.arrowhead.eu" \
  "crypto/sysop.p12" "sysop.example-cloud.my-org.arrowhead.eu"

## The same trust store can be used by all cloud systems, as the only
## certificate in the trust store is that of the cloud they are all
## members of.
create_truststore \
  "crypto/truststore.p12" \
  "crypto/master.crt" "arrowhead.eu"
