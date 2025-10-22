<div style="text-align: center;font-weight: bold;font-size: xx-large;">openGauss 6.0打包脚本—— SUSE 系操作系统</div>

# 概述

## 项目成果

本项目已成功在openSUSE Leap 15.6，openSUSE Tumbleweed，SUSE Linux Enterprise Server 15 SP7三个发行版，以及四种支持架构（x86_64, aarch64, ppc64le, s390x）上完成了openGauss 6.0.0软件包的构建、运行和测试工作。

此外，项目构建仓库（含完整的打包脚本、源码和补丁）已被提交至openSUSE Build Service平台，并顺利通过在线验证。同时本项目已被官方运营的`server:database`类顺利接收通过。具体详情如下：
1.  提交`server:database`申请请求通过：https://build.opensuse.org/requests/1305598
2.  本项目在openSUSE Build Service上的托管地址：https://build.opensuse.org/package/show/server:database/opengauss
3.  openSUSE官方提供构建输出的下载页面：https://software.opensuse.org/download.html?project=server%3Adatabase&package=opengauss

## 项目部署与测试

### 本地编译（可选）

#### openSUSE Leap 15.6 打包编译方法

1. 进行必要的软件源配置（[opensuse | 镜像站使用帮助](https://mirrors.tuna.tsinghua.edu.cn/help/opensuse/)），运行以下命令安装必要软件包：

   ```bash
   $ sudo zypper install git git-lfs rpm-build rpmdevtools cmake gcc gcc-c++ openssl-devel python tar unzip liblz4-devel libzstd-devel libboost_headers-devel cJSON-devel libcgroup-devel libcurl-devel bison flex autoconf java-1_8_0-openjdk-devel libedit-devel libaio-devel libxerces-c-devel libxml2-devel unixODBC-devel jemalloc-devel ncurses-devel libboost_thread-devel libboost_atomic-devel libboost_system-devel libboost_chrono-devel
   ```

2. 运行`rpmdev-setuptree`设置RPM编译环境

3. 下载并部署本项目（注意有git lfs文件）至系统中，将项目中全部文件移动至`~/rpmbuild/SOURCES`文件夹下。

4. 启动编译：

   ```bash
   $ cd ~/rpmbuild/SOURCES
   $ rpmbuild -ba opengauss-server.spec
   ```
   编译结束后的RPM包位于`~/rpmbuild/RPMS/$(uname -m)/opengauss-6.0.0-24.$(uname -m).rpm`

5. 安装编译好的软件包（可在另外的机器上安装，也可在编译机上安装）：

   ```bash
   $ sudo zypper install ~/rpmbuild/RPMS/$(uname -m)/opengauss-6.0.0-24.$(uname -m).rpm
   ```

#### SUSE Linux Enterprise Server 15（SLES）打包编译方法

1. 激活系统，启用相关软件源，命令行中的版本号和架构请根据实际情况替换
```
SUSEConnect --product PackageHub/15.7/x86_64
SUSEConnect --product sle-module-legacy/15.7/x86_64
SUSEConnect --product sle-module-desktop-applications/15.7/x86_64
SUSEConnect --product sle-module-development-tools/15.7/x86_64
```

2. 安装软件包
```
sudo zypper install git-core git-lfs rpm-build rpmdevtools cmake gcc gcc-c++ openssl-devel python tar unzip liblz4-devel libzstd-devel libboost_headers-devel cJSON-devel libcgroup-devel libcurl-devel bison flex autoconf java-1_8_0-openjdk-devel libedit-devel libaio-devel libxerces-c-devel libxml2-devel unixODBC-devel jemalloc-devel ncurses-devel libboost_thread-devel libboost_atomic-devel libboost_system-devel libboost_chrono-devel
```

3. 后续步骤与openSUSE无异

### 安装

搭建openSUSE Leap 15.6 x86_64虚拟机，目前安装openGauss数据库软件包有两种方式：
1. 在线安装：按照 [官方指引](https://software.opensuse.org/download.html?project=server%3Adatabase&package=opengauss)，输入合适的命令，即可通过`zypper`在线安装。
2. 离线安装：若因网络原因无法在线安装，可先获取到离线安装包，传输至虚拟机中后，通过`zypper in opengauss-6.0.0.x86_64.rpm`进行安装，由于签名密钥未被信任，安装过程需输入`i`，继续安装。

安装完成后输入`systemctl status opengauss`即可启动openGauss数据库

### 测试

在另一台机器上部署`benchmarksql-5.0`与opengauss专用的JDBC驱动，进行简单压力测试。其中简单的benchmarksql任务配置如下：
```
conn=jdbc:postgresql://192.168.33.130:7654/tpcctest
user=bot
password=Gaussdba@Mpp

warehouses=5
loadWorkers=10

terminals=20
runTxnsPerTerminal=0
runMins=5
limitTxnsPerMin=0
```
在服务端创建好所需的用户、数据库，并添加相应的远程访问规则。然后在测试机上运行测试，测试过程中在openGauss服务部署的虚拟机上可以观察到数据库进程的正常运行。最终测试结果如下（2虚拟核心，5GB RAM）：

```
Term-00, Running Average tpmTOTAL: 28250.90    Current tpmTOTAL: 935592    Memory Usage: 44MB / 129MB
18:32:30,078 [Thread-5] INFO   jTPCC : Term-00,
18:32:30,078 [Thread-5] INFO   jTPCC : Term-00,
18:32:30,078 [Thread-5] INFO   jTPCC : Term-00, Measured tpmC (NewOrders) = 12695.42
18:32:30,078 [Thread-5] INFO   jTPCC : Term-00, Measured tpmTOTAL = 28244.63
```

综合来看，本项目编译打包的openGauss 6.0.0顺利通过5分钟压力测试，具备实际可用性。

# 项目细节


在适配openSUSE的过程中，遇到许多兼容性和规范性问题。为解决这些问题，本项目在基于openEuler原版打包编译脚本的基础上，进行了大量修改，本章主要阐述不断优化打包过程的具体技术细节。

## RPM SPEC攥写与rpmlint检查

openSUSE为了确保软件打包的规范性，制定了一系列严格的规则，通过rpmlint工具实施检查。根据RPM SPEC规范，提交openSUSE社区时的审核意见，本项目在SPEC攥写规范上进行了针对性适配，主要包括：
+ BuildRequires，Requires标签单行排列，一行只描述一个依赖包
+ 大幅削减SPEC指出的Requires数量，减少不必要的运行时依赖，主要通过rpm自带的依赖分析推理得到Requires数据。
+ `__requires_exclude`变量仅排除掉内部自带动态库，而不是所有库。
+ 在编译依赖库时，去除了不常用的编译、链接选项，现在使用编译器默认选项进行。
+ 获取编译打包的目标架构时，应通过内置宏`%{_arch}`获取。
+ 使用`%cmake, %cmake_build, %cmake_install`内置宏进行openGauss源码编译，而不是直接调用`cmake, make, make install`命令。
+ 在单独的`%install`环节处理安装，`%build`环节仅处理依赖库及openGauss的编译工作。
+ 根据rpmlint检查结果，编写rpmlint过滤规则集：
```py
addFilter(r'(non-standard-gid|non-standard-uid) .* opengauss')
addFilter(r'(postin-without-ldconfig|postun-without-ldconfig) /var/lib/opengauss/pkg_6.0.0/lib/.*')
addFilter(r'shared-lib-calls-exit /var/lib/opengauss/pkg_6.0.0/lib/.*')
addFilter(r'binary-or-shlib-calls-gethostbyname')
addFilter(r'non-standard-group')
addFilter(r'devel-file-in-non-devel-package.* /var/lib/opengauss/pkg_6.0.0/lib/.*.so')
```

## 引入外部编译依赖

经过实际调查分析，在openSUSE上编译openGauss，系统软件源无法提供的依赖模块有：xgboost, Huawei Secure C, DCF, aws-sdk-cpp, Kerberos 5，其中Kerberos 5库为openGauss修改定制版。

Kerberos 5的编译过程主要改动点在于，为了支持高版本GCC，单独加上了`-std=gnu11`选项，以避免编译出错，同时删除不需要安装的目录。具体编译过程如下：
```
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
```

xgboost库的编译过程如下：
```
pushd %{xgboost_name}-%{xgboost_version}
xgboost_dir=${dependency_loc}/xgboost/comm

rm -rf dmlc-core
cp -r ../dmlc-core-%{dmlc_version}/ ./dmlc-core

cmake . -DCMAKE_INSTALL_PREFIX=${xgboost_dir}
make %{?_smp_mflags}
make install
cp -r ${xgboost_dir} ${dependency_loc}/xgboost/llt
popd
```

Huawei Securec的编译使用了[openGauss-third_party](https://gitcode.com/opengauss/openGauss-third_party/tree/6.0.0/platform/Huawei_Secure_C)给出的Makefile，为了符合规范，本项目在该Makefile的链接动态库处加入了额外选项：`-Wl,-soname=libsecurec.so`，确保输出的动态库中包含`soname`字段。该组件编译过程如下：
```
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
```

DCF为openGaus自研组件，主要参考[openEular DCF 编译过程](https://gitee.com/src-openeuler/opengauss-dcf)实现。在openSUSE编译时遇到编译错误，需合入以下补丁方可解决：
```patch
diff --git a/src/election/elc_msg_proc.c b/src/election/elc_msg_proc.c
index 9aa9552..7c3df3d 100644
--- a/src/election/elc_msg_proc.c
+++ b/src/election/elc_msg_proc.c
@@ -179,7 +179,7 @@ bool32 elc_need_demote_follow(uint32 stream_id, timespec_t now, uint32 elc_timeo
     uint32 voter_num = 0;
     dcf_node_role_t node_list[CM_MAX_NODE_COUNT];
     uint32 node_count;
-    uint32 local_node_id = md_get_cur_node(stream_id);
+    uint32 local_node_id = md_get_cur_node();
     dcf_role_t default_role;
     if (elc_stream_get_work_mode(stream_id) != WM_NORMAL) {
         return CM_FALSE;
diff --git a/src/common/cm_concurrency/cm_thread.c b/src/common/cm_concurrency/cm_thread.c
index 723b1de..92e0895 100644
--- a/src/common/cm_concurrency/cm_thread.c
+++ b/src/common/cm_concurrency/cm_thread.c
@@ -296,7 +296,7 @@ uint32 cm_get_current_thread_id(void)
 #define __SYS_GET_SPID 186
 #elif (defined __aarch64__)
 #define __SYS_GET_SPID 178
-#elif (defined __loongarch__)
+#else
 #include<sys/syscall.h>
 #define __SYS_GET_SPID SYS_gettid
 #endif
```
该组件具体编译过程如下：
```
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
```
aws-sdk-cpp库主要[openEular aws-sdk-cpp 编译过程](https://gitee.com/src-openeuler/aws-sdk-cpp)实现，具体过程如下：
```
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
```

## 优化openGauss编译过程

在openGaus本体编译打包过程中，为解决openSUSE上特有的编译、运行问题，本项目对原版openGaus 6.0.0代码进行了针对性修改，确保能够在openSUSE上编译、运行通过。

### 修复CMake配置中的头文件冲突

在openSUSE Tumbleweed系统中，由于使用了高版本的GCC编译器，造成`editline/history.h`, `editline/readline.h`头文件与`readline.h`, `readline/history.h`, `readline/readline.h`头文件冲突，导致CMake配置过程中的重要函数`CHECK_FOR_MPPDB`无法正常运行，进而影响编译。应当将此处的`editline/history.h editline/readline.h`头文件移除，补丁（og-susebuild.path）节选如下：
```patch
diff --git a/cmake/src/build_function.cmake b/cmake/src/build_function.cmake
index fc114c9f2..f22208259 100755
--- a/cmake/src/build_function.cmake
+++ b/cmake/src/build_function.cmake
@@ -395,7 +395,7 @@ function(check_headers_func_c check_include_files check_functions check_decl che
 endfunction(check_headers_func_c)
 
 function(CHECK_FOR_MPPDB)
-    set(CHECK_INC_FILES crtdefs.h crypt.h dld.h editline/history.h editline/readline.h fp_class.h …… readline.h readline/history.h readline/readline.h …… errno.h)
+    set(CHECK_INC_FILES crtdefs.h crypt.h dld.h fp_class.h …… readline.h readline/history.h readline/readline.h …… errno.h)
     set(STDC_HEADERS 1 PARENT_SCOPE)
 
     set(CHECK_FUNCTIONS append_history cbrt ……)
```

### dolphin组件编译
在原版openGaus编译描述文件`CMakeLists.txt`中，存在如下行：
```
install(CODE "execute_process(COMMAND bash cmake.sh WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/contrib/dolphin)")
```
这意味着，dolphin组件是在install阶段通过执行一个脚本进行编译的，由于install过程在单独的环节中执行，且该环节下不应出现编译过程，因而这种策略并不合适。

合理做法是将位于`contrib`目录下的dolphin组件通过`add_subdirectory`加入CMakeLists，使之与项目顶层一同配置和编译，具体如下：
```patch
diff --git a/contrib/CMakeLists.txt b/contrib/CMakeLists.txt
index 7777f062..8b827394 100644
--- a/contrib/CMakeLists.txt
+++ b/contrib/CMakeLists.txt
@@ -58,3 +65,6 @@ if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/datavec)
     add_subdirectory(datavec)
 endif()
 add_subdirectory(gms_profiler)
+if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/dolphin)
+    add_subdirectory(dolphin)
+endif()
\ No newline at end of file
```
同时修改了`contrib/dolphin/CMakeLists.txt`的目录变量，将`CMAKE_SOURCE_DIR`全部修改为`CMAKE_CURRENT_SOURCE_DIR`，确保相关变量能够正确指向openGauss源代码根目录，具体如下：
```patch
diff --git a/contrib/dolphin/CMakeLists.txt b/contrib/dolphin/CMakeLists.txt
index 4a606c78..b9a87a76 100755
--- a/contrib/dolphin/CMakeLists.txt
+++ b/contrib/dolphin/CMakeLists.txt
@@ -38,11 +38,11 @@ if("${ENABLE_PRIVATEGAUSS}" STREQUAL "ON")
 endif()
 
 #FIXME: make it an argument
-set(openGauss "../..")
-set(ENV{openGauss} "../..")
+set(openGauss ".")
+set(ENV{openGauss} ".")
 
 #------------------------------------------------------------------------------------
-set(PROJECT_TRUNK_DIR ${CMAKE_SOURCE_DIR}/../..)
+set(PROJECT_TRUNK_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../..)
 set(PROJECT_OPENGS_DIR ${PROJECT_TRUNK_DIR} CACHE INTERNAL "")
 
 # PROJECT_TRUNK_DIR: ./
@@ -75,7 +75,7 @@ endif()
 
 message(WARNING "dolphin cmake begin")
 
-set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/../../cmake/src)
+set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../../cmake/src)
 
 include(build_function)
 include(set_thirdparty_path)
```
针对dolphin编译修改的更多内容汇总在构建仓下的`og-dolphin.patch`补丁中。全部修改到位后，可实现dolphin组件在`%build`阶段与openGaus一起配置和编译。

### 多架构支持

对于非`x86_64`和`aarch64`架构而言，需在原版openGaus代码中修改部分内容，才能确保在openSUSE支持的`ppc64le, s390x`架构上编译运行通过。该部分所有修改汇总于补丁`og-more-arch-support.patch`中。

特殊指令兼容性问题。在CRC32计算过程中使用了SSE4.2指令集，需在CMakeFiles中移除使用该指令集的源代码文件，具体补丁节选如下：
  ```patch
  diff --git a/src/common/port/CMakeLists.txt b/src/common/port/CMakeLists.txt
  index b9a6a28..e511eb0 100755
  --- a/src/common/port/CMakeLists.txt
  +++ b/src/common/port/CMakeLists.txt
  @@ -90,10 +90,31 @@ if("${BUILD_TUPLE}" STREQUAL "aarch64")
      set(TGT_crc32_arm_parallel_SRC ${CMAKE_CURRENT_SOURCE_DIR}/crc32_arm_parallel.S)
  endif()

  +if("${BUILD_TUPLE}" STREQUAL "ppc64le")
  +    list(REMOVE_ITEM TGT_port_SRC ${CMAKE_CURRENT_SOURCE_DIR}/pg_crc32c_choose.cpp ${CMAKE_CURRENT_SOURCE_DIR}/pg_crc32c_sse42.cpp)
  +endif()
  +
  +if("${BUILD_TUPLE}" STREQUAL "s390x")
  +    list(REMOVE_ITEM TGT_port_SRC ${CMAKE_CURRENT_SOURCE_DIR}/pg_crc32c_choose.cpp ${CMAKE_CURRENT_SOURCE_DIR}/pg_crc32c_sse42.cpp)
  +endif()
  SET(TGT_pgport_INC 
      ${PROJECT_SRC_DIR}/common/backend
      ${PROJECT_SRC_DIR}/common/port
  ```
大小端字节序适配问题。由于s390x平台为大端字节序（其余架构都为小端序），而openGaus在开发过程中未针对大端序进行充分考量，导致服务程序`gaussdb`在运行中会因为锁（LWLock）而卡死。在SPEC中，当目标架构为s390x时，会向CMake命令传入`-DWORDS_BIGENDIAN=ON`选项，强制指定当前为大端序运行平台，随后还需修改以下操作锁（LWLock，BufferDesc）的代码，具体补丁节选如下：
```patch
diff --git a/src/gausskernel/storage/lmgr/lwlock.cpp b/src/gausskernel/storage/lmgr/lwlock.cpp
index d84b4334..7b547c4d 100644
--- a/src/gausskernel/storage/lmgr/lwlock.cpp
+++ b/src/gausskernel/storage/lmgr/lwlock.cpp
@@ -893,7 +893,13 @@ static bool LWLockAttemptLock(LWLock *lock, LWLockMode mode)
             if ((desired_state & (LW_VAL_EXCLUSIVE)) != 0) {
                 return true;
             }
-        } while (!pg_atomic_compare_exchange_u8((((volatile uint8*)&lock->state) + maskId), ((uint8*)&old_state) + maskId, (desired_state >> (8 * maskId))));
+        } while (!pg_atomic_compare_exchange_u8(
+#ifndef WORDS_BIGENDIAN
+            (((volatile uint8*)&lock->state) + maskId), ((uint8*)&old_state) + maskId,
+#else
+            (((volatile uint8*)&lock->state) + sizeof(lock->state) - maskId - 1), ((uint8*)&old_state) + sizeof(lock->state) - maskId - 1,
+#endif
+        (desired_state >> (8 * maskId))));
     } else if (mode == LW_EXCLUSIVE) {
         old_state = pg_atomic_read_u64(&lock->state);
         do {
diff --git a/src/gausskernel/storage/buffer/bufmgr.cpp b/src/gausskernel/storage/buffer/bufmgr.cpp
index c6a4c2c2..3b71a381 100644
--- a/src/gausskernel/storage/buffer/bufmgr.cpp
+++ b/src/gausskernel/storage/buffer/bufmgr.cpp
@@ -1962,7 +1962,13 @@ Buffer ReadBuffer_common_for_localbuf(RelFileNode rnode, char relpersistence, Fo
 
         Assert(buf_state & BM_VALID);
         buf_state &= ~BM_VALID;
-        pg_atomic_write_u32(((volatile uint32 *)&bufHdr->state) + 1, buf_state >> 32);
+        pg_atomic_write_u32(
+#ifndef WORDS_BIGENDIAN
+            ((volatile uint32 *)&bufHdr->state) + 1,
+#else
+            ((volatile uint32 *)&bufHdr->state),
+#endif
+        buf_state >> 32);
     }
 
     /*
```
该问题的根源是使用8bit的读写操作去尝试读/写64bit（或32bit）长度内存数据的低8位时，由于大小端字节序的低8位数据的内存地址计算方式不同，进而产生意料之外的内存读写。

全部应用以上修改后，openGaus可以在`ppc64le, s390x`架构上顺利运行。

## 打包安装脚本

根据在openSUSE Build Service上给出的rpmlint检查结果，进一步优化SPEC文件中的`%install`环节，具体如下：
+ 优化文件权限分配，仅对`bin, lib`文件夹下的程序和库分配可执行权限，其余文件不再赋予可执行权限。
+ 优化重复文件，主要通过`%fdupes`宏将重复文件互相链接，降低空间占用。
+ 删除无用的静态库和头文件，降低空间占用。
+ 去除以下调用separate_debug_symbol.sh分离符号的过程，该过程由rpm在编译、安装后可自动完成。
  ```sh
  cd ${opengauss_source_dir}/build/script
  chmod +x ./separate_debug_information.sh
  sed -i '/"$BIN_DIR\/gaussdb\.map"/d' ./separate_debug_information.sh
  ./separate_debug_information.sh
  ```
+ 去除以下将编译产物整体压缩的过程：
  ```sh
  cd ${opengauss_source_dir}/mppdb_temp_install
  tar -zcf ${kernel_package_name}.tar.bz2 *
  sha256sum ${kernel_package_name}.tar.bz2 | awk '{print $1}' > ${kernel_package_name}.sha256
  ```
  由于openSUSE Build Service平台会对`%post`脚本进行重复执行，还需修改升级脚本，确保同版本升级时不会报错导致异常退出。

# 总结与展望

本项目聚焦于将openGauss 6.0.0集成至openSUSE生态系统，旨在提升openGauss在SUSE系发行版中的可访问性与部署效率。

在项目周期内，已成功在openSUSE Leap 15.6、openSUSE Tumbleweed及SUSE Linux Enterprise Server 15 SP7三大主流SUSE系发行版上完成openGauss 6.0.0的软件包构建、安装、运行与功能验证。通过对软件源代码的分析与调试，实现了对x86_64、aarch64、ppc64le和s390x四种关键架构的全面支持，确保了openGauss在不同硬件平台上的兼容性与可用性。

项目构建仓库（含完整的打包脚本、源码和补丁）已成功提交至openSUSE Build Service（OBS）平台，并通过了OBS的在线构建与验证流程（如图1所示），确保了构建过程的自动化与可复现性。且项目成果已通过openSUSE官方server:database项目组的审核与接纳（如图2所示），这将为openGauss在SUSE生态系统下的维护与推广奠定了坚实基础。
