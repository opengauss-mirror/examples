/*
 * @Author: Lamdaer
 * @Date: 2020-10-24 02:18:15
 * @LastEditTime: 2020-10-24 02:30:08
 * @Description:
 * @FilePath: /opengauss/model/job.js
 */
import { HTTP } from '../utils/http'
export class JobModel extends HTTP {
  getJobList() {
    return this.request({
      url: '/gauss/job',
    })
  }
}
