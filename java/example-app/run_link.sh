#!/usr/bin/env bash

#if [ -z "$*" ]; then
#  echo "no args - default to 2"
#  ARGS=2
#else
#  ARGS=$*
#  echo "args: $ARGS"
#fi

ARGS=$*

source run_dependencies_env

#echo "command: .... -cp ${DEPENDENCIES} io.reticulum.examples.LinkApp $ARGS"

#  -verbose:class \
java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  -cp $DEPENDENCIES io.reticulum.examples.LinkApp $ARGS

#  -cp target/example-app-1.0-SNAPSHOT.jar:/home/jschulthess/.m2/repository/org/dizitart/nitrite/4.3.0/nitrite-4.3.0.jar:/home/jschulthess/.m2/repository/org/dizitart/nitrite-mvstore-adapter/4.3.0/nitrite-mvstore-adapter-4.3.0.jar:lib/io/reticulum/reticulum-network-stack/1.0-SNAPSHOT/reticulum-network-stack-1.0-SNAPSHOT.jar:/home/jschulthess/.m2/repository/commons-cli/commons-cli/1.8.0/commons-cli-1.8.0.jar:/home/jschulthess/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:/home/jschulthess/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar:/home/jschulthess/.m2/repository/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar:/home/jschulthess/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:/home/jschulthess/.m2/repository/org/apache/logging/log4j/log4j-api/2.23.1/log4j-api-2.23.1.jar:/home/jschulthess/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.23.1/log4j-slf4j-impl-2.23.1.jar:/home/jschulthess/.m2/repository/org/apache/logging/log4j/log4j-core/2.23.1/log4j-core-2.23.1.jar:/home/jschulthess/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar:/home/jschulthess/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.16.1/jackson-dataformat-yaml-2.16.1.jar:/home/jschulthess/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar:/home/jschulthess/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar:/home/jschulthess/.m2/repository/org/msgpack/msgpack-core/0.9.8/msgpack-core-0.9.8.jar:/home/jschulthess/.m2/repository/org/msgpack/jackson-dataformat-msgpack/0.9.8/jackson-dataformat-msgpack-0.9.8.jar:/home/jschulthess/.m2/repository/org/bouncycastle/bcpkix-jdk18on/1.77/bcpkix-jdk18on-1.77.jar:/home/jschulthess/.m2/repository/org/bouncycastle/bcprov-jdk15on/1.70/bcprov-jdk15on-1.70.jar:/home/jschulthess/.m2/repository/com/macasaet/fernet/fernet-java8/1.5.0/fernet-java8-1.5.0.jar:/home/jschulthess/.m2/repository/com/igormaznitsa/jbbp/2.0.6/jbbp-2.0.6.jar:/home/jschulthess/.m2/repository/com/igormaznitsa/jbbp/2.0.6/jbbp-2.0.6-javadoc.jar:/home/jschulthess/.m2/repository/io/netty/netty-all/5.0.0.Alpha2/netty-all-5.0.0.Alpha2.jar:/home/jschulthess/.m2/repository/com/github/seancfoley/ipaddress/5.4.2//ipaddress-5.4.2-javadoc.jar:/home/jschulthess/.m2/repository/org/yaml/snakeyaml/1.33/snakeyaml-1.33.jar io.reticulum.examples.LinkApp $ARGS


#  -cp target/example-app-1.0-SNAPSHOT.jar:lib/io/reticulum/reticulum-network-stack/1.0-SNAPSHOT/reticulum-network-stack-1.0-SNAPSHOT.jar:$HOME/.m2/repository/commons-cli/commons-cli/1.7.0/commons-cli-1.7.0.jar:$HOME/.m2/repository/org/apache/logging/log4j/log4j-core/2.23.1/log4j-core-2.23.1.jar:$HOME/.m2/repository/org/apache/logging/log4j/log4j-api/2.23.1/log4j-api-2.23.1.jar:$HOME/.m2/repository/org/bouncycastle/bcpkix-jdk15on/1.70/bcpkix-jdk15on-1.70.jar:$HOME/.m2/repository/org/bouncycastle/bcprov-jdk15on/1.70/bcprov-jdk15on-1.70.jar:$HOME/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:$HOME/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.23.1/log4j-slf4j-impl-2.23.1.jar:$HOME/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:$HOME/.m2/repository/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.14.3/jackson-databind-2.14.3.jar:$HOME/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.14.3/jackson-dataformat-yaml-2.14.3.jar:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.3/jackson-core-2.14.3.jar:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.14.3/jackson-annotations-2.14.3.jar:$HOME/.m2/repository/org/yaml/snakeyaml/1.33/snakeyaml-1.33.jar:$HOME/.m2/repository/io/netty/netty-all/5.0.0.Alpha2/netty-all-5.0.0.Alpha2.jar:$HOME/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar:$HOME/.m2/repository/org/msgpack/msgpack-core/0.9.6/msgpack-core-0.9.6.jar:$HOME/.m2/repository/org/msgpack/jackson-dataformat-msgpack/0.9.3/jackson-dataformat-msgpack-0.9.3.jar:$HOME/.m2/repository/com/igormaznitsa/jbbp/2.0.4/jbbp-2.0.4.jar:$HOME/.m2/repository/com/github/seancfoley/ipaddress/5.4.2/ipaddress-5.4.2.jar:$HOME/.m2/repository/com/macasaet/fernet/fernet-java8/1.4.2/fernet-java8-1.4.2.jar:$HOME/.m2/repository/org/dizitart/nitrite/4.3.0/nitrite-4.3.0.jar:$HOME/.m2/repository/org/dizitart/nitrite-mvstore-adapter/4.3.0/nitrite-mvstore-adapter-4.3.0.jar io.reticulum.examples.LinkApp $ARGS



