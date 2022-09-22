<template>
  <div class="page-container">
    <div class="page-main">
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart1"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart2"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <!-- <div style="width:45%;margin-bottom:40px"> -->
            <!-- <div
              id="chart3"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div> -->
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart4"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart5"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart6"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <!-- <div style="width:45%;margin-bottom:40px"> -->
            <!-- <div
              id="chart7"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px"> -->
            <!-- <div
              id="chart8"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div> -->
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart9"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart10"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart11"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
          <div style="width:45%;margin-bottom:40px">
            <div
              id="chart12"
              class="chart"
              style="width: 100%; height: 376px"
            ></div>
          </div>
        </el-collapse-item>
    </div>
  </div>
</template>
<script>
import { monitorApi1, monitorApi2 } from "@/api/snapshot";
const Mock = require("mockjs");
export default {
  data() {
    return {
      lastTimeArr: [],
      seriesParentArr: [],
      timer1: null,
      timer2: null,
      timer3: null,
      timer4: null,
      timer5: null,
      timer6: null,
      timer7: null,
      timer8: null,
      timer9: null,
      timer10: null,
      timer11: null,
      timer12: null,
    };
  },
  mounted() {
    this.lastTimeArr = Array(20).fill(0);
    this.seriesParentArr = Array(20);
    this.generateChart("chart1", 1, "cpu使用率", 1);
    this.generateChart("chart2", 2, "内存使用率", 1);
    // this.generateChart("chart3", 3, "磁盘使用率", 1);
    this.generateChart("chart4", 4, "磁盘读写IO", 1);
    this.generateChart("chart5", 5, "磁盘读IO", 1);
    this.generateChart("chart6", 6, "磁盘写IO", 1);
    // this.generateChart("chart7", 7, "上行宽带", 1);
    // this.generateChart("chart8", 8, "下行宽带", 1);

    this.generateChart("chart9", 9, "数据库后端连接数", 2);
    this.generateChart("chart10", 10, "数据库使用磁盘空间", 2);
    this.generateChart(
      "chart11",
      11,
      "由于锁定超时而取消此数据库中的查询数",
      2
    );
    this.generateChart("chart12", 12, "数据库中由于死锁而取消的查询数", 2);
  },
  methods: {
    generateChart(id, type, text, apiType, startTime = "", step) {
      const chartDom = document.getElementById(id);
      const myChart = this.$echarts.init(chartDom);
      var option;

      let series1 = [];
      option = {
        title: {
          text: text,
        },
        tooltip: {
          trigger: "axis",
          axisPointer: {
            animation: false,
          },
        },
        grid: {
          left: type == 10 ? "20%" : "10%",
          right: "20%",
        },
        xAxis: {
          type: "time",
          name: "时间（时:分）",
          splitLine: {
            show: false,
          },
          axisLabel: {
            rotate: -30,
            formatter(p) {
              const t = new Date(p);
              return (
                // [t.getFullYear(), t.getMonth() + 1, t.getDate()].join("/") +
                // " " +
                // t.getHours() +
                // ":" +
                (t.getMinutes() > 9 ? t.getMinutes() : "0" + t.getMinutes()) +
                ":" +
                (t.getSeconds() > 9 ? t.getSeconds() : "0" + t.getSeconds())
              );
            },
          },
        },
        yAxis: {
          type: "value",
          name: "",
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
            name: "",
            type: "line",
            showSymbol: false,
            data: [],
          },
          {
            name: "",
            type: "line",
            showSymbol: false,
            data: [],
          },
          {
            name: "",
            type: "line",
            showSymbol: false,
            data: [],
          },
        ],
      };
      option && myChart.setOption(option);

      const params = { type: type < 9 ? type : type - 8 };
      if (startTime) {
        const t = new Date(startTime);
        const y = t.getFullYear();
        const mon = t.getMonth() + 1;
        const d = t.getDate();
        const h = t.getHours();
        const m = t.getMinutes();

        params.startTime =
          new Date([y, mon, d].join("-") + " " + [h, m].join(":")).getTime() /
          1000;
      }
      if (step) {
        // params.step = "8";
      }
      let api = monitorApi1;
      switch (apiType) {
        case 1:
          api = monitorApi1;
          break;
        case 2:
          api = monitorApi2;
          break;
      }
      api(params)
        .then((res) => {
          let temp = [];
          temp = res.data.result;
          this.lastTimeArr.splice(
            type,
            1,
            temp[0].values[temp[0].values.length - 1][0]
          );

          let seriesArr = [];
          for (let i = 0; i < temp.length; i++) {
            let arr = [];
            temp[i].values.forEach((v) => {
              const t = new Date(v[0] * 1000);
              arr.push({
                name: t.toString(),
                label: temp[i].metric.device || temp[i].metric.datname,
                value: [
                  [t.getFullYear(), t.getMonth() + 1, t.getDate()].join("/") +
                    " " +
                    t.getHours() +
                    ":" +
                    t.getMinutes() +
                    ":" +
                    t.getSeconds(),
                  v[1],
                ],
              });
            });
            seriesArr.push(arr);
          }
          this.seriesParentArr.splice(type - 1, 1, seriesArr);

          let arr = [];
          for (let i = 0; i < this.seriesParentArr[type - 1].length; i++) {
            arr.push({
              data: this.seriesParentArr[type - 1][i],
            });
          }
          myChart.setOption({
            series: arr,
          });

          this["timer" + type] = setInterval(() => {
            api(
              Object.assign(params, { startTime: this.lastTimeArr[type] })
            ).then((res) => {
              for (let i = 0; i < this.seriesParentArr[type - 1].length; i++) {
                this.seriesParentArr[type - 1][i].shift();
              }
              temp = res.data.result;
              this.lastTimeArr.splice(
                type,
                1,
                temp[0].values[temp[0].values.length - 1][0]
              );

              let seriesArr = [];
              for (let i = 0; i < temp.length; i++) {
                let arr = [];
                temp[i].values.forEach((v, k) => {
                  // 去掉重复的点
                  if (k == 0) {
                    return;
                  }
                  const t = new Date(v[0] * 1000);
                  arr.push({
                    name: t.toString(),
                    label: temp[i].metric.device || temp[i].metric.datname,
                    value: [
                      [t.getFullYear(), t.getMonth() + 1, t.getDate()].join(
                        "/"
                      ) +
                        " " +
                        t.getHours() +
                        ":" +
                        t.getMinutes() +
                        ":" +
                        t.getSeconds(),
                      v[1],
                    ],
                  });
                });
                seriesArr.push(arr);
              }
              for (let i = 0; i < seriesArr.length; i++) {
                this.seriesParentArr[type - 1][i] = this.seriesParentArr[
                  type - 1
                ][i].concat(seriesArr[i]);
              }
              // console.log("seriesParentArr====", this.seriesParentArr[type - 1]);
              let arr = [];
              for (let i = 0; i < this.seriesParentArr[type - 1].length; i++) {
                arr.push({
                  data: this.seriesParentArr[type - 1][i],
                });
              }
              myChart.setOption({
                series: arr,
              });
            });
          }, 14000);
        })
        .catch((e) => {
          console.log(e);
        });
    },
  },
  destroyed() {
    this.lastTimeArr.forEach((v, i) => {
      clearInterval(this["timer" + (i + 1)]);
    });
  },
};
</script>
<style lang="scss" scoped>
.page-container {
  height: calc(100vh - 50px);
  background-color: #ececec;
  overflow: hidden;
  .page-main {
    height: calc(100% - 40px);
    margin: 20px;
    padding: 10px 20px;
    background-color: #ffffff;
    border-radius: 6px;
    overflow: auto;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
  }
}
</style>