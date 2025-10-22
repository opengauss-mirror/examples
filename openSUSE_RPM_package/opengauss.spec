#
# spec file for package openGauss-server
#
# Copyright (c) 2025 SUSE LLC
#
# All modifications and additions to the file contributed by third parties
# remain the property of their copyright owners, unless otherwise agreed
# upon. The license for this file, and modifications and additions to the
# file, is the same license as for the pristine package itself (unless the
# license for the pristine package is not an Open Source License, in which
# case the license is the MIT License). An "Open Source License" is a
# license that conforms to the Open Source Definition (Version 1.9)
# published by the Open Source Initiative.

# Please submit bugfixes or comments via https://bugs.opensuse.org/
#

%define krb5_name krb5
%define krb5_version 1.18.3-final
%define xgboost_name xgboost
%define xgboost_version 1.4.1
%define dmlc_name dmlc-core
%define dmlc_version 0.5
%define securec_name Huawei_Secure_C
%define securec_version V100R001C01SPC010B002
%define dcf_name DCF
%define dcf_version v6.0.0
%define awssdk_name aws-sdk-cpp
%define awssdk_version 1.11.327
%define port 7654
%define datapath /var/lib/opengauss
%define apppath %{_prefix}/local/opengauss
%define tmppath /var/lib/opengauss/pkg_6.0.0

Name:           opengauss
Version:        6.0.0
Release:        0
Summary:        An open source relational database management system
License:        MulanPSL-2.0 and MIT and TCL and Apache-2.0 and BSL-1.0
URL:            https://gitcode.com/opengauss/openGauss-server
Source0:        openGauss-server-%{version}.tar.gz
Source1:        %{krb5_name}-%{krb5_version}.tar.gz
Source2:        %{dmlc_name}-v%{dmlc_version}.tar.gz
Source3:        %{xgboost_name}-v%{xgboost_version}.tar.gz
Source4:        %{securec_name}_%{securec_version}.zip
Source5:        %{dcf_name}-%{dcf_version}.tar.gz
# Source6 is generated through following steps:
# $ git clone https://github.com/aws/aws-sdk-cpp -b %%{version} --recurse-submodules --depth=1
# $ pushd aws-sdk-cpp && git-archive-all.sh --format tar --prefix aws-sdk-cpp-%%{version}/ aws-sdk-cpp-%%{version}.tar && xz aws-sdk-cpp-%%{version}.tar && popd
# where git-archive-all.sh comes from https://github.com/fabacab/git-archive-all.sh
Source6:        %{awssdk_name}-%{awssdk_version}.tar.xz


Source10:       opengauss-bashprofile
Source11:       opengauss.service
Source12:       autostart.sh
Source13:       version.cfg
Source14:       upgrade.sh
Source20:       krb-configure
Source30:       Huawei_Secure_C_Makefile
Source40:       opengauss-rpmlintrc

Patch0:         og-cmake.patch
Patch1:         og-delete-obs.patch
Patch2:         og-openssl3-adptor.patch
Patch3:         og-susebuild.patch
Patch4:         og-dolphin.patch
Patch5:         og-more-arch-support.patch
Patch11:        krb5-backport-Add-a-simple-DER-support-header.patch
Patch12:        krb5-backport-CVE-2024-37370-CVE-2024-37371-Fix-vulnerabilities-in-GSS-message-token-handling.patch
Patch13:        krb5-cve-2022-42898.patch
Patch14:        krb5-CVE-2023-36054.patch
Patch15:        krb5.patch
Patch16:        xgboost-cmake-3.13.patch
Patch17:        dmlc-core-port-to-newer-cmake.patch
Patch18:        DCF_susebuild.patch
Patch19:        DCF-6.0.0-add-more-arch-support.patch

