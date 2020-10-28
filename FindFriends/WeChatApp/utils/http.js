/*
 * @Author: Lamdaer
 * @Date: 2020-10-24 02:07:32
 * @LastEditTime: 2020-10-24 02:11:05
 * @Description:
 * @FilePath: /opengauss/utils/http.js
 */
import { config } from '../config'
const tips = {
  1: '抱歉出现了一个错误',
}
export class HTTP {
  request({ url, data = {}, method = 'GET' }) {
    return new Promise((resolve, resject) => {
      this._request(url, resolve, resject, data, method)
    })
  }
  _request(url, resolve, reject, data = {}, method = 'GET') {
    wx.request({
      url: config.API_BASE_URL + url,
      method: method,
      data: data,
      success: (res) => {
        const code = res.statusCode.toString()
        if (code.startsWith('2')) {
          resolve(res.data)
        } else {
          reject()
          const error_code = res.data.code
          this._show_error(error_code)
        }
      },
      fail: (res) => {
        reject()
        console.log(res)
        this._show_error(1)
      },
    })
  }
  _show_error(error_code) {
    if (!error_code) {
      error_code = 1
    }
    wx.showToast({
      title: tips[error_code],
      icon: 'none',
      duration: 2000,
    })
  }
}
