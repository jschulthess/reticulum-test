#!/usr/bin/env bash

ARGS=$*

source run_dependencies.env

#echo "command: .... -cp ${DEPENDENCIES} io.reticulum.examples.MeshAppBuffer $ARGS"

java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  -cp $DEPENDENCIES io.reticulum.examples.MeshAppBuffer $ARGS