BuildRequires:  libopenssl-3-devel >= 3.1.4-150600.5
BuildRequires:  cmake 
BuildRequires:  gcc
BuildRequires:  gcc-c++
BuildRequires:  python3
BuildRequires:  tar
BuildRequires:  unzip
BuildRequires:  lsb-release
BuildRequires:  fdupes
BuildRequires:  cJSON-devel
BuildRequires:  bison
BuildRequires:  flex
BuildRequires:  autoconf
BuildRequires:  java-1_8_0-openjdk-devel
BuildRequires:  unixODBC-devel
BuildRequires:  jemalloc-devel
BuildRequires:  ncurses-devel
BuildRequires:  zlib-devel
BuildRequires:  minizip-devel
BuildRequires:  libedit-devel
BuildRequires:  libaio-devel
BuildRequires:  libxerces-c-devel
BuildRequires:  libcgroup-devel
BuildRequires:  libcurl-devel
BuildRequires:  libxml2-devel
BuildRequires:  liblz4-devel
BuildRequires:  libzstd-devel
BuildRequires:  libboost_headers-devel
BuildRequires:  libboost_thread-devel
BuildRequires:  libboost_atomic-devel
BuildRequires:  libboost_system-devel
BuildRequires:  libboost_chrono-devel
%ifarch aarch64
BuildRequires: libnuma-devel
%endif

%global __provides_exclude lib.*[.]so.*
%global __requires_exclude lib(com_err|gss|ecpg|pg|pq|aws-cpp|dcf|kadm5|kdb5|k5crypto|krad|krb5|page|verto).*[.]so.*
Requires: openssl-3 >= 3.1.4-150600.5
Requires(pre): lsof
Requires(pre): shadow
Requires(post): systemd
Requires(post): procps
Requires(post): sed
Requires(post): gawk
Requires(post): tar

%description
openGauss is a user-friendly, enterprise-level, and open-source relational
database jointly built with partners. openGauss provides multi-core
architecture-oriented ultimate performance, full-link service, data security,
AI-based optimization, and efficient O&M capabilities.

%prep
%setup -q -c -n %{name}-%{version}
%setup -q -D -T -a 1
%setup -q -D -T -a 2
%setup -q -D -T -a 3
%setup -q -D -T -a 4
%setup -q -D -T -a 5
%setup -q -D -T -a 6

pushd openGauss-server-%{version} 
%patch -P 0 -p1
%patch -P 1 -p1
%patch -P 2 -p1
%patch -P 3 -p1
%patch -P 4 -p1
%patch -P 5 -p1
popd

pushd %{krb5_name}-%{krb5_name}-%{krb5_version}
%patch -P 11 -p1
%patch -P 12 -p1
%patch -P 13 -p1
%patch -P 14 -p1
%patch -P 15 -p1
popd

pushd %{xgboost_name}-%{xgboost_version}
%patch -P 16 -p1
popd

pushd %{dmlc_name}-%{dmlc_version}
%patch -P 17 -p1
popd

pushd %{dcf_name}-%{dcf_version}
%patch -P 18 -p1
%patch -P 19 -p1
popd

%build
dependency_loc=$(pwd)/binarylibs/kernel/dependency


########### build krb5 ###########
pushd %{krb5_name}-%{krb5_name}-%{krb5_version}
krb5_dir=${dependency_loc}/kerberos/comm

cd src
rm -rf configure; cp %{SOURCE20} ./configure
autoheader; chmod +x configure
./configure CFLAGS='-std=gnu11' --prefix=${krb5_dir} --disable-rpath --disable-pkinit --with-system-verto=no

make %{?_smp_mflags}
make install %{?_smp_mflags}

cd ${krb5_dir}
rm -rf lib/pkgconfig share var
popd

########### build xgboost ###########
pushd %{xgboost_name}-%{xgboost_version}
xgboost_dir=${dependency_loc}/xgboost/comm

rm -rf dmlc-core
cp -r ../dmlc-core-%{dmlc_version}/ ./dmlc-core

cmake . -DCMAKE_INSTALL_PREFIX=${xgboost_dir}
make %{?_smp_mflags}
make install
cp -r ${xgboost_dir} ${dependency_loc}/xgboost/llt
popd

########### build securec ###########
pushd %{securec_name}_%{securec_version}
securec_dir=${dependency_loc}/../platform/Huawei_Secure_C/comm
cp %{SOURCE30} ./src/Makefile

make -C src securecstatic securecshare %{?_smp_mflags}

