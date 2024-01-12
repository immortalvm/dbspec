#!/usr/bin/env -S docker build . --output=dist --file

FROM nr/dbspec-ex:1.0 AS build

COPY adventure.dbspec .

# Hint: dbspec -v4 for debug output
RUN /etc/init.d/postgresql start \
    && ./adventure.dbspec

FROM scratch
COPY --from=build /spec/dist /
