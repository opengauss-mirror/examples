<template>
  <div class="page-container">
    <div class="page-main">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="6">
            <el-form-item prop="beginSnapId" label="起始快照">
              <el-select
                v-model="form.beginSnapId"
                placeholder="请选择起始快照"
                size="small"
              >
                <el-option
                  v-for="(v, i) in snapshots"
                  :key="i"
                  :label="'快照ID：' + v.snapshot_id + ' 时间：' + v.start_ts"
                  :value="v.snapshot_id"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item prop="endSnapId" label="结束快照">
              <el-select
                v-model="form.endSnapId"
                placeholder="请选择结束快照"
                size="small"
              >
                <el-option
                  v-for="(v, i) in snapshots"
                  :key="i"
                  :label="'快照ID：' + v.snapshot_id + ' 时间：' + v.end_ts"
                  :value="v.snapshot_id"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item prop="reportType" label="report类型">
              <el-select
                v-model="form.reportType"
                placeholder="请选择report类型"
                size="small"
              >
                <el-option
                  v-for="(v, i) in reportTypes"
                  :key="i"
                  :label="v.label"
                  :value="v.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item prop="reportScope" label="report范围">
              <el-select
                v-model="form.reportScope"
                placeholder="请选择report的范围"
                size="small"
              >
                <el-option
                  v-for="(v, i) in reportScopes"
                  :key="i"
                  :label="v.label"
                  :value="v.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <el-form-item prop="nodeName" label="节点名称">
              <el-input
                v-model="form.nodeName"
                placeholder="请输入节点名称"
                size="small"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item prop="reportName" label="报告名称">
              <el-input
                v-model="form.reportName"
                placeholder="请输入报告名称"
                size="small"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="btns">
        <el-button
          class="s-btn"
          type="primary"
          size="small"
          @click="createSnapshot"
          >生成快照</el-button
        >
        <el-button
          class="s-btn s-btn2"
          type="primary"
          size="small"
          @click="downLoadSnapshot"
          >查看报告</el-button
        >
      </div>
    </div>
  </div>
</template>

<script>
import {
  snapshotListApi,
  snapshotGenerateApi,
  snapshotDownloadApi,
} from "@/api/snapshot";

export default {
  data() {
    return {
      form: {},
      rules: {
        beginSnapId: [
          {
            required: true,
            message: "请选择开始快照",
            trigger: "change",
          },
        ],
        endSnapId: [
          {
            required: true,
            message: "请选择结束快照",
            trigger: "change",
          },
        ],
        reportType: [
          {
            required: true,
            message: "请选择report类型",
            trigger: "change",
          },
        ],
        reportScope: [
          {
            required: true,
            message: "请选择report范围",
            trigger: "change",
          },
        ],
        reportName: [
          {
            required: true,
            message: "请输入报告名称",
            trigger: "blur",
          },
        ],
      },
      snapshots: [],
      reportTypes: [
        {
          label: "汇总数据",
          value: "summary",
        },
        {
          label: "明细数据",
          value: "detail",
        },
        {
          label: "汇总数据+明细数据",
          value: "all",
        },
      ],
      reportScopes: [
        {
          label: "数据库级别的信息",
          value: "cluster",
        },
        {
          label: "节点级别的信息",
          value: "node",
        },
      ],
    };
  },
  mounted() {
    snapshotListApi()
      .then((res) => {
        if (res.code == 200) {
          this.snapshots = JSON.parse(res.result);
          this.snapshots.map((v) => {
            const s = new Date(v.start_ts);
            const e = new Date(v.end_ts);
            v.start_ts =
              [
                s.getFullYear(),
                this.formaterTime(s.getMonth() + 1),
                this.formaterTime(s.getDate()),
              ].join("-") +
              " " +
              [
                this.formaterTime(s.getHours()),
                this.formaterTime(s.getMinutes()),
                this.formaterTime(s.getSeconds()),
              ].join(":");
            v.end_ts =
              [
                e.getFullYear(),
                this.formaterTime(e.getMonth() + 1),
                this.formaterTime(e.getDate()),
              ].join("-") +
              " " +
              [
                this.formaterTime(e.getHours()),
                this.formaterTime(e.getMinutes()),
                this.formaterTime(e.getSeconds()),
              ].join(":");
          });
        } else {
          this.$message({
            type: "error",
            message: res.message,
          });
        }
      })
      .catch((e) => {
        this.$message({
          type: "error",
          message: e,
        });
      });
  },
  methods: {
    formaterTime(time) {
      return time < 10 ? "0" + time : time;
    },
    createSnapshot() {
      snapshotGenerateApi()
        .then((res) => {
          if (res.code == 200) {
            this.$message({ type: "success", message: "生成报告成功" });
          } else {
            this.$message({ type: "success", message: res.message });
          }
        })
        .catch((e) => {
          this.$message({
            type: "error",
            message: e,
          });
        });
    },
    downLoadSnapshot() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          snapshotDownloadApi(this.form)
            .then((res) => {
              if (res.code == 200) {
                window.open(res.result.url);
              } else {
                this.$message({
                  type: "error",
                  message: res.message,
                });
              }
            })
            .catch((e) => {
              this.$message({
                type: "error",
                message: e.message,
              });
            });
        }
      });
    },
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
  }
  .el-select {
    width: 100%;
  }
  .btns {
    margin: 0 30px;
    .s-label {
      display: inline-block;
      width: 100px;
      color: #666666;
    }
    .s-btn2 {
      margin-left: 10px;
    }
  }
}
</style>