mkdir -p ${securec_dir}
cp -arf include ${securec_dir}
mkdir ${securec_dir}/lib
cp src/libsecurec.a ${securec_dir}/lib/
cp src/libsecurec.a ${securec_dir}/lib/libboundscheck.a
mkdir ${securec_dir}/../Dynamic_Lib
cp src/libsecurec.so ${securec_dir}/../Dynamic_Lib
popd

########### build DCF ###########
pushd %{dcf_name}-%{dcf_version}
dcf_dir=${dependency_loc}/../component/dcf
mkdir library
cp -r ${securec_dir} ./library/huawei_security
mkdir -p library/cJSON/include
cp /usr/include/cjson/cJSON.h library/cJSON/include

cmake -S . -B tmp_build -DCMAKE_INSTALL_PREFIX=${dcf_dir} -DCMAKE_BUILD_TYPE=Release -DENABLE_EXPORT_API=OFF -DTEST=OFF
make -C tmp_build %{?_smp_mflags}

mkdir -p ${dcf_dir}
cp -arf ./output/lib ${dcf_dir}/
cp -arf ./src/interface ${dcf_dir}/include
popd

########### build aws-sdk-cpp ###########
pushd %{awssdk_name}-%{awssdk_version}
awssdk_dir=${dependency_loc}/aws-sdk-cpp/comm
mkdir tmp_build && cd tmp_build

cmake .. -DBUILD_ONLY="s3" \
         -DCMAKE_SKIP_RPATH=TRUE \
         -DCMAKE_BUILD_TYPE=Release \
         -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
         -DCMAKE_INSTALL_PREFIX=${awssdk_dir} \
         -DENABLE_TESTING=OFF
make %{?_smp_mflags}
make install
mv ${awssdk_dir}/lib64 ${awssdk_dir}/lib || true
popd

########### build opengauss ###########
%ifarch riscv64 ppc64le s390x
rm -rf openGauss-server-%{version}/contrib/dolphin
%endif
cd openGauss-server-%{version}

export BUILD_TUPLE=%{_arch}
export DEBUG_TYPE=release
export THIRD_BIN_PATH=${dependency_loc}/../../../binarylibs

%cmake -DENABLE_MULTIPLE_NODES=OFF \
       -DENABLE_THREAD_SAFETY=ON \
       -DENABLE_LLVM_COMPILE=OFF \
       -DENABLE_OBS=OFF \
       -DENABLE_OPENSSL3=ON \
%ifarch riscv64 ppc64le s390x
       -DENABLE_BBOX=OFF \
%endif
%ifarch s390x
       -DWORDS_BIGENDIAN=ON \
%endif
       -DWITH_OPENEULER_OS=ON

%cmake_build

%install

cd openGauss-server-%{version}
opengauss_source_dir=$(pwd)

%cmake_install

