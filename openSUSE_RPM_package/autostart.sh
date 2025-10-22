#!/bin/bash

result=`su - opengauss -c "source ~/.bash_profile; gs_ctl start -D /var/lib/opengauss/data"`
if [ $? -ne 0 ]; then
    echo "Start openGauss database failed."
    echo $result
else
    echo "Start openGauss database success."
fi