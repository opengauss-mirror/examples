import request from '@/utils/request'

export function login(params) {
    return request({
        url: '/basic/login',
        method: 'post',
        data: params
    })
}

export function snapshotListApi() {
    return request({
        url: '/jdbc/getSnapshotList',
        method: 'get'
    })
}

export function snapshotGenerateApi() {
    return request({
        url: '/jdbc/generateSnapshot',
        method: 'get'
    })
}

export function snapshotDownloadApi(params) {
    return request({
        url: "/jdbc/downloadWRDReport",
        method: 'post',
        params
    })
}

export function slowSql(params) {
    return request({
        url: "/jdbc/checkSlowSql",
        method: 'get',
        params
    })
}

export function monitorApi1(params) {
    return request({
        url: '/screen/system',
        method: 'get',
        params
    })
}

export function monitorApi2(params) {
    return request({
        url: '/screen/gauss',
        method: 'get',
        params
    })
}

// session性能分析
export function getSessionRelationApi(params) {
    return request({
        url: '/session/getSessionRelation',
        method: 'get',
        params
    })
}
export function getBlockSessionInfoApi(params) {
    return request({
        url: '/session/getBlockSessionInfo',
        method: 'get',
        params
    })
}
export function getMostWaitEventApi(params) {
    return request({
        url: '/session/getMostWaitEvent',
        method: 'get',
        params
    })
}
export function getEventOfSessionByTimeApi(params) {
    return request({
        url: '/session/getEventOfSessionByTime',
        method: 'get',
        params
    })
}
export function getEventOfSqlByTimeApi(params) {
    return request({
        url: '/session/getEventOfSqlByTime',
        method: 'get',
        params
    })
}
