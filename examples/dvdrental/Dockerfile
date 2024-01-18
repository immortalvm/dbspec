#!/usr/bin/env -S docker build . --output=dist --file

FROM nr/dbspec-ex:1.0 AS build

COPY dvd.dbspec dvdrental.tar imdb_ratings.csv  .
RUN mkdir dist

RUN /etc/init.d/postgresql start \
    && ./dvd.dbspec

FROM scratch
COPY --from=build /spec/dist /