# Clean devel files
rm -r %{buildroot}%{_prefix}/include
rm -r %{buildroot}%{_prefix}/lib/pkgconfig
rm -r %{buildroot}%{_prefix}/lib/cmake
rm -r %{buildroot}%{_prefix}/lib/aws*
rm -r %{buildroot}%{_prefix}/lib/s2n
rm -r %{buildroot}%{_prefix}/lib/libsimsearch
rm %{buildroot}%{_prefix}/lib/*.a

mkdir -p %{buildroot}/var/lib/opengauss/data
mkdir -p %{buildroot}%{tmppath}/script

# move to {tmppath}
mv %{buildroot}%{_prefix}/* %{buildroot}%{tmppath}
mkdir -p %{buildroot}%{apppath}

# make package upgrade sql
tar -C ${opengauss_source_dir}/src/include/catalog -zcf upgrade_sql.tar.gz upgrade_sql
sha256sum upgrade_sql.tar.gz | awk '{print $1}' > upgrade_sql.sha256
if [ $? -ne 0 ]; then
    echo "Generate upgrade_sql.sha256 failed."
    exit 1
fi
cp -r upgrade_sql.tar.gz %{buildroot}%{tmppath}
cp -r upgrade_sql.sha256 %{buildroot}%{tmppath}

# opengauss datanode dir.
install -d -m 700 $RPM_BUILD_ROOT%{?_localstatedir}/lib/opengauss/data

# opengauss .bash_profile
install -m 644 %{SOURCE10} $RPM_BUILD_ROOT%{?_localstatedir}/lib/opengauss/.bash_profile
# auto start files
install -m 644 %{SOURCE11} %{buildroot}%{tmppath}/script/opengauss.service
install -m 755 %{SOURCE12} %{buildroot}%{tmppath}/script/autostart.sh

# upgrade script
install -m 644 %{SOURCE13} %{buildroot}%{tmppath}/version.cfg
install -m 755 %{SOURCE14} %{buildroot}%{tmppath}/script/upgrade.sh

# process shell scripts
sed -i '1c #!/bin/bash' %{buildroot}%{tmppath}/bin/gs_dbmind
sed -i '1c #!/bin/bash' %{buildroot}%{tmppath}/bin/gs_loader.sh

# permission
chmod -R 755 %{buildroot}%{tmppath}/bin/*
chmod -R 755 %{buildroot}%{tmppath}/lib/*
chmod -R 644 %{buildroot}%{tmppath}/bin/*.conf

%fdupes %{buildroot}%{tmppath}

%pre
# add opengauss user
/usr/sbin/groupadd -r opengauss || :
/usr/sbin/useradd -M -N -g opengauss -r -d %{datapath} -s /bin/bash -c "openGauss Server" opengauss || :

# for install step 
# 1:install 2:upgrade
if [ $1 -eq 1 ]; then
    echo "Preparing for install"
    portinfo=$(lsof -i:%{port})
    if [ "${portinfo}" != "" ]; then
        echo "Error: The port[%{port}] is occupied. Please use command 'lsof -i:%{port} to check it.'"
    fi

    if [ -d /var/lib/opengauss/data ]; then
        if [ "`ls -A /var/lib/opengauss/data`" != "" ]; then
            echo "Datanode dir(/var/lib/opengauss/data) is not empty."
            echo "Please delete dir and reinstall opengauss."
            exit 1
        fi
        process_id=$(ps -ef | grep /var/lib/opengauss/data | grep -v grep | awk '{print $2}')
        if [ "$process_id" != "" ]; then
            echo "A process of opengauss already exists. Use command (ps -ef | grep /var/lib/opengauss/data) to confirm."
            echo "Please kill the process and reinstall opengauss."
            exit 1
        fi
    fi
    
elif [ $1 -eq 2 ]; then
    echo "Preparing for upgrade"
    old_version=$(rpm -qi opengauss | grep -i version | awk -F':' '{print $2}' | sed 's/^[ \t]*//;s/[ \t]*$//')
    if [ "$(printf '%s\n' "%{version}" "$old_version" | sort -V | head -n1)" == "%{version}" ]; then
        echo "Warning: New version (%{version}) must be greater than the old version ($old_version)."
        exit 0
    fi
    if [[ "${old_version}" == "2.1.0" && %{version} == "6.0.0" ]]; then
        echo "The opengauss do not support upgrade from 2.1.0 to 6.0.0."
        exit 1
    fi
fi

%post
start_opengauss(){
    result=$(su - opengauss -c "source ~/.bash_profile; gs_initdb -D /var/lib/opengauss/data -U opengauss --nodename=single_node")
    if [ $? -ne 0 ]; then
        echo "Init openGauss database failed."
        echo "$result"
        exit 1
    else
        echo "Init openGauss database success."
    fi
}

add_service(){
    cp %{apppath}/script/opengauss.service /usr/lib/systemd/system/
    systemctl daemon-reload
}

remove_service(){
    service_name=/usr/lib/systemd/system/opengauss.service
    if [ -f $service_name ]; then
        systemctl disable opengauss.service
        rm $service_name
    fi
}

