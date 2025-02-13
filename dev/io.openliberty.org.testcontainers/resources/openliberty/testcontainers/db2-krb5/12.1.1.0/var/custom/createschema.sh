#!/bin/sh

source ${SETUPDIR?}/include/db2_constants

chmod 777 /etc/krb5.keytab
chmod 777 ${DB2DIR?}/adm/db2start

hostname db2
sudo -i -u db2inst1 bash << EOF
export DB2_KRB5_PRINCIPAL=db2srvc@EXAMPLE.COM
${DB2DIR?}/bin/db2 UPDATE DATABASE MANAGER CONFIGURATION USING CLNT_KRB_PLUGIN IBMkrb5
${DB2DIR?}/bin/db2 UPDATE DATABASE MANAGER CONFIGURATION USING AUTHENTICATION KERBEROS

kinit -k -t /etc/krb5.keytab db2inst1
${DB2DIR?}/adm/db2start
${DB2DIR?}/bin/db2 db2stop
${DB2DIR?}/bin/db2 db2start

${DB2DIR?}/bin/db2 CREATE DATABASE TESTDB

echo "SETUP SCRIPT COMPLETE"
EOF