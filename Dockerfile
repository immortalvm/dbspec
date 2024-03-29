#!/usr/bin/env -S docker build . --tag=nr/dbspec:1.1 --file
# Cf. https://stackoverflow.com/a/74532086

FROM docker.io/library/ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y \
    python3 python3-pip \
    openjdk-11-jdk-headless ant \
    gcc

# TODO: Make architecture independent
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64
ENV LANG C.UTF-8

COPY . /dbspec
WORKDIR dbspec

# On windows we may have lost the execution bit.
RUN chmod 755 build.py
RUN ant clean && ant create_run_jar

###

FROM docker.io/library/ubuntu:22.04

RUN apt-get update && apt-get install -y \
    openjdk-11-jdk-headless ant \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /dbspec/dbspec.jar /dbspec/*.so /dbspec/
COPY --from=build /dbspec/lib/*.jar /dbspec/lib/
COPY --chmod=755 <<"EOF" /usr/local/bin/dbspec
#!/usr/bin/env bash
$JAVA_HOME/bin/java -jar /dbspec/dbspec.jar $@
EOF

WORKDIR /root
CMD ["/usr/bin/env", "bash"]
