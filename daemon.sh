#!/bin/bash
# Make sure only root can run our script
if [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root" 1>&2
    exit 1
fi

while [ 1 ]
do
   LEIN_ROOT=true lein run $1 $2;
   sleep 10;
done
