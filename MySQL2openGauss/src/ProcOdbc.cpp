#include <sqlext.h>
#include <stdio.h>
#include <stdlib.h>
#ifdef WIN32
#include <windows.h>
#endif
#include <string.h>
#include <unistd.h>

#include <fstream>
#include <iostream>
#include <list>
#include <map>

#include "ProcOdbc.h"

#define SQLK_CREATE_TABLE "CREATE TABLE "
#define SQLK_UNIQUE_KEY "UNIQUE KEY "
#define SQLK_PRIMARY_KEY "PRIMARY KEY "
#define SQLK_FOREIGN_KEY "FOREIGN KEY "
#define SQLK_KEY "KEY "
#define SQLK_COMMENT " COMMENT '"
#define SQLK_BYTEA_START " bytea("
#define SQLK_BYTEA_END ")"
#define SQLK_BYTEA " bytea"
#define SQLK_SET_START " set("
#define SQLK_SET_END ")"
#define SQLK_TEXT " text"
#define SQLK_PROCEDURE "PROCEDURE"
#define SQLK_PROCEDURE_DEF_END ")"
#define SQLK_FUNCTION "FUNCTION"
#define SQLK_FUNCTION_DEF_END "\n"
#define SQLK_INSERT_INTO "\nINSERT INTO \""
#define SQLK_DOUBLE_BRACKET " double("
#define SQLK_DOUBLE_BRACKET_END ")"

#define DATA_NOT_SEND_ERROR "could not send data"
#define NO_CONNECTION_ERROR "no connection"
#define NULL_STRING_ERROR "violates not-null constraint"

#define TABLE_ALREADY_EXIST_ERROR "\" already exists;"

#define INFO_LOG_LEVEL 3
#define WARN_LOG_LEVEL 4
#define ERROR_LOG_LEVEL 5

