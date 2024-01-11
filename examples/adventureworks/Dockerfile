#!/usr/bin/env -S docker build . --output=dist --file

FROM nr/dbspec:1.0 AS build

ARG DEBIAN_FRONTEND=noninteractive
ARG TZ=Etc/UTC

RUN apt-get update && apt-get install -y \
    postgresql ruby \
    python3 python3-pip \
    git wget libxml2-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /
USER postgres
RUN /etc/init.d/postgresql start \
    && psql -c "CREATE USER dbspec WITH CREATEDB ENCRYPTED PASSWORD 'secret';" \
    && psql -c "CREATE DATABASE dbspec OWNER dbspec;"

USER root
RUN echo "listen_addresses = '*'" >> /etc/postgresql/14/main/postgresql.conf
RUN echo "local   all             dbspec                                  scram-sha-256" >> /etc/postgresql/14/main/pg_hba.conf

###

RUN mkdir /spec
COPY adventure.dbspec /spec/
WORKDIR /spec

COPY <<"EOF" dbspec.conf
host=localhost
port=5432
user=dbspec
password=secret
EOF

# Hint: dbspec -v4 for debug output
RUN /etc/init.d/postgresql start \
    && dbspec adventure.dbspec

###

FROM scratch
COPY --from=build /spec/dist /
