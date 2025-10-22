#!/bin/bash
err_upgrade_pre=201
err_upgrade_bin=202
err_upgrade_post=203
err_rollback_pre=204
err_rollback_bin=205
err_rollback_post=206
err_check_init=207
err_parameter=208
err_upgrade_commit=209
err_version_same=210
err_no_need_commit=211
err_inner_sys=212
err_dn_role_null=213

version=6.0.0
GAUSS_BASE_PATH="/usr/local/opengauss"
GAUSS_UPGRADE_BASE_PATH="/var/lib/opengauss/opengauss_upgrade/pkg_${version}"
GAUSS_BACKUP_BASE_PATH="/var/lib/opengauss/opengauss_upgrade/bak"
GAUSS_TMP_PATH="/var/lib/opengauss/opengauss_upgrade/tmp"
GAUSS_LOG_FILE="/var/lib/opengauss/opengauss_upgrade/opengauss_upgrade.log"
new_opengauss_dir=/var/lib/opengauss/pkg_${version}
GAUSS_LISTEN_PORT=7654

function create_dir() {
    rm -rf ${GAUSS_BACKUP_BASE_PATH}
    rm -rf ${GAUSS_TMP_PATH}
    rm -rf ${GAUSS_UPGRADE_BASE_PATH}
    mkdir -p ${GAUSS_UPGRADE_BASE_PATH}
    mkdir -p ${GAUSS_BACKUP_BASE_PATH}
    mkdir -p ${GAUSS_TMP_PATH}
    # touch ${GAUSS_LOG_FILE}
}

# create_dir

