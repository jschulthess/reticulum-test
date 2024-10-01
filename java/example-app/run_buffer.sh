#!/usr/bin/env bash

ARGS=$*

source run_dependencies_env

#echo "command: .... -cp ${DEPENDENCIES} io.reticulum.examples.MeshApp $ARGS"

java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  -cp $DEPENDENCIES io.reticulum.examples.BufferApp $ARGS

