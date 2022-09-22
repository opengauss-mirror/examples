<template>
  <div class="page-container">
    <div class="page-main">
      <el-table v-loading="loading" :data="data1">
        <el-table-column prop="node_name" label="node_name"></el-table-column>
        <el-table-column
          prop="thread_name"
          label="thread_name"
        ></el-table-column>
        <el-table-column prop="query_id" label="query_id"></el-table-column>
        <el-table-column prop="tid" label="tid"></el-table-column>
        <el-table-column prop="sessionid" label="sessionid"></el-table-column>
        <el-table-column prop="lwtid" label="lwtid"></el-table-column>
        <el-table-column prop="tlevel" label="tlevel"></el-table-column>
        <el-table-column prop="smpid" label="smpid"></el-table-column>
        <el-table-column
          prop="wait_status"
          label="wait_status"
        ></el-table-column>
        <el-table-column prop="wait_event" label="wait_event"></el-table-column>
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
      data1: [],
    };
  },
  filters: {
    timestapToDate: function (val) {
      return new Date(val).toLocaleString();
    },
  },
  created() {
    this.loading = true;
    this.f1();
  },
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
    f1() {
      getSessionRelationApi()
        .then((res) => {
          this.data1 = JSON.parse(res.result);
        })
        .finally(() => {
          this.loading = false;
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