/*
 * @Author: Lamdaer
 * @Date: 2020-10-24 01:54:38
 * @LastEditTime: 2020-10-24 02:41:40
 * @Description:
 * @FilePath: /opengauss/components/info/index.js
 */
// components/info/index.js
Component({
  /**
   * 组件的属性列表
   */
  properties: {
    jobList: {
      type: Array,
    },
  },

  /**
   * 组件的初始数据
   */
  data: {
    index: null,
  },

  /**
   * 组件的方法列表
   */
  methods: {
    jobChange(e) {
      console.log(e)
      this.setData({
        index: e.detail.value,
      })
    },
  },
})
