#!/bin/bash
#############################################################################
# Copyright (c) 2023 Huawei Technologies Co.,Ltd.
#
# openGauss is licensed under Mulan PSL v2.
# You can use this software according to the terms
# and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
#
#   http://license.coscl.org.cn/MulanPSL2
#
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
# ----------------------------------------------------------------------------
# Description  : shell script for build datakit and plugins.
#############################################################################
mvn --threads=4C clean
# rm -rf /Users/wangziqi/.m2/repository/*
#mvn --threads=4C clean install -Dbuild.frontend.skip=true -Dweb.build.skip=true -Dmaven.test.skip=true
#mvn --threads=1C -T 1 clean install -Dmaven.test.skip=true
mvn clean install -Dmaven.test.skip=true

version_num=7.0.0-RC3

rm -rf visualtool-plugin/*
rm -rf openGauss-datakit-${version_num}.jar

cp plugins/base-ops/target/base-ops-${version_num}-repackage.jar  visualtool-plugin/
cp plugins/er-model/target/er-model-${version_num}-repackage.jar  visualtool-plugin/

#function fetch_build_install_spring_brick() {
#    cd $root_path
#    git clone https://gitcode.com/wang4721/springboot-plugin-framework-parent.git
#    cd springboot-plugin-framework-parent
#    echo "build dir:${root_path} ,to run cmd: mvn clean install -Dmaven.test.skip=true"
#    mvn clean install -Dmaven.test.skip=true
#    if [ $? -ne 0 ]; then
#      echo "Build spring brick failed..."
#      exit 1
#    fi
#    cd ..
#    rm -rf springboot-plugin-framework-parent
#}