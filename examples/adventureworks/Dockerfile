#!/usr/bin/env -S docker build . --output=dist --file

FROM nr/dbspec-ex:1.3 AS build

COPY adventure.dbspec .
RUN /etc/init.d/postgresql start \
    && ./adventure.dbspec

# For inclusion in the SIP
# TODO: Include the dockerfiles in .. and ../.. as well.
# (The problem is that Docker COPY will not allow this.)
COPY Dockerfile .

# By using a separate DbSpec file, we can exploit the caching mechanism of
# Docker. Thus, we don't have to recreate the SIARD file while developing
# the SIP creation script.
COPY package.dbspec .
RUN ./package.dbspec

## This does not make much sense when using Docker for building.
# COPY cleanup.dbspec .
# RUN /etc/init.d/postgresql start \
#     && ./cleanup.dbspec

FROM scratch
COPY --from=build /spec/dist /
