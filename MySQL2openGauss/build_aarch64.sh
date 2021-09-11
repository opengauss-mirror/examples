#!/bin/bash
cd `dirname $0`
BASEDIR=`pwd`

build_error () {
  echo build error !!!
  exit 1
}

# unixODBC
cd lib/unixODBC
tar -xf unixODBC-2.3.9-aarch64.tar.gz
cd $BASEDIR

# mysql
cd mysqlsrc
tar -xf mysql-boost-5.7.27.tar.gz
cd mysql-5.7.27
ln -s $BASEDIR/lib/unixODBC unixODBC
rm -f ./client/CMakeLists.txt
cp -f $BASEDIR/cmake/mysql-5.7.27/client/CMakeLists.txt ./client/
cp -f $BASEDIR/src/* ./client/
cmake . -DCMAKE_INSTALL_PREFIX=$BASEDIR/mysql5727 -DWITH_BOOST=$BASEDIR/mysqlsrc/mysql-5.7.27/boost/boost_1_59_0
make -j2
# ignore error and make again
make
if [ "$?" != "0" ]; then
  build_error
fi
rm -f $BASEDIR/bin/mysqltoopengauss
cp -f $BASEDIR/mysqlsrc/mysql-5.7.27/client/mysqltoopengauss $BASEDIR/bin/
cd $BASEDIR

# libs
cd lib
tar -xf depends-aarch64.tar.gz
cd $BASEDIR

# pack
cd bin
chmod +x *
cd ..
rm -rf output/mysqltoopengauss
mkdir -p output/mysqltoopengauss
cp -rf bin output/mysqltoopengauss
cp -rf conf output/mysqltoopengauss
cp -rf lib output/mysqltoopengauss
rm -rf output/mysqltoopengauss/lib/unixODBC/*.tar.gz
rm -rf output/mysqltoopengauss/lib/*.tar.gz
cd output
chmod +x mysqltoopengauss/bin/*
tar -czf mysqltoopengauss.tar.gz mysqltoopengauss
rm -rf mysqltoopengauss
echo mysqltoopengauss is pack to `pwd`/mysqltoopengauss.tar.gz