namespace DMODBC {

struct ReplaceKV {
  std::string key;
  std::string value;
};

struct DBInfo {
  SQLHENV V_OD_Env;     // Handle ODBC environment
  SQLHSTMT V_OD_hstmt;  // Handle statement
  SQLHDBC V_OD_hdbc;    // Handle connection
  char sqlattr[100];
  SQLINTEGER V_OD_erg;
  SQLINTEGER V_OD_buffer;
  SQLINTEGER V_OD_err;
  SQLINTEGER V_OD_id;
};

static void printDBError(SQLSMALLINT type, SQLHDBC hdbc,
                         struct DBError *dbError) {
  char status[16];
  int buflen = 1024;
  char *msg = (char *)malloc(buflen);
  msg[0] = '\0';
  SQLSMALLINT sqlmsglen;
  SQLINTEGER sqlerr;
  SQLGetDiagRec(type, hdbc, 1, (SQLCHAR *)status, &sqlerr, (SQLCHAR *)msg,
                buflen - 1, &sqlmsglen);
  if (buflen < sqlmsglen) {
    free(msg);
    buflen = sqlmsglen;
    msg = (char *)malloc(buflen);
    msg[0] = '\0';
    SQLGetDiagRec(type, hdbc, 1, (SQLCHAR *)status, &sqlerr, (SQLCHAR *)msg,
                  buflen - 1, &sqlmsglen);
  }
  printf("%s: %s\n", status, msg);
  if (dbError != NULL) {
    dbError->error = status;
    dbError->error += ": ";
    dbError->error += msg;
    if (strstr(msg, TABLE_ALREADY_EXIST_ERROR)) {
      dbError->alreadyExistError = true;
    } else {
      dbError->alreadyExistError = false;
    }
  }
  free(msg);
}

static bool isConnectionError(SQLSMALLINT type, SQLHDBC hdbc) {
  bool ret = false;
  char status[16];
  int buflen = 1024;
  char *msg = (char *)malloc(buflen);
  msg[0] = '\0';
  SQLSMALLINT sqlmsglen;
  SQLINTEGER sqlerr;
  SQLGetDiagRec(type, hdbc, 1, (SQLCHAR *)status, &sqlerr, (SQLCHAR *)msg,
                buflen - 1, &sqlmsglen);
  if (buflen < sqlmsglen) {
    free(msg);
    buflen = sqlmsglen;
    msg = (char *)malloc(buflen);
    msg[0] = '\0';
    SQLGetDiagRec(type, hdbc, 1, (SQLCHAR *)status, &sqlerr, (SQLCHAR *)msg,
                  buflen - 1, &sqlmsglen);
  }
  if (strstr(msg, DATA_NOT_SEND_ERROR) || strstr(msg, NO_CONNECTION_ERROR)) {
    ret = true;
  }
  free(msg);
  return ret;
}

static bool isNullStringError(SQLSMALLINT type, SQLHDBC hdbc) {
  bool ret = false;
  char status[16];
  int buflen = 1024;
  char *msg = (char *)malloc(buflen);
  msg[0] = '\0';
  SQLSMALLINT sqlmsglen;
  SQLINTEGER sqlerr;
  SQLGetDiagRec(type, hdbc, 1, (SQLCHAR *)status, &sqlerr, (SQLCHAR *)msg,
                buflen - 1, &sqlmsglen);
  if (buflen < sqlmsglen) {
    free(msg);
    buflen = sqlmsglen;
    msg = (char *)malloc(buflen);
    msg[0] = '\0';
    SQLGetDiagRec(type, hdbc, 1, (SQLCHAR *)status, &sqlerr, (SQLCHAR *)msg,
                  buflen - 1, &sqlmsglen);
  }
  if (strstr(msg, NULL_STRING_ERROR)) {
    ret = true;
  }
  free(msg);
  return ret;
}

static void replace(std::string &src, const char *orgstr, const char *newstr) {
  std::string::size_type pos = src.find(orgstr);
  while (pos != std::string::npos) {
    src.replace(pos, strlen(orgstr), newstr);

    pos = src.find(orgstr);
  }
}

static bool replaceonce(std::string &src, const char *orgstr,
                        const char *newstr) {
  bool ret = false;
  std::string::size_type pos = src.find(orgstr);
  if (pos != std::string::npos) {
    src.replace(pos, strlen(orgstr), newstr);
    ret = true;
  }
  return ret;
}

static std::string trim(const std::string &s) {
  static const char *whiteSpace = " \t\r\n";

  if (s.empty()) {
    return s;
  }

  std::string::size_type b = s.find_first_not_of(whiteSpace);
  if (std::string::npos == b) {
    return "";
  }

  std::string::size_type e = s.find_last_not_of(whiteSpace);

  return std::string(s, b, e - b + 1);
}

static int getLine(std::istream &in, std::string &strLine) {
  strLine = "";
  int count = 0;
  bool lineFlag = false;
  char ch;

  do {
    ch = in.get();
    if (!in.good()) {
      if (count > 0) {
        return count;
      } else {
        return -1;
      }
    }

    switch (ch) {
      case '\r':
        break;
      case '\n':
        lineFlag = true;
        break;
      default:
        strLine += ch;
        count++;
        break;
    }
  } while (!lineFlag);

  return count;
}

static void loadKvList(std::istream &in, std::list<ReplaceKV> &replaceKvList) {
  std::string fullLine, command;
  std::string leftSide, rightSide;
  std::string::size_type length;

  while (getLine(in, fullLine) != -1) {
    length = fullLine.find('#');
    if (std::string::npos == length) {
      command = fullLine;
    } else if (length > 0) {
      command = fullLine.substr(0, length);
    } else {
      continue;
    }

    command = trim(command);

    length = command.find('=');
    if (std::string::npos != length) {
      if ((length + 1) != command.size()) {
        leftSide = trim(command.substr(0, length));
        rightSide = trim(command.substr(length + 1));
      } else {
        leftSide = trim(command.substr(0, length));
        rightSide = "";
      }
    } else {
      continue;
    }

    ReplaceKV replaceKv;
    replaceKv.key = leftSide;
    replaceKv.value = rightSide;
    replaceKvList.push_back(replaceKv);
  }
}

static void loadKvList(const char *kvListFile,
                       std::list<ReplaceKV> &replaceKvList) {
  std::ifstream inputStream;
  inputStream.open(kvListFile);
  if (!inputStream.fail()) {
    loadKvList(inputStream, replaceKvList);
    inputStream.close();
  } else {
    printf("cannot open kv file %s\n", kvListFile);
  }
}

static void loadKvMap(std::istream &in,
                      std::map<std::string, std::string> &replaceKvMap) {
  std::string fullLine, command;
  std::string leftSide, rightSide;
  std::string::size_type length;

  while (getLine(in, fullLine) != -1) {
    length = fullLine.find('#');
    if (std::string::npos == length) {
      command = fullLine;
    } else if (length > 0) {
      command = fullLine.substr(0, length);
    } else {
      continue;
    }

    command = trim(command);

    length = command.find('=');
    if (std::string::npos != length) {
      if ((length + 1) != command.size()) {
        leftSide = trim(command.substr(0, length));
        rightSide = trim(command.substr(length + 1, command.size() - length));
      } else {
        leftSide = trim(command.substr(0, length));
      }
    } else {
      continue;
    }

    replaceKvMap[leftSide] = rightSide;
  }
}

static void loadKvMap(const char *kvListFile,
                      std::map<std::string, std::string> &replaceKvMap) {
  std::ifstream inputStream;
  inputStream.open(kvListFile);
  if (!inputStream.fail()) {
    loadKvMap(inputStream, replaceKvMap);
    inputStream.close();
  } else {
    printf("cannot open kv file %s\n", kvListFile);
  }
}

static std::list<ReplaceKV> getCreateTableReplaceKV() {
  std::list<ReplaceKV> ret;
  std::string replaceKvFile;
  const char *homedir = getenv(MYSQL_TO_OPENGAUSS_HOME_DIR);
  if (!homedir) {
    return ret;
  }

  replaceKvFile = homedir;
  replaceKvFile += "/conf/createtablerkv.prop";
  loadKvList(replaceKvFile.c_str(), ret);
  return ret;
}

static std::list<ReplaceKV> getCreateFunctionReplaceKV() {
  std::list<ReplaceKV> ret;
  std::string replaceKvFile;
  const char *homedir = getenv(MYSQL_TO_OPENGAUSS_HOME_DIR);
  if (!homedir) {
    return ret;
  }

  replaceKvFile = homedir;
  replaceKvFile += "/conf/createfunctionrkv.prop";
  loadKvList(replaceKvFile.c_str(), ret);
  return ret;
}

static std::list<ReplaceKV> getCreateFunctionTypeKV() {
  std::list<ReplaceKV> ret;
  std::string replaceKvFile;
  const char *homedir = getenv(MYSQL_TO_OPENGAUSS_HOME_DIR);
  if (!homedir) {
    return ret;
  }

  replaceKvFile = homedir;
  replaceKvFile += "/conf/createfunctiontkv.prop";
  loadKvList(replaceKvFile.c_str(), ret);
  return ret;
}

ProcOdbc::ProcOdbc(const char *datasource)
    : isconn_(false), loglevel_(INFO_LOG_LEVEL) {
  strcpy(datasource_, datasource);
  dbinfo_ = new DBInfo;

  currentArgc_ = 4;
  currentArgv_ = (char **)malloc(sizeof(char *) * currentArgc_);
  currentArgv_[0] = (char *)malloc(256);
  strcpy(currentArgv_[0], "mysqltoopengauss");
  currentArgv_[1] = (char *)malloc(256);
  strcpy(currentArgv_[1], "--compatible=postgresql");
  currentArgv_[2] = (char *)malloc(256);
  strcpy(currentArgv_[2], "--default-character-set=utf8");
  currentArgv_[3] = (char *)malloc(256);
  currentArgv_[3][0] = '\0';
}

ProcOdbc::~ProcOdbc() {
  disConnect();
  delete (DBInfo *)dbinfo_;
  freeCurrentArgv();
}

struct MysqlInfo ProcOdbc::getMysqlInfo() {
  std::string confFile;
  std::map<std::string, std::string> confMap;
  struct MysqlInfo ret;
  const char *homedir = getenv(MYSQL_TO_OPENGAUSS_HOME_DIR);
  if (!homedir) {
    return ret;
  }

