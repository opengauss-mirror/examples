<template>
  <div class="page-container">
    <div class="page-main">
      <el-collapse v-model="activeNames">
        <el-collapse-item name="1">
          <template slot="title">
            cpu使用率
            <!-- <i class="header-icon el-icon-info"></i> -->
          </template>
          <div style="width:100%">
            <div id="chart1" style="width: 100%; height: 376px"></div>
          </div>
        </el-collapse-item name="2">
        <el-collapse-item title="panel2">
          <template slot="title">
            内存使用率
            <!-- <i class="header-icon el-icon-info"></i> -->
          </template>
          <div style="width:100%">
            <div id="chart2" style="width: 100%; height: 376px"></div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>
<script>
import { monitorApi } from "@/api/snapshot";
const Mock = require("mockjs");
export default {
  data() {
    return {
      activeNames: ["1"],
      datas: [],
    };
  },
  mounted() {
    var chartDom = document.getElementById("chart1");
    var myChart = this.$echarts.init(chartDom);
    var option;
    let data = [];
    option = {
      title: {
        text: "",
      },
      tooltip: {
        trigger: "axis",
        axisPointer: {
          animation: false,
        },
        formatter: function (p) {
          const t = new Date(Number(p[0].name));
          return (
            "时间：" +
            t.getHours() +
            ":" +
            (t.getMinutes() > 9 ? t.getMinutes() : "0" + t.getMinutes()) +
            ":" +
            t.getSeconds() +
            "<br>" +
            "使用率：" +
            p[0].value +
            "%"
          );
        },
      },
      grid: {
        left: "10%",
        right: "15%",
      },
      xAxis: {
        // type: "value",
        name: "时间（时:分）",
        splitLine: {
          show: false,
        },
        axisLabel: {
          formatter(p) {
            const t = new Date(Number(p));
            return (
              t.getHours() +
              ":" +
              (t.getMinutes() > 9 ? t.getMinutes() : "0" + t.getMinutes()) +
              ":" +
              t.getSeconds()
            );
          },
        },
        data: [],
      },
      yAxis: {
        type: "value",
        min: 0,
        max: 200,
        name: "使用率（%）",
        boundaryGap: [0, "100%"],
        splitLine: {
          show: true,
        },
        axisLine: {
          show: true,
        },
        axisTick: {
          show: true,
        },
      },
      series: [
        {
          name: "cpu使用率",
          type: "line",
          showSymbol: false,
          data: data,
        },
      ],
      timer1: null,
    };
    option && myChart.setOption(option);

    const temp = [];
    monitorApi({ type: 1 })
      .then((res) => {
        this.datas = res.data.result[0].values;
        this.datas.forEach((v) => {
          const t = new Date(v[0] * 1000);
          data.push((Number(v[1]) * 100).toFixed(2));
          temp.push(v[0] * 1000);
        });

        myChart.setOption({
          xAxis: {
            data: temp,
          },
          series: [
            {
              data: data,
            },
          ],
        });

        let k = 0;
        this.timer1 = setInterval(() => {
          for (let i = 0; i < 5; i++) {
            const tt =
              this.datas[this.datas.length - 1][0] * 1000 + (k + 1) * 1000;
            const t = new Date(tt);
            const { v1, v2 } = Mock.mock({
              "v1|1": [
                {
                  a: tt,
                },
              ],
              "v2|1": [{ a: "@integer(25,40)" }],
            });
            temp.push(v1.a);
            data.push(v2.a);
            k++;
          }
          console.log("v1===", temp);
          myChart.setOption({
            xAxis: {
              data: temp,
            },
            series: [
              {
                data: data,
              },
            ],
          });
          for (let i = 0; i < 5; i++) {
            temp.shift();
            data.shift();
          }

          myChart.setOption({
            xAxis: {
              data: temp,
            },
            series: [
              {
                data: data,
              },
            ],
          });
        }, 1000);
      })
      .catch((e) => {
        console.log(e);
      });
  },
  destroyed() {
    clearInterval(this.timer1);
  },
};
</script>
<style lang="scss">
.page-container {
  height: calc(100vh - 50px);
  background-color: #ececec;
  overflow: hidden;
  .page-main {
    height: calc(100% - 40px);
    margin: 20px;
    padding: 10px;
    background-color: #ffffff;
    border-radius: 6px;
    .el-collapse-item__header,
    .el-collapse-item__wrap {
      // background-color: #ececec;
    }
  }
}
</style>