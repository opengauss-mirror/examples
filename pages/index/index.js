/*
 * @Author: Lamdaer
 * @Date: 2020-10-24 01:58:20
 * @LastEditTime: 2020-10-24 03:28:07
 * @Description:
 * @FilePath: /opengauss/pages/index/index.js
 */
// pages/index/index.js
import { JobModel } from '../../model/job'
import { HobbyParentModel } from '../../model/hobby-parent'
const jobModel = new JobModel()
const hobbyParentModel = new HobbyParentModel()
Page({
  getJobList() {
    jobModel.getJobList().then((response) => {
      this.setData({
        jobList: response.data.jobList,
      })
      console.log(response)
    })
  },
  getHobbyList() {
    hobbyParentModel.getHobbyList().then((res) => {
      console.log(res)
    })
  },
  /**
   * 页面的初始数据
   */
  data: {
    jobList: [],
    hobbyParentList: [],
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    this.getJobList()
    this.getHobbyList()
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {},

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {},

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {},

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {},

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {},

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {},

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {},
})