function cp_new_all_pkg_to_tmp_dir() {
    cp -rf ${new_opengauss_dir}/* ${GAUSS_UPGRADE_BASE_PATH}
}

function debug() {
    local current_time=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$current_time]" "$1" >>"${GAUSS_LOG_FILE}"
}

function log() {
    local current_time=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$current_time]" "$1" >>"${GAUSS_LOG_FILE}"
    echo "$1"
}

function die() {
    local current_time=$(date +"%Y-%m-%d %H:%M:%S")
    if [[ -f "${GAUSS_LOG_FILE}" ]]; then
        echo "[$current_time]" "$1" >>"${GAUSS_LOG_FILE}"
    fi
    echo -e "\033[31mError: $1\033[0m"
    exit $2
}

function check_config_path() {
    local temp_value="$1"
    if [[ "$temp_value" == *[\(\)\{\}\[\]\<\>\"\'\`\\\ \*\!\|\;\&\$\~\?]* ]]; then
        die "$temp_value may contain illegal characters" ${err_check_init}
    fi
    if echo "$temp_value" | grep -Eq "^/{1,}$"; then
        die "path cannot be / " ${err_check_init}
    fi
}

function check_env() {
    if [[ "$GAUSSHOME" == "" ]]; then
        die "GAUSSHOME cannot be null!" ${err_check_init}
    fi
    if [[ "$GAUSSDATA" == "" ]] && [[ "$PGDATA" == "" ]]; then
        die "GAUSSDATA or PGDATA cannot be all null!" ${err_check_init}
    fi
    if [[ "$PGDATA" == "" ]]; then
        PGDATA=${GAUSSDATA}
    fi
    if [[ "$GAUSSDATA" == "" ]]; then
        GAUSSDATA=${PGDATA}
    fi
    check_config_path "$GAUSSHOME"
    check_config_path "$GAUSSDATA"
    check_config_path "$PGDATA"
    # ensure GAUSSDATA not contain GAUSSHOME
    if echo "$GAUSSDATA" | grep -wq "^$GAUSSHOME"; then
        die "GAUSSDATA cannot be in GAUSSHOME!" ${err_check_init}
    fi
    log "Current env value: GAUSSHOME is $GAUSSHOME, PGDATA is $PGDATA."
}

function check_upgrade_config() {
    local tempfile="$GAUSS_TMP_PATH"/".temp_check_guc_value"
    # guc output from opengauss or gauss is diff
    if gs_guc check -D ${PGDATA} -c "$1" >"$tempfile" 2>&1; then
        tempvalue=$(cat "$tempfile" | tail -2 | head -1 | sed 's/\[[^][]*\]//g' | sed 's/[[:space:]]//g' | awk -F= '{print $2}')
        if ! rm -f ${tempfile}; then
            log "rm -f $tempfile failed"
            return 1
        fi
        if [[ "$tempvalue" == "$2" ]]; then
            debug "guc check $1=$2 successfully"
            return 0
        elif [[ "$1" == "sync_config_strategy" && "$tempvalue" == "NULL" ]]; then
            debug "guc check $1=$2 successfully"
            return 0
        else
            return 1
        fi
    else
        if ! rm -f ${tempfile}; then
            log "rm -f $tempfile failed"
            return 1
        fi
        return 1
    fi
}

function reload_upgrade_config() {
    if check_upgrade_config "$1" "$2"; then
        return 0
    fi
    # only primary need to reload upgrade_mode, standby wait sync from primary
    query_dn_role
    if [[ X"$dn_role" == X"standby" || X"$dn_role" == X"cascade_standby" ]]; then
        return 0
    fi
    # ensure value of sync_config_strategy is all_node or default
    if ! check_upgrade_config "sync_config_strategy" "all_node"; then
        return 1
    fi

    local current_time=$(date +"%Y-%m-%d %H:%M:%S")
    echo -n \[${current_time}\] "  " >>"${GAUSS_LOG_FILE}"
    if [[ -f ${PGDATA}/postmaster.pid ]]; then
        gs_guc_action="reload"
    else
        gs_guc_action="set"
    fi
    for i in $(seq 1 3); do
        if gs_guc $gs_guc_action -D ${PGDATA} -c "$1=$2" >>"${GAUSS_LOG_FILE}" 2>&1; then
            return 0
        fi
        sleep 2
    done
    return 1
}

function check_version() {
    version=$(gaussdb -V)
    if [[ $version =~ "V500R002C00" || $version =~ "2.1.0" ]]; then
        echo "2.1.0" >${GAUSSHOME}/version.cfg
        echo "92.421" >>${GAUSSHOME}/version.cfg
        echo "1f6832d" >>${GAUSSHOME}/version.cfg
        old_version=1f6832d
        old_cfg=$(sed -n 2p "$GAUSSHOME/version.cfg" | sed 's/\.//g')
    else
        if [[ ! -f "${GAUSSHOME}/version.cfg" ]]; then
            die "Cannot find current version.cfg!" ${err_upgrade_pre}
        else
            old_version=$(sed -n 3p "$GAUSSHOME"/version.cfg)
            old_cfg=$(sed -n 2p "$GAUSSHOME/version.cfg" | sed 's/\.//g')
        fi
    fi
    if [[ -f "$GAUSS_UPGRADE_BASE_PATH"/version.cfg ]]; then
        new_version_cfg_path="${GAUSS_UPGRADE_BASE_PATH}/version.cfg"
    else
        die "Cannot find new version.cfg!" ${err_upgrade_pre}
    fi

    new_version=$(sed -n 3p "$new_version_cfg_path")
    new_cfg=$(sed -n 2p "$new_version_cfg_path" | sed 's/\.//g')

    if [[ X"$old_version" == X || X"$old_cfg" == X || X"$new_version" == X || X"$new_cfg" == X ]]; then
        die "Maybe version.cfg is not normal" ${err_upgrade_pre}
    fi
    if ! echo "$old_cfg" | grep -Ewq "[0-9]{3,6}"; then
        die "Maybe version.cfg is not normal" ${err_upgrade_pre}
    fi
    if ! echo "$new_cfg" | grep -Ewq "[0-9]{3,6}"; then
        die "Maybe version.cfg is not normal" ${err_upgrade_pre}
    fi

    if [[ ${new_cfg} -lt ${old_cfg} ]]; then
        die "Current version is newer!" ${err_upgrade_pre}
    fi

    log "Old version commitId is $old_version, version info is $old_cfg"
    log "New version commitId is $new_version, version info is $new_cfg"
}

function check_disk() {
    avail_disk=$(df -BM "$GAUSS_UPGRADE_BASE_PATH" | tail -n 1 | awk '{print $4}')
    avail_disk=${avail_disk:0:-1}
    if [[ X"$min_disk" == "X" ]]; then
        min_disk=2048
    fi
    if [[ ${avail_disk} -lt ${min_disk} ]]; then
        die "avail disk must be >= ${min_disk}MB, check with cmd: df -BM $GAUSS_UPGRADE_BASE_PATH!" ${err_check_init}
    fi
    log "Check available disk space successfully."
}

function start_dbnode() {
    start_cmd="gs_ctl start -D ${PGDATA} "
    log "start gaussdb by cmd: $start_cmd"
    ${start_cmd} >>"${GAUSS_LOG_FILE}" 2>&1
    if [ $? -ne 0 ]; then
        die "failed to $start_cmd"
    fi
}

function rollback_post() {
    if ! check_db_process; then
        die "Gaussdb is not running" ${err_rollback_post}
    fi
    if ! reload_upgrade_config upgrade_mode 2; then
        die "set upgrade_mode to 2 failed" ${err_upgrade_post}
    fi
    if exec_sql "$GAUSS_UPGRADE_BASE_PATH"/temp_sql/temp_rollback-post_maindb.sql maindb && exec_sql "$GAUSS_UPGRADE_BASE_PATH"/temp_sql/temp_rollback-post_otherdb.sql otherdb; then
        debug "rollback post sql successfully"
    else
        die "rollback post sql failed" ${err_rollback_post}
    fi
}

function prepare_sql() {
    #$1: upgrade,upgrade-post,rollback,rollback-post
    #$2: maindb,otherdb
    temp_old=${old_cfg}
    temp_new=${new_cfg}
    local action="$1"
    local dbname="$2"
    local tempfile="$GAUSS_TMP_PATH"/temp_sql/"temp_"${action}_${dbname}.sql
    temp_file_num=0
    if echo "START TRANSACTION;set IsInplaceUpgrade = on;" >"$tempfile" && chmod 600 "$tempfile"; then
        debug "Begin to generate $tempfile"
    else
        die "Write $tempfile failed" ${err_upgrade_pre}
    fi
    if ! echo "SET search_path = 'pg_catalog';SET local client_min_messages = NOTICE;SET local log_min_messages = NOTICE;" >>"$tempfile"; then
        die "Write $tempfile failed" ${err_upgrade_pre}
    fi
    if ! echo "SET statement_timeout = 3600000;" >>"$tempfile"; then
        die "Write $tempfile failed" ${err_upgrade_pre}
    fi
    if [[ "$action" == "upgrade" || "$action" == "upgrade-post" ]]; then
        while [[ ${temp_old} -lt ${temp_new} ]]; do
            ((temp_old = $temp_old + 1))
            local upgrade_sql_file="upgrade_sql/upgrade_catalog_${dbname}/${action}_catalog_${dbname}_${temp_old:0:2}_${temp_old:2}.sql"
            if [[ -f "$upgrade_sql_file" ]]; then
                if ! cat "$upgrade_sql_file" >>"$tempfile"; then
                    die "Write $tempfile failed" ${err_upgrade_pre}
                fi
                debug "$upgrade_sql_file >> $tempfile"
                ((temp_file_num = temp_file_num + 1))
            fi
        done
    fi
    if [[ "$1" == "rollback" || "$1" == "rollback-post" ]]; then
        while [[ ${temp_new} -gt ${temp_old} ]]; do
            local upgrade_sql_file="upgrade_sql/rollback_catalog_${dbname}/${action}_catalog_${dbname}_${temp_new:0:2}_${temp_new:2}.sql"
            if [[ -f "$upgrade_sql_file" ]]; then
                if ! cat "$upgrade_sql_file" >>"$tempfile"; then
                    die "Write $tempfile failed" ${err_upgrade_pre}
                fi
                debug "$upgrade_sql_file >>$tempfile"
                ((temp_file_num = temp_file_num + 1))
            fi
            ((temp_new = $temp_new - 1))
        done
    fi
    if ! echo "COMMIT;" >>"$tempfile"; then
        die "Write $tempfile failed" ${err_upgrade_pre}
    fi
    #file not meet requirements
    if [[ ${temp_file_num} -eq 0 ]]; then
        debug "No sql file for ${action} ${dbname}!"
        rm -f "$tempfile"
    else
        debug "get ${temp_file_num} files for ${action} ${dbname}!"
    fi
}

function prepare_sql_all() {
    local dir_temp_sql="$GAUSS_TMP_PATH"/temp_sql
    local sql_tar_file="$GAUSS_UPGRADE_BASE_PATH"/upgrade_sql.tar.gz
    local sql_tar_sha="$GAUSS_UPGRADE_BASE_PATH"/upgrade_sql.sha256

    if [[ ! -f "${sql_tar_file}" ]] || [[ ! -f "${sql_tar_sha}" ]]; then
        die "${sql_tar_file} or ${sql_tar_sha} not exit!" ${err_upgrade_pre}
    else
        local sha_expect=$(cat ${sql_tar_sha})
        local sha_current=$(sha256sum ${sql_tar_file} | awk '{print $1}')
        if [[ "$sha_expect" != "$sha_current" ]]; then
            die "The sha256 value of $sql_tar_file does not match $sql_tar_sha!" ${err_upgrade_pre}
        fi
        if [[ -d "$dir_temp_sql" ]]; then
            rm -rf "$dir_temp_sql"
        fi
        if mkdir -p -m 700 "$dir_temp_sql" && tar -zxf "$sql_tar_file" -C "$dir_temp_sql"; then
            log "decompress upgrade_sql.tar.gz successfully."
        else
            die "decompress upgrade_sql.tar.gz failed" ${err_upgrade_pre}
        fi
    fi
    #total 8
    cd "$dir_temp_sql"
    for action in upgrade upgrade-post rollback rollback-post; do
        for db_base in maindb otherdb; do
            prepare_sql ${action} ${db_base}
        done
    done
}

function pre_exec_sql() {
    if exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_upgrade_maindb.sql maindb && exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_upgrade_otherdb.sql otherdb; then
        debug "exec pre sql successfully"
        return 0
    else
        log "exec pre sql failed"
        return 1
    fi
}

function post_exec_sql() {
    if exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_upgrade-post_maindb.sql maindb && exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_upgrade-post_otherdb.sql otherdb; then
        debug "upgrade post sql successfully"
    else
        die "upgrade post sql failed" ${err_upgrade_post}
    fi
}

function stop_dbnode() {
    if ! check_db_process; then
        return 0
    fi
    gs_ctl stop -D ${PGDATA} >>"${GAUSS_LOG_FILE}" 2>&1
}

function check_db_process() {
    legal_path=$(echo "$GAUSSHOME/bin/gaussdb" | sed 's#//*#/#g')
    ps wwx | grep "${legal_path}" | grep -v grep >/dev/null
}

function query_dn_role() {
    gs_ctl query -D ${PGDATA} >"${GAUSS_TMP_PATH}/temp_dn_role"
    dn_role_temp=$(grep local_role "${GAUSS_TMP_PATH}/temp_dn_role" | head -1 | awk '{print $3}')
    # rm -f "${GAUSS_TMP_PATH}/temp_dn_role"
    if [[ "$dn_role_temp" = "Normal" ]]; then
        dn_role_temp="normal"
    elif [[ "$dn_role_temp" = "Primary" ]]; then
        dn_role_temp="primary"
    elif [[ "$dn_role_temp" = "Standby" ]]; then
        dn_role_temp="standby"
    elif [[ "$dn_role_temp" = "Cascade" ]]; then
        dn_role_temp="cascade_standby"
    else
        dn_role_temp=""
    fi

    dn_role="$dn_role_temp"
    if [[ "$dn_role" != "$dn_role_temp" ]]; then
        die "dn_role maybe not right" ${err_dn_role_null}
    fi
}

function rollback_bin() {
    export GAUSSHOME=${GAUSS_BASE_PATH}
    export LD_LIBRARY_PATH=${GAUSSHOME}/lib:$LD_LIBRARY_PATH
    export PATH=${GAUSSHOME}/bin:$PATH
    start_dbnode
    if ! reload_upgrade_config upgrade_mode 0; then
        die "set upgrade_mode to 0 failed" ${err_upgrade_post}
    fi
}

function rollback_pre_sql() {
    if exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_rollback_maindb.sql maindb && exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_rollback_otherdb.sql otherdb; then
        debug "rollback pre sql successfully"
    else
        die "rollback pre sql failed" ${err_rollback_pre}
    fi
}

function rollback_post() {
    if ! reload_upgrade_config upgrade_mode 2; then
        die "set upgrade_mode to 2 failed" ${err_upgrade_post}
    fi
    if exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_rollback-post_maindb.sql maindb && exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_rollback-post_otherdb.sql otherdb; then
        debug "rollback post sql successfully"
    else
        die "rollback post sql failed" ${err_rollback_post}
    fi
}

function set_upgrade_config() {
    if check_upgrade_config "$1" "$2"; then
        return 0
    fi
    local current_time=$(date +"%Y-%m-%d %H:%M:%S")
    echo -n \[${current_time}\] "  " >>"${GAUSS_LOG_FILE}"
    for i in $(seq 1 3); do
        if gs_guc set -D ${PGDATA} -c "$1=$2" >>"${GAUSS_LOG_FILE}" 2>&1; then
            debug "guc set $1=$2 successfully"
            return 0
        fi
        sleep 2
    done
    return 1
}

function set_upgrade_mode() {
    if ! set_upgrade_config upgrade_mode 2; then
        die "set upgrade_mode to 2 failed" ${err_rollback_bin}
    fi
}

function check_upgrade_mode_by_sql() {
    # check upgrade_mode = 2 by sql
    check_upgrade_mode_result="$GAUSS_TMP_PATH"/".temp_upgrade_mode"
    if echo "" >${check_upgrade_mode_result} && chmod 600 ${check_upgrade_mode_result}; then
        debug "Begin to generate check_upgrade_mode_result."
    else
        log "generate $check_upgrade_mode_result failed."
        return 1
    fi
    gsql -p ${GAUSS_LISTEN_PORT} -d postgres --pipeline -X -t -A \
        -c "show upgrade_mode;" >${check_upgrade_mode_result} 2>&1 &
    sleep 0.1

    for i in $(seq 1 60); do
        check_mode_sql=$(cat ${check_upgrade_mode_result})
        if [[ "$check_mode_sql" == "2" ]]; then
            rm -f ${check_upgrade_mode_result}
            return 0
        elif [[ "$check_mode_sql" == "0" ]]; then
            rm -f ${check_upgrade_mode_result}
            gsql -p ${GAUSS_LISTEN_PORT} -d postgres --pipeline -X -t -A \
                -c "show upgrade_mode;" >${check_upgrade_mode_result} 2>&1 &
        elif [[ "$check_mode_sql" == "" ]]; then
            debug "Wait for check_upgrade_mode_result..."
        else
            log "$(cat ${check_upgrade_mode_result})"
            return 1
        fi
        sleep 0.5
    done
    if [[ -f "${check_upgrade_mode_result}" ]]; then
        debug "check_upgrade_mode_result is $(cat ${check_upgrade_mode_result})"
        rm -f ${check_upgrade_mode_result}
    else
        debug "get upgrade_mode by gsql failed"
    fi
    return 1
}

function exec_sql() {
    #$1: sqlfilename
    #$2: maindb,otherdb
    query_dn_role
    if [[ X"$dn_role" == X"standby" || X"$dn_role" == X"cascade_standby" ]]; then
        return 0
    fi
    if [[ ! -f "$1" ]]; then
        return 0
    fi

    if ! check_upgrade_mode_by_sql; then
        return 1
    fi

    temp_result="$GAUSS_TMP_PATH"/"temp_sql_temp_result_$(date +%Y%m%d_%H%M%S)"
    if echo "" >"$temp_result" && chmod 600 "$temp_result"; then
        debug "begin exec sql ,file name is $1"
    else
        log "Generate $temp_result failed."
    fi
    sqlbegin="gsql -p $GAUSS_LISTEN_PORT -X -t -A "
    if [[ "$2" == "maindb" ]]; then
        if ${sqlbegin} -d postgres --echo-queries --set ON_ERROR_STOP=on -f $1 >>"$temp_result" 2>&1; then
            debug "Exec $1 on database: postgres successfully"
        else
            log "Exec sql on postgres failed."
            debug "$(cat ${temp_result})"
            rm -f ${temp_result}
            return 1
        fi
    else
        if databases=$(${sqlbegin} -d postgres -c "SELECT datname FROM pg_catalog.pg_database where datname != 'postgres';"); then
            temp_num=$(echo ${databases} | awk '{print NF}')
            debug "Num of other databases: $temp_num"
        else
            log "Exec sql to get databases failed."
            return 1
        fi
        for database in ${databases}; do
            debug "Begin exec $1 on database: $database "
            ${sqlbegin} -d ${database} --echo-queries --set ON_ERROR_STOP=on -f $1 >>"$temp_result" 2>&1
        done
    fi
    if grep -wE "ERROR:|FATAL:|could not connect to server" ${temp_result}; then
        log "Exec sql failed."
        debug "$(cat ${temp_result})"
        rm -f ${temp_result}
        return 1
    else
        debug "Exec all sql successfully."
        rm -f ${temp_result}
        return 0
    fi
}

function guc_delete() {
    if [[ ! -f "$GAUSS_TMP_PATH"/temp_sql/upgrade_sql/set_guc/delete_guc ]]; then
        log "No need to delete guc"
    fi
    for para in $(cat "$GAUSS_TMP_PATH"/temp_sql/upgrade_sql/set_guc/delete_guc); do
        if echo ${para} | grep -w datanode >/dev/null; then
            para=$(echo ${para} | awk '{print $1}')
            if sed -i "/^${para}[ =]/d" ${PGDATA}/postgresql.conf; then
                debug "$para was deleted successfully."
            else
                die "$para was deleted failed" ${err_upgrade_bin}
            fi
        fi
    done
    log "Delete guc successfully"
}

function delete_tmp_files() {
    rm -rf "$GAUSS_TMP_PATH"
    rm -rf ${GAUSS_LOG_FILE}
}

function add_pg_proc_index() {
    version=$(gaussdb -V)
    if [[ ! $version =~ "V500R002C00" && ! $version =~ "2.1.0" ]]; then
        return 0
    fi
    add_index_cmd="start transaction; set isinplaceupgrade=on;
ALTER INDEX pg_proc_proname_args_nsp_index unusable;
SET LOCAL inplace_upgrade_next_system_object_oids = IUO_CATALOG, false, true, 0, 0, 0, 9666;
CREATE INDEX pg_catalog.pg_proc_proname_all_args_nsp_index on pg_catalog.pg_proc USING BTREE(proname name_ops, pronamespace oid_ops, propackageid oid_ops);
SET LOCAL inplace_upgrade_next_system_object_oids = IUO_CATALOG, false, true, 0, 0, 0, 0;
commit;"
    sqlbegin="gsql -p $GAUSS_LISTEN_PORT -X -t -A "
    result=$(${sqlbegin} -d postgres -c "${add_index_cmd}")
    if [ $? -ne 0 ]; then
        log "Exec sql to get databases failed."
        return 1
    fi
}

function upgrade_pre() {
    # 1.检查环境变量,版本,磁盘
    check_env
    check_version
    check_disk
    # 2.准备sql
    prepare_sql_all
    # 3.设置升级模式
    if ! reload_upgrade_config upgrade_mode 2; then
        die "set upgrade_mode to 0 failed" ${err_upgrade_pre}
    fi
    # 4.添加pg_proc_index
    add_pg_proc_index
    # 5.执行pre sql
    pre_exec_sql
    if [ $? -ne 0 ]; then
        rollback_pre_sql
        exit 1
    fi
}

function remove_path() {
    local remove_dir="$1"
    export PATH=$(echo $PATH | tr ':' '\n' | grep -v "^${remove_dir}$" | tr '\n' ':' | sed 's/:$//')
}

function remove_lib() {
    local remove_dir="$1"
    export LD_LIBRARY_PATH=$(echo $LD_LIBRARY_PATH | tr ':' '\n' | grep -v "^${remove_dir}$" | tr '\n' ':' | sed 's/:$//')
}

function upgrade_bin() {
    # 1.停止旧库
    if ! stop_dbnode; then
        die "Stop gaussdb failed" ${err_upgrade_bin}
    fi
    # 2.删除guc
    guc_delete
    # 3.启动新库
    remove_path ${GAUSS_BASE_PATH}/bin
    remove_lib ${GAUSS_BASE_PATH}/lib
    export GAUSSHOME=${GAUSS_UPGRADE_BASE_PATH}
    export LD_LIBRARY_PATH=${GAUSSHOME}/lib:$LD_LIBRARY_PATH
    export PATH=${GAUSSHOME}/bin:$PATH
    start_dbnode
}

function upgrade_post() {
    # 1.执行升级sql，升级元数据
    if ! check_db_process; then
        die "Guassdb is not running" ${err_upgrade_post}
    fi
    if exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_upgrade-post_maindb.sql maindb && exec_sql "$GAUSS_TMP_PATH"/temp_sql/temp_upgrade-post_otherdb.sql otherdb; then
        debug "upgrade post sql successfully"
    else
        log "upgrade post sql failed"
        rollback_post
        exit 1
    fi
}

function upgrade_commit() {
    # 1.设置升级模式为0
    if ! reload_upgrade_config upgrade_mode 0; then
        die "set upgrade_mode to 0 failed" ${err_upgrade_commit}
    fi
    # 2.删除临时文件
    # delete_tmp_files
}

function cp_bak() {
    cp -rf ${GAUSS_BASE_PATH}/* ${GAUSS_BACKUP_BASE_PATH}
    # rm -rf ${GAUSS_BASE_PATH}/*
}

function cp_new() {
    cp -rf ${GAUSS_UPGRADE_BASE_PATH}/* ${GAUSS_BASE_PATH}
}

function main() {
    cp_bak
    cp_new_all_pkg_to_tmp_dir
    upgrade_pre
    upgrade_bin
    upgrade_post
    upgrade_commit
    stop_dbnode
    # cp_new
    # remove_path ${GAUSS_UPGRADE_BASE_PATH}/bin
    # remove_lib ${GAUSS_UPGRADE_BASE_PATH}/lib
    # source /var/lib/opengauss/.bash_profile
    # which gs_ctl
    # start_dbnode
    return 0
}

main
