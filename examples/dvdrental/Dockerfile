#!/usr/bin/env -S docker build . --output=dist --file

FROM nr/dbspec-ex:1.2 AS build

RUN wget https://www.postgresqltutorial.com/wp-content/uploads/2019/05/dvdrental.zip
RUN unzip dvdrental.zip

COPY dvd.dbspec imdb_ratings.csv  .
RUN mkdir dist

RUN /etc/init.d/postgresql start \
    && ./dvd.dbspec

FROM scratch
COPY --from=build /spec/dist /