  confFile = homedir;
  confFile += "/conf/mysql2opengauss.prop";
  loadKvMap(confFile.c_str(), confMap);
  ret.host = confMap["mysql_host"];
  ret.port = atoi(confMap["mysql_port"].c_str());
  ret.username = confMap["mysql_username"];
  ret.password = confMap["mysql_password"];
  ret.database = confMap["mysql_database"];
  ret.loglevel = atoi(confMap["loglevel"].c_str());
  ret.dsthost = confMap["opengauss_host"];
  ret.dstport = atoi(confMap["opengauss_port"].c_str());
  ret.dstdatabase = confMap["opengauss_database"];
  printf("from MySQL %s:%d database %s to openGauss %s:%d database %s\n",
         confMap["mysql_host"].c_str(), atoi(confMap["mysql_port"].c_str()),
         confMap["mysql_database"].c_str(), confMap["opengauss_host"].c_str(),
         atoi(confMap["opengauss_port"].c_str()),
         confMap["opengauss_database"].c_str());

  return ret;
}

bool ProcOdbc::isConnected() { return isconn_; }

bool ProcOdbc::connect() {
  if (isconn_) {
    // 已经连接
    return isconn_;
  }

  DBInfo *dbinfo = (DBInfo *)dbinfo_;
  // 1. 申请环境句柄
  dbinfo->V_OD_erg =
      SQLAllocHandle(SQL_HANDLE_ENV, SQL_NULL_HANDLE, &dbinfo->V_OD_Env);
  if ((dbinfo->V_OD_erg != SQL_SUCCESS) &&
      (dbinfo->V_OD_erg != SQL_SUCCESS_WITH_INFO)) {
    printf("Error AllocHandle SQL_HANDLE_ENV\n");
    return isconn_;
  }
  // 2. 设置环境属性（版本信息）
  SQLSetEnvAttr(dbinfo->V_OD_Env, SQL_ATTR_ODBC_VERSION, (void *)SQL_OV_ODBC3,
                0);
  // 3. 申请连接句柄
  dbinfo->V_OD_erg =
      SQLAllocHandle(SQL_HANDLE_DBC, dbinfo->V_OD_Env, &dbinfo->V_OD_hdbc);
  if ((dbinfo->V_OD_erg != SQL_SUCCESS) &&
      (dbinfo->V_OD_erg != SQL_SUCCESS_WITH_INFO)) {
    SQLFreeHandle(SQL_HANDLE_ENV, dbinfo->V_OD_Env);
    printf("Error AllocHandle SQL_HANDLE_DBC\n");
    printDBError(SQL_HANDLE_ENV, dbinfo->V_OD_Env, NULL);
    return isconn_;
  }
  // 4. 设置连接属性
  SQLSetConnectAttr(dbinfo->V_OD_hdbc, SQL_ATTR_AUTOCOMMIT,
                    (SQLPOINTER)SQL_AUTOCOMMIT_ON, 0);
  // 5.
  // odbc.ini文件中已经配置了用户名密码，那么这里可以留空（""）；但是不建议这么做，因为一旦odbc.ini权限管理不善，将导致数据库用户密码泄露。
  printf("datasource: %s\n", datasource_);
  dbinfo->V_OD_erg =
      SQLConnect(dbinfo->V_OD_hdbc, (SQLCHAR *)datasource_, SQL_NTS,
                 (SQLCHAR *)"", SQL_NTS, (SQLCHAR *)"", SQL_NTS);
  if ((dbinfo->V_OD_erg != SQL_SUCCESS) &&
      (dbinfo->V_OD_erg != SQL_SUCCESS_WITH_INFO)) {
    printf("Error SQLConnect %d\n", (int)(dbinfo->V_OD_erg));
    printDBError(SQL_HANDLE_DBC, dbinfo->V_OD_hdbc, NULL);
    SQLFreeHandle(SQL_HANDLE_ENV, dbinfo->V_OD_Env);
    return isconn_;
  }
  printf("Connected !\n");

  // 6. 设置语句属性
  SQLSetStmtAttr(dbinfo->V_OD_hstmt, SQL_ATTR_QUERY_TIMEOUT, (SQLPOINTER *)3,
                 0);
  // 7. 申请语句句柄
  SQLAllocHandle(SQL_HANDLE_STMT, dbinfo->V_OD_hdbc, &dbinfo->V_OD_hstmt);

  isconn_ = true;

  return isconn_;
}

void ProcOdbc::disConnect() {
  if (!isconn_) {
    // 没有连接，直接返回
    return;
  }
  isconn_ = false;

  DBInfo *dbinfo = (DBInfo *)dbinfo_;

  // 16. 断开数据源连接并释放句柄资源
  SQLFreeHandle(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt);
  SQLDisconnect(dbinfo->V_OD_hdbc);
  SQLFreeHandle(SQL_HANDLE_DBC, dbinfo->V_OD_hdbc);
  SQLFreeHandle(SQL_HANDLE_ENV, dbinfo->V_OD_Env);
}

bool ProcOdbc::reConnect() {
  disConnect();
  return connect();
}

std::list<std::string> ProcOdbc::procCreateTableSql(const char *sql,
                                                    const char *incrementkey) {
  std::string retsql = sql;
  std::string ik;
  if (incrementkey != NULL) {
    ik = incrementkey;
  }
  replaceCreateTableSql(retsql);
  return procCreateTableSqlLine(retsql, ik);
}

std::string ProcOdbc::procProcedureFunctionSql(const char *sql,
                                               bool &isfunction) {
  static std::list<ReplaceKV> funrkv;
  static std::list<ReplaceKV> funtkv;
  std::string ret;
  std::string sqlstr = sql;
  replace(sqlstr, "`", "\"");
  replace(sqlstr, "\r", "");
  std::string::size_type pos = sqlstr.find(SQLK_PROCEDURE);
  std::string::size_type pos2 = sqlstr.find(SQLK_PROCEDURE_DEF_END, pos + 1);
  if ((pos != std::string::npos) && (pos2 != std::string::npos)) {
    isfunction = false;
    ret = "CREATE ";
    ret += sqlstr.substr(pos, pos2 - pos + 1);
    ret += " IS ";
    ret += sqlstr.substr(pos2 + 1);
  } else {
    isfunction = true;
    if (funrkv.empty()) {
      funrkv = getCreateFunctionReplaceKV();
      funtkv = getCreateFunctionTypeKV();
    }

    // 替换一般信息，只换一次
    for (std::list<ReplaceKV>::iterator iter = funrkv.begin();
         iter != funrkv.end(); iter++) {
      if (replaceonce(sqlstr, iter->key.c_str(), iter->value.c_str())) {
        break;
      }
    }

    // 替换类型，只换一次
    for (std::list<ReplaceKV>::iterator iter = funtkv.begin();
         iter != funtkv.end(); iter++) {
      if (replaceonce(sqlstr, iter->key.c_str(), iter->value.c_str())) {
        break;
      }
    }

    pos = sqlstr.find(SQLK_FUNCTION);
    pos2 = sqlstr.find(SQLK_FUNCTION_DEF_END, pos + 1);
    if ((pos != std::string::npos) && (pos2 != std::string::npos)) {
      ret = "CREATE ";
      ret += sqlstr.substr(pos, pos2 - pos + 1);
      ret += " AS $$ ";
      ret += sqlstr.substr(pos2 + 1);
      ret += "; $$ LANGUAGE plpgsql";
    }
  }
  return ret;
}

std::string &ProcOdbc::replaceCreateTableSql(std::string &sql) {
  static std::list<ReplaceKV> replaceKVs;
  if (replaceKVs.empty()) {
    replaceKVs = getCreateTableReplaceKV();
  }
  for (std::list<ReplaceKV>::iterator iter = replaceKVs.begin();
       iter != replaceKVs.end(); iter++) {
    replace(sql, iter->key.c_str(), iter->value.c_str());
  }
  formatByteaAtCreateTableSql(sql);
  formatSetAtCreateTableSql(sql);
  return sql;
}

std::list<std::string> ProcOdbc::procCreateTableSqlLine(
    const std::string &sql, const std::string &incrementkey) {
  std::list<std::string> ret;
  std::string table;
  std::string outputsql;
  std::string tmp;
  std::string tmp2;
  std::string::size_type tmppos;
  std::string::size_type tmppos2;
  std::string::size_type tmppos3;
  std::string::size_type tmppos4;
  std::string::size_type pos = 0;
  std::string::size_type nextpos;
  bool iskeyline;
  int createTableLen = strlen(SQLK_CREATE_TABLE);
  int uniqueKeyLen = strlen(SQLK_UNIQUE_KEY);
  int keyLen = strlen(SQLK_KEY);
  int commentLen = strlen(SQLK_COMMENT);
  int doubleBracketLen = strlen(SQLK_DOUBLE_BRACKET);
  nextpos = sql.find('\n', pos);
  while (nextpos != std::string::npos) {
    iskeyline = false;
    tmp = sql.substr(pos, nextpos - pos);

    // CREATE TABLE
    tmppos = tmp.find(SQLK_CREATE_TABLE);
    if (tmppos != std::string::npos) {
      tmppos2 = tmp.find('(');
      if (tmppos2 != std::string::npos) {
        table = tmp.substr(tmppos + createTableLen,
                           tmppos2 - tmppos - createTableLen - 1);
      }
      iskeyline = true;
    } else {
      // double bracket
      tmppos = tmp.find(SQLK_DOUBLE_BRACKET);
      if (tmppos != std::string::npos) {
        tmppos2 = tmp.find(SQLK_DOUBLE_BRACKET_END, tmppos + doubleBracketLen);
        if (tmppos2 != std::string::npos) {
          tmp2 = tmp.substr(0, tmppos);
          tmp2 += " double precision ";
          tmp2 += tmp.substr(tmppos2 + strlen(SQLK_DOUBLE_BRACKET_END));
          tmp = tmp2;
        }
      }

      // COMMENT
      tmppos = tmp.find(SQLK_COMMENT);
      if (tmppos != std::string::npos) {
        tmppos2 = tmp.find('\'', tmppos + commentLen);
        tmppos3 = tmp.find('"');
        tmppos4 = tmp.find('"', tmppos3 + 2);
        if ((tmppos2 != std::string::npos) && (tmppos3 != std::string::npos) &&
            (tmppos4 != std::string::npos)) {
          tmp2 = "COMMENT ON COLUMN ";
          tmp2 += table + ".";
          tmp2 += tmp.substr(tmppos3, tmppos4 - tmppos3 + 1);
          tmp2 += " IS ";
          tmp2 += tmp.substr(tmppos + commentLen - 1,
                             tmppos2 + 2 - tmppos - commentLen);
          ret.push_back(tmp2);
        }
        tmp = tmp.substr(0, tmppos) + ",";
      } else {
        // UNIQUE KEY
        tmppos = tmp.find(SQLK_UNIQUE_KEY);
        if (tmppos != std::string::npos) {
          tmppos2 = tmp.find('(');
          if (tmppos2 != std::string::npos) {
            tmp2 = "  CONSTRAINT";
            tmp2 += " \"";
            tmp2 += table.substr(1, table.size() - 2);
            tmp2 += "_";
            tmp2 += tmp.substr(tmppos + uniqueKeyLen + 1,
                               tmppos2 - tmppos - uniqueKeyLen - 1);
            tmp2 += "UNIQUE ";
            tmp2 += tmp.substr(tmppos2);
            tmp = tmp2;
          }
          iskeyline = true;
        } else {
          // PRIMARY KEY
          tmppos = tmp.find(SQLK_PRIMARY_KEY);
          if (tmppos != std::string::npos) {
            // PRIMARY KEY不需要处理
            iskeyline = true;
          } else {
            // FOREIGN KEY
            tmppos = tmp.find(SQLK_FOREIGN_KEY);
            if (tmppos != std::string::npos) {
              // FOREIGN KEY不需要处理
              iskeyline = true;
            } else {
              // KEY
              tmppos = tmp.find(SQLK_KEY);
              if (tmppos != std::string::npos) {
                tmppos2 = tmp.find('(');
                if (tmppos2 != std::string::npos) {
                  tmppos3 = tmp.find(')', tmppos2);
                  if (tmppos3 != std::string::npos) {
                    tmp2 = "CREATE INDEX";
                    tmp2 += " \"";
                    tmp2 += table.substr(1, table.size() - 2);
                    tmp2 += "_";
                    tmp2 += tmp.substr(tmppos + keyLen + 1,
                                       tmppos2 - tmppos - keyLen - 2);
                    tmp2 += " ON ";
                    tmp2 += table;
                    tmp2 += tmp.substr(tmppos2, tmppos3 - tmppos2 + 1);
                    ret.push_back(tmp2);
                    tmp = "";
                  }
                }
                iskeyline = true;
              }
            }
          }
        }
      }
    }

    if (!tmp.empty()) {
      if (!iskeyline && !incrementkey.empty()) {
        if (tmp.find(incrementkey) != std::string::npos) {
          // 自增主键
          tmppos = tmp.rfind(",");
          if (tmppos != std::string::npos) {
            tmp = tmp.substr(0, tmppos) + " default nextval('seq_" +
                  table.substr(1, table.size() - 2) + "_" +
                  incrementkey.substr(1, incrementkey.size() - 2) + "'),";
          } else {
            tmp = tmp.substr(0, tmppos) + " default nextval('seq_" +
                  table.substr(1, table.size() - 2) + "_" +
                  incrementkey.substr(1, incrementkey.size() - 2) + "')";
          }
        }
      }
      outputsql += tmp + "\n";
    }

    pos = nextpos + 1;
    nextpos = sql.find('\n', pos);
  }
  if (outputsql.at(outputsql.size() - 2) == ',') {
    outputsql = outputsql.substr(0, outputsql.size() - 2) + "\n";
  }
  outputsql += sql.substr(pos);
  ret.push_front(outputsql);

  if (!incrementkey.empty()) {
    // 如果有自增主键，增加自增主键创建语句
    outputsql = "create SEQUENCE seq_";
    outputsql += table.substr(1, table.size() - 2) + "_" +
                 incrementkey.substr(1, incrementkey.size() - 2);
    outputsql += " cache 100";
    ret.push_front(outputsql);

    outputsql = "alter SEQUENCE seq_";
    outputsql += table.substr(1, table.size() - 2) + "_" +
                 incrementkey.substr(1, incrementkey.size() - 2);
    outputsql += " OWNED by ";
    outputsql += table;
    outputsql += ".";
    outputsql += incrementkey;
    ret.push_back(outputsql);
  }
  return ret;
}

std::string &ProcOdbc::formatByteaAtCreateTableSql(std::string &sql) {
  std::string::size_type pos1 = sql.find(SQLK_BYTEA_START);
  std::string::size_type pos2;
  while (pos1 != std::string::npos) {
    pos2 = sql.find(SQLK_BYTEA_END, pos1 + 1);
    if (pos2 == std::string::npos) {
      break;
    }
    sql.replace(pos1, pos2 - pos1 + strlen(SQLK_BYTEA_END), SQLK_BYTEA);
    pos1 = sql.find(SQLK_BYTEA_START);
  }
  return sql;
}

std::string &ProcOdbc::formatSetAtCreateTableSql(std::string &sql) {
  std::string::size_type pos1 = sql.find(SQLK_SET_START);
  std::string::size_type pos2;
  while (pos1 != std::string::npos) {
    pos2 = sql.find(SQLK_SET_END, pos1 + 1);
    if (pos2 == std::string::npos) {
      break;
    }
    sql.replace(pos1, pos2 - pos1 + strlen(SQLK_SET_END), SQLK_TEXT);
    pos1 = sql.find(SQLK_SET_START);
  }
  return sql;
}

char **ProcOdbc::getCurrentArgv() { return currentArgv_; }

int ProcOdbc::getCurrentArgc() { return currentArgc_ - 1; }

void ProcOdbc::setLoglevel(int loglevel) { loglevel_ = loglevel; };

bool ProcOdbc::executeCreateTableSql(const char *sql, DBError *dbError) {
  bool ret = true;
  DBInfo *dbinfo = (DBInfo *)dbinfo_;

  // 直接执行SQL语句。
  if (loglevel_ <= INFO_LOG_LEVEL) {
    printf("start: execute sql %s\n", sql);
  }
  int execret = execSqlStably(sql);
  if ((execret != SQL_SUCCESS) && (execret != SQL_SUCCESS_WITH_INFO)) {
    printf("finished error(%d): execute sql %s\n", (int)execret, sql);
    printDBError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt, dbError);
    ret = false;

    // if (dbError) {
    // if (!dbError->alreadyExistError) {
    //   errcreatetablesqls_.push_back(sql);
    // }
    //}
  } else {
    if (loglevel_ <= INFO_LOG_LEVEL) {
      printf("finished successfully: execute sql %s\n", sql);
    }
  }
  return ret;
}