create_dir() {
    if [ -d /usr/local/opengauss ]; then
        rm -rf /usr/local/opengauss
    fi
    mkdir -p /usr/local/opengauss
    cp -r /var/lib/opengauss/pkg_%{version}/* /usr/local/opengauss
    chmod -R 755 /usr/local/opengauss/script/*.sh
    chown -R opengauss:opengauss /usr/local/opengauss
}

upgrade_create_dir() {
    rm -rf /var/lib/opengauss/opengauss_upgrade/pkg_%{version}
    rm -rf /var/lib/opengauss/opengauss_upgrade/bak
    rm -rf /var/lib/opengauss/opengauss_upgrade/tmp
    mkdir -p /var/lib/opengauss/opengauss_upgrade/pkg_%{version}
    mkdir -p /var/lib/opengauss/opengauss_upgrade/bak
    mkdir -p /var/lib/opengauss/opengauss_upgrade/tmp
    chown -R opengauss:opengauss /var/lib/opengauss
}

# for install step
# 1:install 2:upgrade
if [ $1 -eq 1 ]; then
    echo "install" > /var/lib/opengauss/recode_install_flag
    create_dir
    start_opengauss
    add_service
elif [ $1 -eq 2 ]; then
    echo "upgrade" > /var/lib/opengauss/recode_install_flag
    echo "start upgrade..."
    upgrade_create_dir
    cmd="source ~/.bash_profile; cd /var/lib/opengauss/pkg_%{version}; sh script/upgrade.sh"
    su - opengauss -c "$cmd"
    if [ $? -ne 0 ]; then
        echo "Upgrade failed."
        echo "Please cat the log information: cat /var/lib/opengauss/opengauss_upgrade/opengauss_upgrade.log"
        echo "failed" > /var/lib/opengauss/upgrade_result
        exit 1
    else
        echo "Upgrade success."
        echo "success" > /var/lib/opengauss/upgrade_result
    fi
    remove_service
    add_service
fi

%preun
remove_service(){
    service_name=/usr/lib/systemd/system/opengauss.service
    if [ -f $service_name ]; then
        systemctl disable opengauss.service
        rm $service_name
    fi
}

# 0: uninstall 1:upgrade
if [ $1 -eq 0 ]; then
    echo "remove opengauss service"
    remove_service
fi

%postun
clear_database(){
    pid=$(ps -ef | grep /var/lib/opengauss/data | grep -v grep | awk '{print $2}')
    if [ "$pid" != "" ]; then
        kill -9 $pid
    fi
    if [ -d /usr/local/opengauss ]; then
        rm -rf /usr/local/opengauss
    fi
    if [ -d /usr/local/opengauss_%{version} ]; then
        rm -rf /usr/local/opengauss_%{version}
    fi
    if [ -f /var/lib/opengauss/recode_install_flag ]; then
        rm -rf /var/lib/opengauss/recode_install_flag
    fi
    if [ -f /var/lib/opengauss/upgrade_result ]; then
        rm -rf /var/lib/opengauss/upgrade_result
    fi
    if [ -d /var/lib/opengauss/pkg_%{version} ]; then
        rm -rf /var/lib/opengauss/pkg_%{version}
    fi
    if [ -d /var/lib/opengauss/opengauss_upgrade ]; then
        rm -rf /var/lib/opengauss/opengauss_upgrade
    fi
    if [ -d /var/lib/opengauss/log ]; then
        rm -rf /var/lib/opengauss/log
    fi
}

# 0: uninstall
if [ $1 -eq 0 ]; then
    echo "clean database"
    clear_database
fi

%posttrans
flag=$(cat /var/lib/opengauss/recode_install_flag)
if [ $flag = "install" ]; then
    echo "opengauss install successfully!"
    echo "Please run: systemctl start opengauss.service"
else
    echo "upgrade posttrans"
    if [ -d "/usr/local/opengauss" ] && [ "$(ls -A /usr/local/opengauss)" ]; then
        rm -rf /usr/local/opengauss
    fi
    mkdir -p /usr/local/opengauss
    upgrade_file=/var/lib/opengauss/upgrade_result
    if [[ -f ${upgrade_file} && $(cat $upgrade_file) = "success" ]]; then
        echo "opengauss upgrade successfully"
        cp -rf /var/lib/opengauss/opengauss_upgrade/pkg_%{version}/* /usr/local/opengauss
    else
        echo "opengauss upgrade failed, rollback in progress"
        cp -rf /var/lib/opengauss/opengauss_upgrade/bak/* /usr/local/opengauss
    fi
    chown -R opengauss:opengauss /usr/local/opengauss
    chmod -R 700 /var/lib/opengauss/data
    systemctl restart opengauss.service
fi


%files
%doc
%defattr (-,opengauss,opengauss)
%{apppath}

%defattr (-,opengauss,opengauss)
%{?_localstatedir}/lib/opengauss


%changelog
