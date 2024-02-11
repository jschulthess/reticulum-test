#!/usr/bin/env bash

java \
-cp target/example-app-1.0-SNAPSHOT.jar \
com.mobilefabrik.app.ByteToHex1

java \
-cp target/example-app-1.0-SNAPSHOT.jar \
com.mobilefabrik.app.ByteToHex2

java \
-cp target/example-app-1.0-SNAPSHOT.jar \
com.mobilefabrik.app.ByteToHex3

java \
-cp target/example-app-1.0-SNAPSHOT.jar \
com.mobilefabrik.app.ByteToHex4

java \
-cp target/example-app-1.0-SNAPSHOT.jar \
com.mobilefabrik.app.ByteToHex5
#com.mobilefabrik.app.Hello

# second lot
java \
-cp target/example-app-1.0-SNAPSHOT.jar \
com.mobilefabrik.app.ByteHex1

java \
-cp target/example-app-1.0-SNAPSHOT.jar:$HOME/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar \
com.mobilefabrik.app.ByteHex2
