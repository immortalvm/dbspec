#!/usr/bin/env -S docker build . --tag=nr/dbspec-ex:1.2 --file

FROM nr/dbspec:1.2

ARG DEBIAN_FRONTEND=noninteractive
ARG TZ=Etc/UTC

RUN apt-get update && apt-get install -y \
    postgresql ruby \
    python3 python3-pip \
    git wget libxml2-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /

# Install eatb from PyPi. (As of version 0.1.19, this is still a bit messy.)
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY default_commands.cfg .
RUN mv default_commands.cfg $(python3 -c 'import site; print(site.getsitepackages()[0])')/eatb/resources/

USER postgres
RUN /etc/init.d/postgresql start \
    && psql -c "CREATE USER dbspec WITH CREATEDB ENCRYPTED PASSWORD 'secret';" \
    && psql -c "CREATE DATABASE dbspec OWNER dbspec;"

USER root
RUN echo "listen_addresses = '*'" >> /etc/postgresql/14/main/postgresql.conf
RUN echo "local   all             dbspec                                  scram-sha-256" >> /etc/postgresql/14/main/pg_hba.conf

RUN mkdir /spec
WORKDIR /spec

COPY <<"EOF" dbspec.conf
host=localhost
port=5432
user=dbspec
password=secret
EOF

CMD ["/usr/bin/env", "bash"]
