# Copyright 2023 OpenSPG Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License
# is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied.

FROM docker.m.daocloud.io/ubuntu:20.04

# For apt to be noninteractive
ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_NONINTERACTIVE_SEEN true
ENV LANG C.UTF-8
ENV PATH=/home/admin/miniconda3/bin:$PATH

ARG TARGETPLATFORM
ARG APT_INSTALL="apt-get install --no-install-recommends -y"
ARG MINICONDA_FILE

RUN apt-get clean && apt-get update && \
    $APT_INSTALL ca-certificates && \
    $APT_INSTALL ca-certificates-java && \
    $APT_INSTALL git less vim wget curl telnet nano jq procps net-tools unzip zip tar gzip nfs-common && \
    # Install openjdk 8.
    $APT_INSTALL openjdk-8-jdk && \
    update-alternatives --set java /usr/lib/jvm/java-8-openjdk-$(dpkg --print-architecture)/jre/bin/java && \
    # Install build / source control tools
    $APT_INSTALL wget git maven less vim

RUN if [ "${TARGETPLATFORM}" = "linux/amd64" ]; then \
      export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64; \
    else \
      export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64; \
      $APT_INSTALL gcc-aarch64-linux-gnu; \
      $APT_INSTALL build-essential libpython3.8-dev; \
    fi && \
    echo "export JAVA_HOME=${JAVA_HOME}" >> /etc/profile

RUN . /etc/profile && echo ${JAVA_HOME} && mkdir -p /home/admin/ && chmod -R 777 /home/admin &&\
    # wget -q http://hyperloop.cn-hangzhou.alipay.aliyun-inc.com/kgrl/user/zhongshu.zzs/tmp/Miniconda3-py310_25.1.1-2-Linux-x86_64.sh && \
    wget -q http://hyperloop.cn-hangzhou.alipay.aliyun-inc.com/kgrl/user/zhongshu.zzs/tmp/$MINICONDA_FILE && \
    ln -s /usr/bin/md5sum /usr/bin/md5 &&\
    bash $MINICONDA_FILE -b -u -p /home/admin/miniconda3 &&\
    rm -rf $MINICONDA_FILE && \
    /home/admin/miniconda3/bin/conda init &&\
    pip install openspg-kag==0.7.0 &&\
    pip install pemja==0.4.0 && \
    pip cache purge
