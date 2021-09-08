#!/bin/bash
cd `dirname $0`
cd ..
BASEDIR=`pwd`

# set env
export MYSQL_TO_OPENGAUSS_HOME=$BASEDIR
export LD_LIBRARY_PATH=$BASEDIR/lib/unixODBC/unixODBC-2.3.9/lib:$BASEDIR/unixODBC-2.3.9/lib:$BASEDIR/lib:$LD_LIBRARY_PATH
export ODBCSYSINI=$BASEDIR/conf
export ODBCINI=$BASEDIR/conf/odbc.ini
export CREATE_TABLE_REPLACE_KV=$BASEDIR/conf/createtablerkv.prop
source $BASEDIR/conf/mysql2opengauss.prop
source $BASEDIR/conf/metrics.prop

# odbc.ini
sed "s:OPENGAUSS_SERVERNAME:$opengauss_host:g" $BASEDIR/conf/odbc.ini.template > $BASEDIR/conf/odbc.ini
sed -i "s:OPENGAUSS_PORT:$opengauss_port:g" $BASEDIR/conf/odbc.ini
sed -i "s:OPENGAUSS_USERNAME:$opengauss_username:g" $BASEDIR/conf/odbc.ini
sed -i "s:OPENGAUSS_PASSWORD:$opengauss_password:g" $BASEDIR/conf/odbc.ini
sed -i "s:OPENGAUSS_DATABASE:$opengauss_database:g" $BASEDIR/conf/odbc.ini

sed -i "s:METRICS_SERVERNAME:$metrics_host:g" $BASEDIR/conf/odbc.ini
sed -i "s:METRICS_PORT:$metrics_port:g" $BASEDIR/conf/odbc.ini
sed -i "s:METRICS_USERNAME:$metrics_username:g" $BASEDIR/conf/odbc.ini
sed -i "s:METRICS_PASSWORD:$metrics_password:g" $BASEDIR/conf/odbc.ini
sed -i "s:METRICS_DATABASE:$metrics_database:g" $BASEDIR/conf/odbc.ini

export metrics_dbtype

# odbcinst.ini
sed "s:BASEDIR:$BASEDIR:g" $BASEDIR/conf/odbcinst.ini.template > $BASEDIR/conf/odbcinst.ini

# run
cd bin
./mysqltoopengauss

if [ "$?" == "0" ];
then
  echo success.
else
  echo failed to migrate mysql data!!!
fi