bool ProcOdbc::executeInsertSql(const char *sql, DBError *dbError) {
  bool ret = true;
  int insertIntoLen = strlen(SQLK_INSERT_INTO);
  std::string sqlstr = sql;
  std::string insertSql;

  // 拆分成多条sql语句
  std::string::size_type pos = 0;
  std::string::size_type pos2 = sqlstr.find(SQLK_INSERT_INTO, insertIntoLen);
  while (true) {
    if (pos2 == std::string::npos) {
      insertSql = sqlstr.substr(pos);
      ret = executeInsertSql(insertSql.c_str(), "' '", dbError);
      break;
    } else {
      insertSql = sqlstr.substr(pos, pos2 - pos);
      ret = executeInsertSql(insertSql.c_str(), "' '", dbError);
    }
    pos = pos2 + 1;
    pos2 = sqlstr.find(SQLK_INSERT_INTO, pos + insertIntoLen);
  }
  return ret;
}

bool ProcOdbc::executeInsertSql(const char *sql, const char *replaceNullStr,
                                DBError *dbError) {
  bool ret = true;
  std::string sqlstr;
  DBInfo *dbinfo = (DBInfo *)dbinfo_;

  // 直接执行SQL语句。
  if (loglevel_ <= INFO_LOG_LEVEL) {
    printf("start: execute sql %s\n", sql);
  }
  int execret = execSqlStably(sql);
  if ((execret != SQL_SUCCESS) && (execret != SQL_SUCCESS_WITH_INFO)) {
    if (isNullStringError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt)) {
      // 非空字符串约束，把''改成replaceNullStr，并重试一次
      printDBError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt, dbError);
      printf("replace '' by %s and retry\n", replaceNullStr);

      sqlstr = sql;
      replace(sqlstr, "''", replaceNullStr);
      execret = execSqlStably(sqlstr.c_str());
    }
  }

  if ((execret != SQL_SUCCESS) && (execret != SQL_SUCCESS_WITH_INFO)) {
    printf("finished error(%d): execute sql %s\n", (int)execret, sql);
    printDBError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt, dbError);
    ret = false;
  } else {
    if (loglevel_ <= INFO_LOG_LEVEL) {
      printf("finished successfully: execute sql %s\n", sql);
    }
  }
  return ret;
}

