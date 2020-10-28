/*
 * @Author: Lamdaer
 * @Date: 2020-10-24 02:56:28
 * @LastEditTime: 2020-10-24 03:27:02
 * @Description:
 * @FilePath: /opengauss/model/hobby-parent.js
 */
import { HTTP } from '../utils/http'
export class HobbyParentModel extends HTTP {
  getHobbyList() {
    return this.request({
      url: '/gauss/hobby_parent/with_children',
    })
  }
}
