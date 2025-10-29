// src/boot/axios.js
import { boot } from 'quasar/wrappers'
import axios from 'axios'
import { Notify } from 'quasar'
import { getToken } from '../utils/auth'

// 创建 axios 实例
const { protocol, host, port } = window.location
let baseURL = `${protocol}//${host}/plugins/er-model/`

if (port === '3211') {
  // 本地插件独立开发环境
  baseURL = `${protocol}//${host}/plugins/er-model/`
}

const api = axios.create({
  baseURL,
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 如果返回的是字符串（如导出 SQL），直接返回
    if (typeof response.data === 'string') {
      return response
    }

    const code = response.data.code || 200
    const msg = response.data.message || '接口返回错误'

    if (code === 401) {
      Notify.create({ type: 'negative', message: '未登录或登录已过期' })
      return Promise.reject(new Error('请先登录'))
    } else if (code !== 200) {
      Notify.create({ type: 'negative', message: msg })
      return Promise.reject(new Error(msg))
    }

    return response.data
  },
  (error) => {
    Notify.create({ type: 'negative', message: error.message || '网络异常' })
    return Promise.reject(error)
  }
)

export default boot(({ app }) => {
  // 在全局注册 axios 实例
  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

export { axios, api }