bool ProcOdbc::executeCreateViewSql(const char *sql, DBError *dbError) {
  bool ret = true;
  DBInfo *dbinfo = (DBInfo *)dbinfo_;

  // 直接执行SQL语句。
  if (loglevel_ <= INFO_LOG_LEVEL) {
    printf("start: execute sql %s\n", sql);
  }
  int execret = execSqlStably(sql);
  if ((execret != SQL_SUCCESS) && (execret != SQL_SUCCESS_WITH_INFO)) {
    printf("finished error(%d): execute sql %s\n", (int)execret, sql);
    printDBError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt, dbError);
    ret = false;
  } else {
    if (loglevel_ <= INFO_LOG_LEVEL) {
      printf("finished successfully: execute sql %s\n", sql);
    }
  }
  return ret;
}

bool ProcOdbc::executeGeneralSql(const char *sql, DBError *dbError) {
  bool ret = true;
  DBInfo *dbinfo = (DBInfo *)dbinfo_;

  // 直接执行SQL语句。
  if (loglevel_ <= INFO_LOG_LEVEL) {
    printf("start: execute sql %s\n", sql);
  }
  int execret = execSqlStably(sql);
  if ((execret != SQL_SUCCESS) && (execret != SQL_SUCCESS_WITH_INFO)) {
    printf("finished error(%d): execute sql %s\n", (int)execret, sql);
    printDBError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt, dbError);
    ret = false;
  } else {
    if (loglevel_ <= INFO_LOG_LEVEL) {
      printf("finished successfully: execute sql %s\n", sql);
    }
  }
  return ret;
}

