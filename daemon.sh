#!/bin/bash
# Make sure only root can run our script
if [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root" 1>&2
    exit 1
fi

ROOT_DIR=`pwd`

if [[ -d ".git" ]]
then
    APP_DIR="./"
else
    APP_DIR="./current/"
fi

echo $ROOT_DIR
echo $APP_DIR

if [[ -d ".git" && $3 = "bootstrap" ]]
then
    echo ".git still exists, bootstrapping directory structure"
    CURRENT_COMMIT=`git rev-parse HEAD`
    mkdir "revisions"
    mkdir "revisions/$CURRENT_COMMIT"
    `find ./ -maxdepth 1 \
             -mindepth 1 \
             \( ! -name revisions -a ! -name daemon.sh \) \
             -exec  mv {} revisions/$CURRENT_COMMIT/ \;`
    `ln -s revisions/$CURRENT_COMMIT current`
    echo "Commit $CURRENT_COMMIT is now linked to ./current"
    APP_DIR="./current/"
fi

while [ 1 ]
do
   if [[ -f "updating" || -f "new.rev" ]]
   then
     if [[ -f "updating" ]]
     then
       NEW_COMMIT=`cat old.rev`
       echo "Rolling back to $NEW_COMMIT"
     elif [[ -f "new.rev" ]]
     then
       NEW_COMMIT=`cat new.rev`
       touch "updating"
       echo "Updating to $NEW_COMMIT"
     fi
     `rm -r current`
     `ln -s revisions/$NEW_COMMIT current`
   fi
   echo Starting borglet
   cd $APP_DIR && LEIN_ROOT=true lein run $1 $2;
   cd $ROOT_DIR
   sleep 10;
done
