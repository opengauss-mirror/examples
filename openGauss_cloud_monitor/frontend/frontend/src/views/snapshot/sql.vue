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
            <el-form-item prop="beginTime" label="开始时间">
              <el-date-picker
                v-model="form.beginTime"
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
            <el-button type="primary" size="small" @click="getSlowSql"
              >查询</el-button
            >
          </el-col>
        </el-row>
      </el-form>

      <el-table :data="data">
        <el-table-column prop="node_name" label="节点名称"></el-table-column>
        <el-table-column prop="db_name" label="数据库名称"></el-table-column>
        <el-table-column prop="schema_name" label="模式"></el-table-column>
        <!-- <el-table-column prop="public"></el-table-column> -->
        <!-- <el-table-column prop="origin_node"></el-table-column> -->
        <el-table-column prop="user_name" label="用户名"></el-table-column>
        <el-table-column
          prop="application_name"
          label="客户端名称"
        ></el-table-column>
        <el-table-column
          prop="client_addr"
          label="客户端地址"
        ></el-table-column>
        <el-table-column
          prop="client_port"
          label="客户端端口"
        ></el-table-column>
        <!-- <el-table-column prop="unique_query_id"></el-table-column>
        <el-table-column prop="debug_query_id"></el-table-column> -->
        <el-table-column prop="query" label="sql语句">
          <template slot-scope="scope">
            <el-popover trigger="hover" width="600" placement="top">
              <p>{{ scope.row.query }}</p>
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium">sql语句</el-tag>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="start_time" label="开始时间">
          <template slot-scope="scope">
            <div>{{ scope.row.start_time | timestapToDate }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="finish_time" label="结束时间">
          <template slot-scope="scope">
            <div>{{ scope.row.finish_time | timestapToDate }}</div>
          </template>
        </el-table-column>
        <!-- <el-table-column prop="slow_sql_threshold"></el-table-column>
        <el-table-column prop="transaction_id"></el-table-column>
        <el-table-column prop="thread_id"></el-table-column>
        <el-table-column prop="session_id"></el-table-column>
        <el-table-column prop="n_soft_parse"></el-table-column>
        <el-table-column prop="n_hard_parse"></el-table-column>
        <el-table-column prop="query_plan"></el-table-column>
        <el-table-column prop="n_returned_rows"></el-table-column>
        <el-table-column prop="n_tuples_fetched"></el-table-column>
        <el-table-column prop="n_tuples_returned"></el-table-column>
        <el-table-column prop="n_tuples_inserted"></el-table-column>
        <el-table-column prop="n_tuples_updated"></el-table-column>
        <el-table-column prop="n_tuples_deleted"></el-table-column>
        <el-table-column prop="n_blocks_fetched"></el-table-column>
        <el-table-column prop="n_blocks_hit"></el-table-column>
        <el-table-column prop="db_time"></el-table-column>
        <el-table-column prop="cpu_time"></el-table-column>
        <el-table-column prop="execution_time"></el-table-column>
        <el-table-column prop="parse_time"></el-table-column>
        <el-table-column prop="plan_time"></el-table-column>
        <el-table-column prop="rewrite_time"></el-table-column>
        <el-table-column prop="pl_execution_time"></el-table-column>
        <el-table-column prop="pl_compilation_time"></el-table-column>
        <el-table-column prop="data_io_time"></el-table-column>
        <el-table-column prop="net_send_info"></el-table-column>
        <el-table-column prop="net_recv_info"></el-table-column>
        <el-table-column prop="net_stream_send_info"></el-table-column>
        <el-table-column prop="net_stream_recv_info"></el-table-column>
        <el-table-column prop="lock_count"></el-table-column>
        <el-table-column prop="lock_time"></el-table-column>
        <el-table-column prop="lock_wait_count"></el-table-column>
        <el-table-column prop="lock_wait_time"></el-table-column>
        <el-table-column prop="lock_max_count"></el-table-column>
        <el-table-column prop="lwlock_count"></el-table-column>
        <el-table-column prop="lwlock_wait_count"></el-table-column>
        <el-table-column prop="lwlock_time"></el-table-column>
        <el-table-column prop="lwlock_wait_time"></el-table-column>
        <el-table-column prop="details"></el-table-column>
        <el-table-column prop="is_slow_sql"></el-table-column>
        <el-table-column prop="trace_id"></el-table-column> -->
      </el-table>
    </div>
  </div>
</template>
<script>
import { slowSql } from "@/api/snapshot";
export default {
  data() {
    return {
      form: {},
      rules: {
        beginTime: [
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
      data: [
        // {
        //   node_name: "dn_6001",
        //   db_name: "postgres",
        //   schema_name: '"$user"',
        //   public: "",
        //   origin_node: "0",
        //   user_name: "coder",
        //   application_name: "Data Studio",
        //   client_addr: "171.36.30.253",
        //   client_port: "17026",
        //   unique_query_id: "1462432811",
        //   debug_query_id: "5348024557502676",
        //   query:
        //     "DECLARE\r\n  count INTEGER := 1;\r\nBEGIN LOOP\r\n    IF count > 1000000 THEN\r\n      raise info 'count is %. ', count;\r\n\r\n      EXIT;\r\n\r\n    ELSE\r\n      count :=count+1;\r\n      insert into coder.tb_test\r\n        (id, name)\r\n      values\r\n        (count, '王五');\r\n\r\n     /* if count % 100 = 0 THEN\r\n        COMMENT;\r\n      end if;*/\r\n\r\n    END IF;\r\n\r\n  END LOOP;\r\n  commit;\r\nEND;",
        //   start_time: "2022-08-18 18:58:23.240352",
        //   finish_time: "2022-08-18 18:58:34.794484",
        //   slow_sql_threshold: "2000000",
        //   transaction_id: "133153",
        //   thread_id: "140319868843776",
        //   session_id: "140319868843776",
        //   n_soft_parse: "999999",
        //   n_hard_parse: "5",
        //   query_plan: null,
        //   n_returned_rows: "0",
        //   n_tuples_fetched: "42",
        //   n_tuples_returned: "21",
        //   n_tuples_inserted: "1000000",
        //   n_tuples_updated: "0",
        //   n_tuples_deleted: "0",
        //   n_blocks_fetched: "1044514",
        //   n_blocks_hit: "1037100",
        //   db_time: "11554127",
        //   cpu_time: "9706384",
        //   execution_time: "0",
        //   parse_time: "27",
        //   plan_time: "110",
        //   rewrite_time: "2",
        //   pl_execution_time: "11553781",
        //   pl_compilation_time: "165",
        //   data_io_time: "50027",
        //   net_send_info: '{"time":3043206, "n_calls":1000010, "size":32005061}',
        //   net_recv_info: '{"time":0, "n_calls":0, "size":0}',
        //   net_stream_send_info: '{"time":0, "n_calls":0, "size":0}',
        //   net_stream_recv_info: '{"time":0, "n_calls":0, "size":0}',
        //   lock_count: "4007481",
        //   lock_time: "0",
        //   lock_wait_count: "0",
        //   lock_wait_time: "0",
        //   lock_max_count: "3000002",
        //   lwlock_count: "0",
        //   lwlock_wait_count: "0",
        //   lwlock_time: "0",
        //   lwlock_wait_time: "0",
        //   details: null,
        //   is_slow_sql: true,
        //   trace_id: "",
        // },
        // {
        //   node_name: "dn_6001",
        //   db_name: "postgres",
        //   schema_name: '"$user"',
        //   public: "",
        //   origin_node: "0",
        //   user_name: "coder",
        //   application_name: "Data Studio",
        //   client_addr: "171.36.30.253",
        //   client_port: "17026",
        //   unique_query_id: "1462432811",
        //   debug_query_id: "5348024557502676",
        //   query:
        //     "DECLARE\r\n  count INTEGER := 1;\r\nBEGIN LOOP\r\n    IF count > 1000000 THEN\r\n      raise info 'count is %. ', count;\r\n\r\n      EXIT;\r\n\r\n    ELSE\r\n      count :=count+1;\r\n      insert into coder.tb_test\r\n        (id, name)\r\n      values\r\n        (count, '王五');\r\n\r\n     /* if count % 100 = 0 THEN\r\n        COMMENT;\r\n      end if;*/\r\n\r\n    END IF;\r\n\r\n  END LOOP;\r\n  commit;\r\nEND;",
        //   start_time: "2022-08-18 18:58:23.240352",
        //   finish_time: "2022-08-18 18:58:34.794484",
        //   slow_sql_threshold: "2000000",
        //   transaction_id: "133153",
        //   thread_id: "140319868843776",
        //   session_id: "140319868843776",
        //   n_soft_parse: "999999",
        //   n_hard_parse: "5",
        //   query_plan: null,
        //   n_returned_rows: "0",
        //   n_tuples_fetched: "42",
        //   n_tuples_returned: "21",
        //   n_tuples_inserted: "1000000",
        //   n_tuples_updated: "0",
        //   n_tuples_deleted: "0",
        //   n_blocks_fetched: "1044514",
        //   n_blocks_hit: "1037100",
        //   db_time: "11554127",
        //   cpu_time: "9706384",
        //   execution_time: "0",
        //   parse_time: "27",
        //   plan_time: "110",
        //   rewrite_time: "2",
        //   pl_execution_time: "11553781",
        //   pl_compilation_time: "165",
        //   data_io_time: "50027",
        //   net_send_info: '{"time":3043206, "n_calls":1000010, "size":32005061}',
        //   net_recv_info: '{"time":0, "n_calls":0, "size":0}',
        //   net_stream_send_info: '{"time":0, "n_calls":0, "size":0}',
        //   net_stream_recv_info: '{"time":0, "n_calls":0, "size":0}',
        //   lock_count: "4007481",
        //   lock_time: "0",
        //   lock_wait_count: "0",
        //   lock_wait_time: "0",
        //   lock_max_count: "3000002",
        //   lwlock_count: "0",
        //   lwlock_wait_count: "0",
        //   lwlock_time: "0",
        //   lwlock_wait_time: "0",
        //   details: null,
        //   is_slow_sql: true,
        //   trace_id: "",
        // },
      ],
    };
  },
  filters: {
    timestapToDate: function (val) {
      return new Date(val).toLocaleString();
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
    getSlowSql() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          const s = new Date(this.form.beginTime);
          const e = new Date(this.form.endTime);
          if (s > e) {
            this.$message({
              type: "error",
              message: "开始时间不能大于结束时间",
            });
            return;
          }
          slowSql(this.form).then((res) => {
            console.log(res);
            if (res.code == 200) {
              this.data = JSON.parse(res.result) || [];
            }
          });
        } else {
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