int ProcOdbc::execSqlStably(const char *sql) {
  int retry = 30;
  DBInfo *dbinfo = (DBInfo *)dbinfo_;
  SQLINTEGER execret;
  while (retry-- > 0) {
    execret = SQLExecDirect(dbinfo->V_OD_hstmt, (SQLCHAR *)sql, SQL_NTS);
    if ((execret != SQL_SUCCESS) && (execret != SQL_SUCCESS_WITH_INFO)) {
      if (isConnectionError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt)) {
        // 连接错误，重新连接，并重试
        printDBError(SQL_HANDLE_STMT, dbinfo->V_OD_hstmt, NULL);
        if (retry > 0) {
          printf("reconnect and retry(%d)...", retry);
          sleep(2);
        }
        reConnect();
      } else {
        // 其他错误，退出循环
        break;
      }
    } else {
      // 执行成功，退出循环
      break;
    }
  }
  return (int)execret;
}

bool ProcOdbc::isBinaryBlobChar(unsigned char ch) {
  return (ch <= 31) || (ch >= 127) /*&& (ch <= 255)*/ || (ch == 39) ||
         (ch == 92);
}

std::list<std::string> ProcOdbc::clearAndGetErrCreateTableSqls() {
  std::list<std::string> tmp;
  std::list<std::string> ret = errcreatetablesqls_;
  errcreatetablesqls_ = tmp;
  return ret;
}

