<template>
  <div class="page-container">
    <div class="page-main">
      <el-form
        class="sql-form"
        ref="form"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-row>
          <el-col :span="7">
            <el-form-item prop="startTime" label="开始时间">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                size="small"
                value-format="yyyy-MM-dd HH:mm:ss"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item prop="endTime" label="结束时间">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                size="small"
                value-format="yyyy-MM-dd HH:mm:ss"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="4" :offset="1">
            <el-button type="primary" size="small" @click="queryForm"
              >查询</el-button
            >
          </el-col>
        </el-row>
      </el-form>
      <el-table v-loading="loading" :data="data">
        <el-table-column prop="sampleid" label="sampleid"></el-table-column>
        <el-table-column prop="sample_time" label="sample_time">
          <template slot-scope="scope">
            <div>{{ scope.row.sample_time | formatterTime }}</div>
          </template></el-table-column
        >
        <el-table-column
          prop="need_flush_sample"
          label="need_flush_sample"
        ></el-table-column>
        <el-table-column prop="databaseid" label="databaseid"></el-table-column>
        <el-table-column prop="thread_id" label="thread_id"></el-table-column>
        <el-table-column prop="sessionid" label="sessionid"></el-table-column>
        <el-table-column prop="start_time" label="start_time">
          <template slot-scope="scope">
            <div>{{ scope.row.start_time | formatterTime }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="event" label="event"></el-table-column>
        <el-table-column prop="lwtid" label="lwtid"></el-table-column>
        <el-table-column prop="tlevel" label="tlevel"></el-table-column>
        <el-table-column prop="smpid" label="smpid"></el-table-column>
        <el-table-column prop="userid" label="userid"></el-table-column>
        <el-table-column
          prop="application_name"
          label="application_name"
        ></el-table-column>
        <el-table-column prop="dn_6001" label="dn_6001"></el-table-column>
        <el-table-column prop="query_id" label="query_id"></el-table-column>
        <el-table-column
          prop="wait_status"
          label="wait_status"
        ></el-table-column>
        <el-table-column
          prop="global_sessionid"
          label="global_sessionid"
        ></el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script>
import {
  getSessionRelationApi,
  getBlockSessionInfoApi,
  getMostWaitEventApi,
  getEventOfSessionByTimeApi,
  getEventOfSqlByTimeApi,
} from "@/api/snapshot";
export default {
  data() {
    return {
      loading: false,
      data: [],
      form: {},
      rules: {
        startTime: [
          {
            required: true,
            message: "请选择开始时间",
            trigger: "blur",
          },
        ],
        endTime: [
          {
            required: true,
            message: "请选择结束时间",
            trigger: "blur",
          },
        ],
      },
    };
  },
  filters: {
    timestapToDate: function (val) {
      return new Date(val).toLocaleString();
    },
  },
  created() {},
  filters: {
    formatterTime(val) {
      const s = new Date(val);
      return (
        s.getFullYear() +
        "-" +
        (s.getMonth() + 1) +
        "-" +
        s.getDate() +
        " " +
        s.getHours() +
        ":" +
        s.getMinutes() +
        ":" +
        s.getSeconds()
      );
    },
  },
  methods: {
    formatterTime(val) {
      return (
        val.getFullYear() +
        "-" +
        (val.getMonth() + 1) +
        "-" +
        val.getDate() +
        " " +
        val.getHours() +
        ":" +
        val.getMinutes() +
        ":" +
        val.getSeconds()
      );
    },
    queryForm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.loading = true;
          const s = new Date(this.form.startTime);
          const e = new Date(this.form.endTime);
          if (s > e) {
            this.$message({
              type: "error",
              message: "开始时间不能大于结束时间",
            });
            return;
          }
          getBlockSessionInfoApi(this.form)
            .then((res) => {
              if (res.code == 200) {
                this.data = JSON.parse(res.result) || [];
              }
            })
            .finally(() => {
              this.loading = false;
            });
        } else {
          this.loading = false;
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
    overflow: auto;
    .el-tabs {
      height: 100%;
    }
    .el-tabs__content {
      height: calc(100% - 55px);
      overflow: auto;
    }
  }
}
.sql-form {
  .el-form-item__content {
    line-height: 1 !important;
  }
  .el-form-item__label {
    line-height: 32px !important;
  }
  .el-input {
    width: 100% !important;
  }
}
</style>