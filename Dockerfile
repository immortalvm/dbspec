#!/usr/bin/env -S docker build . --tag=nr/dbspec:1.3 --secret id=GITHUB_ACTOR --secret id=GITHUB_TOKEN --file
# Cf. https://stackoverflow.com/a/74532086

FROM docker.io/library/ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y \
    openjdk-11-jdk-headless

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV LANG=C.UTF-8

COPY . /dbspec
WORKDIR /dbspec

# On windows we may have lost the execution bit.
RUN chmod 755 ./gradlew
RUN --mount=type=secret,id=GITHUB_ACTOR \
    --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_ACTOR=$(cat /run/secrets/GITHUB_ACTOR) && \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    ./gradlew clean distZip

###

FROM docker.io/library/ubuntu:22.04

RUN apt-get update && apt-get install -y \
    openjdk-11-jdk-headless unzip \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /dbspec/build/distributions/dbspec-*.zip /dbspec/

WORKDIR /dbspec
RUN unzip *.zip
RUN rm dbspec-*.zip

RUN ln -s /dbspec/bin/dbspec /usr/local/bin/dbspec

WORKDIR /root
CMD ["/usr/bin/env", "bash"]