bool ProcOdbc::executeLeftErrorSqls() {
  bool ret = true;
  if (!executeLeftErrorCreateTableSqls()) {
    ret = false;
  }

  return ret;
}

bool ProcOdbc::executeLeftErrorCreateTableSqls() {
  std::list<std::string> sqls = clearAndGetErrCreateTableSqls();
  int retry = 1000;
  struct DBError dbError;
  while (sqls.size() > 0) {
    // 有错误sql时不断执行
    printf("retry...\n");
    for (std::list<std::string>::iterator iter = sqls.begin();
         iter != sqls.end(); iter++) {
      printf("retry sql %s\n", iter->c_str());
      executeCreateTableSql(iter->c_str(), &dbError);
    }

    if (--retry == 0) {
      if (hasErrorCreateTableSqls()) {
        // 还仍然有错误
        printErrorCreateTableSqls();
      }
      break;
    }

    sqls = clearAndGetErrCreateTableSqls();
  }
  return !hasErrorCreateTableSqls();
}

bool ProcOdbc::hasErrorSqls() { return hasErrorCreateTableSqls(); }

bool ProcOdbc::hasErrorCreateTableSqls() {
  return errcreatetablesqls_.size() > 0;
}

void ProcOdbc::printErrorSqls() { printErrorCreateTableSqls(); }

void ProcOdbc::printErrorCreateTableSqls() {
  if (errcreatetablesqls_.size() > 0) {
    printf("error sql count: %d\n", (int)errcreatetablesqls_.size());
    for (std::list<std::string>::iterator iter = errcreatetablesqls_.begin();
         iter != errcreatetablesqls_.end(); iter++) {
      printf("      %s\n", iter->c_str());
    }
  }
}

void ProcOdbc::freeCurrentArgv() {
  for (int index = 0; index < currentArgc_; index++) {
    free(currentArgv_[index]);
  }
  free(currentArgv_);
}

};  // namespace DMODBC
