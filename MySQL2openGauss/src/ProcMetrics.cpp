#include <stdio.h>
#include <stdlib.h>
#ifdef WIN32
#include <sys/timeb.h>
#else
#include <sys/time.h>
#endif
#include <string.h>
#include <unistd.h>

#include <fstream>
#include <iostream>
#include <list>
#include <map>

#include "ProcMetrics.h"

#define OPENGAUSS_DBTYPE "opengauss"
#define MYSQL_DBTYPE "mysql"
#define OPENGAUSS_DB 1
#define MYSQL_DB 2

#define INSERT_KEY "),("
#define INSERT_KEY_LEN 3

namespace DMODBC {

static int sg_dbtype;

static int countInsertSql(const char* sql) {
  int ret = 1;
  sql = strstr(sql, INSERT_KEY);
  while (sql) {
    ret++;
    sql = strstr(sql + INSERT_KEY_LEN, INSERT_KEY);
  }
  return ret;
}

Synchronized::Synchronized(Mutex& m) : mutex_(&m) { mutex_->lock(); }

Synchronized::~Synchronized() { mutex_->unlock(); }

Mutex::Mutex() {
#ifdef WIN32
  InitializeCriticalSection(&lock_);
#else
  pthread_mutex_init(&lock_, NULL);
#endif
}

Mutex::~Mutex() {
#ifdef WIN32
  DeleteCriticalSection(&lock_);
#else
  pthread_mutex_destroy(&lock_);
#endif
}

void Mutex::lock() {
#ifdef WIN32
  EnterCriticalSection(&lock_);
#else
  pthread_mutex_lock(&lock_);
#endif
}

void Mutex::unlock() {
#ifdef WIN32
  LeaveCriticalSection(&lock_);
#else
  pthread_mutex_unlock(&lock_);
#endif
}

MetricInfo::~MetricInfo() {}

SqlInfo::SqlInfo()
    : baseinfo_(NULL), starttime_(0), endtime_(0), error_(false) {}

SqlInfo::~SqlInfo() {}

std::string SqlInfo::toSql() {
  std::string ret;
  if (error_) {
    char* tmp;
    int len = 1024;
    if (sql_.size() > errordetail_.size()) {
      if (len < (int)sql_.size() * 2) {
        len = (int)sql_.size() * 2;
      }
    } else {
      if (len < (int)errordetail_.size() * 2) {
        len = (int)errordetail_.size() * 2;
      }
    }
    tmp = (char*)malloc(len);
    switch (sg_dbtype) {
      case OPENGAUSS_DB:
        ret =
            "insert into sql_info_metrics(procpid, proctime, name, type, "
            "srcaddr, "
            "dstaddr, srcdb, "
            "dstdb, starttime, endtime, consumetime, iserror, errorinfo, "
            "sqldata) "
            "values(";
        sprintf(tmp,
                "%d, TO_TIMESTAMP(%d), '%s', '%s', '%s', '%s', '%s', '%s', "
                "TO_TIMESTAMP(%d), "
                "TO_TIMESTAMP(%d), %d, %d, '",
                baseinfo_->procpid, (int)(baseinfo_->proctime / 1000),
                name_.c_str(), type_.c_str(), baseinfo_->srcaddr.c_str(),
                baseinfo_->dstaddr.c_str(), baseinfo_->srcdb.c_str(),
                baseinfo_->dstdb.c_str(), (int)(starttime_ / 1000),
                (int)(endtime_ / 1000), (int)(endtime_ - starttime_),
                error_ ? 1 : 0);
        break;
      case MYSQL_DB:
        ret =
            "insert into sql_info_metrics(procpid, proctime, name, type, "
            "srcaddr, "
            "dstaddr, srcdb, "
            "dstdb, starttime, endtime, consumetime, iserror, errorinfo, "
            "sqldata) "
            "values(";
        sprintf(tmp,
                "%d, FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), '%s', '%s', "
                "'%s', "
                "'%s', '%s', '%s', "
                "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), "
                "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, %d, '",
                baseinfo_->procpid, (int)(baseinfo_->proctime / 1000),
                name_.c_str(), type_.c_str(), baseinfo_->srcaddr.c_str(),
                baseinfo_->dstaddr.c_str(), baseinfo_->srcdb.c_str(),
                baseinfo_->dstdb.c_str(), (int)(starttime_ / 1000),
                (int)(endtime_ / 1000), (int)(endtime_ - starttime_),
                error_ ? 1 : 0);
        break;
      default:
        // 不支持其他类型
        return "";
    }
    ret += tmp;
    formatSql(errordetail_.c_str(), errordetail_.size(), tmp);
    ret += tmp;
    ret += "', '";
    formatSql(sql_.c_str(), sql_.size(), tmp);
    ret += tmp;
    ret += "')";
    free(tmp);
  }
  return ret;
}

void SqlInfo::formatSql(const char* in, int inlen, char* out) {
  int outindex = 0;
  for (int index = 0; index < inlen; index++) {
    if (in[index] == '\'') {
      out[outindex] = '\'';
      out[outindex + 1] = '\'';
      outindex += 2;
    } else {
      out[outindex] = in[index];
      outindex++;
    }
  }
  out[outindex] = '\0';
}

TableInfo::TableInfo()
    : baseinfo_(NULL),
      starttime_(0),
      endtime_(0),
      rightcount_(0),
      errorcount_(0),
      insertrightcount_(0),
      inserterrorcount_(0) {}

TableInfo::~TableInfo() {}

std::string TableInfo::toSql() {
  std::string ret;
  char tmp[1024];
  switch (sg_dbtype) {
    case OPENGAUSS_DB:
      sprintf(tmp,
              "%d, TO_TIMESTAMP(%d), '%s', '%s', '%s', '%s', '%s', "
              "TO_TIMESTAMP(%d), "
              "TO_TIMESTAMP(%d), %d, %d, %d, %d, %d, %d",
              baseinfo_->procpid, (int)(baseinfo_->proctime / 1000),
              table_.c_str(), baseinfo_->srcaddr.c_str(),
              baseinfo_->dstaddr.c_str(), baseinfo_->srcdb.c_str(),
              baseinfo_->dstdb.c_str(), (int)(starttime_ / 1000),
              (int)(endtime_ / 1000), (int)(endtime_ - starttime_),
              (rightcount_ + errorcount_), rightcount_, errorcount_,
              insertrightcount_, inserterrorcount_);
      break;
    case MYSQL_DB:
      sprintf(
          tmp,
          "%d, FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), '%s', '%s', '%s', "
          "'%s', '%s', "
          "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), "
          "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, %d, %d, %d, %d, "
          "%d",
          baseinfo_->procpid, (int)(baseinfo_->proctime / 1000), table_.c_str(),
          baseinfo_->srcaddr.c_str(), baseinfo_->dstaddr.c_str(),
          baseinfo_->srcdb.c_str(), baseinfo_->dstdb.c_str(),
          (int)(starttime_ / 1000), (int)(endtime_ / 1000),
          (int)(endtime_ - starttime_), (rightcount_ + errorcount_),
          rightcount_, errorcount_, insertrightcount_, inserterrorcount_);
      break;
    default:
      // 不支持其他类型
      return "";
  }
  ret =
      "insert into table_info_metrics(procpid, proctime, tablename, srcaddr, "
      "dstaddr, srcdb, "
      "dstdb, starttime, endtime, consumetime, totalcount, rightcount, "
      "errorcount, insertrightcount, inserterrorcount) values(";

  ret += tmp;
  ret += ")";
  return ret;
}

TimeTableInfo::TimeTableInfo(long long updatetime, TableInfo* tableInfo) {
  baseinfo_ = tableInfo->baseinfo_;
  updatetime_ = updatetime;
  table_ = tableInfo->table_;
  starttime_ = tableInfo->starttime_;
  endtime_ = tableInfo->endtime_;
  rightcount_ = tableInfo->rightcount_;
  errorcount_ = tableInfo->errorcount_;
  insertrightcount_ = tableInfo->insertrightcount_;
  inserterrorcount_ = tableInfo->inserterrorcount_;
}

TimeTableInfo::TimeTableInfo(TableInfo* tableInfo) {
  baseinfo_ = tableInfo->baseinfo_;
  updatetime_ = ProcMetrics::getCurrentTime();
  table_ = tableInfo->table_;
  starttime_ = tableInfo->starttime_;
  endtime_ = tableInfo->endtime_;
  rightcount_ = tableInfo->rightcount_;
  errorcount_ = tableInfo->errorcount_;
  insertrightcount_ = tableInfo->insertrightcount_;
  inserterrorcount_ = tableInfo->inserterrorcount_;
}

TimeTableInfo::~TimeTableInfo() {}

std::string TimeTableInfo::toSql() {
  std::string ret;
  char tmp[1024];
  switch (sg_dbtype) {
    case OPENGAUSS_DB:
      sprintf(tmp,
              "TO_TIMESTAMP(%d), %d, TO_TIMESTAMP(%d), '%s', '%s', '%s', '%s', "
              "'%s', "
              "TO_TIMESTAMP(%d), "
              "TO_TIMESTAMP(%d), %d, %d, %d, %d, %d, %d",
              (int)(updatetime_ / 1000), baseinfo_->procpid,
              (int)(baseinfo_->proctime / 1000), table_.c_str(),
              baseinfo_->srcaddr.c_str(), baseinfo_->dstaddr.c_str(),
              baseinfo_->srcdb.c_str(), baseinfo_->dstdb.c_str(),
              (int)(starttime_ / 1000), (int)(endtime_ / 1000),
              (int)(updatetime_ - starttime_), (rightcount_ + errorcount_),
              rightcount_, errorcount_, insertrightcount_, inserterrorcount_);
      break;
    case MYSQL_DB:
      sprintf(tmp,
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, "
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), '%s', '%s', '%s', "
              "'%s', '%s', "
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), "
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, %d, %d, %d, "
              "%d, %d",
              (int)(updatetime_ / 1000), baseinfo_->procpid,
              (int)(baseinfo_->proctime / 1000), table_.c_str(),
              baseinfo_->srcaddr.c_str(), baseinfo_->dstaddr.c_str(),
              baseinfo_->srcdb.c_str(), baseinfo_->dstdb.c_str(),
              (int)(starttime_ / 1000), (int)(endtime_ / 1000),
              (int)(updatetime_ - starttime_), (rightcount_ + errorcount_),
              rightcount_, errorcount_, insertrightcount_, inserterrorcount_);
      break;
    default:
      // 不支持其他类型
      return "";
  }
  ret =
      "insert into time_table_info_metrics(updatetime, procpid, proctime, "
      "tablename, "
      "srcaddr, "
      "dstaddr, srcdb, "
      "dstdb, starttime, endtime, consumetime, totalcount, rightcount, "
      "errorcount, insertrightcount, inserterrorcount) values(";
  ret += tmp;
  ret += ")";
  return ret;
}

DBInfo::DBInfo()
    : baseinfo_(NULL),
      starttime_(0),
      endtime_(0),
      tablecount_(0),
      viewcount_(0),
      procedurecount_(0),
      functioncount_(0),
      rightcount_(0),
      errorcount_(0),
      insertrightcount_(0),
      inserterrorcount_(0) {}

DBInfo::~DBInfo() {}

std::string DBInfo::toSql() {
  std::string ret;
  char tmp[1024];
  switch (sg_dbtype) {
    case OPENGAUSS_DB:
      sprintf(tmp,
              "%d, TO_TIMESTAMP(%d), '%s', '%s', '%s', '%s', TO_TIMESTAMP(%d), "
              "TO_TIMESTAMP(%d), %d, %d, %d, %d, %d, %d, %d, %d, %d, %d",
              baseinfo_->procpid, (int)(baseinfo_->proctime / 1000),
              baseinfo_->srcaddr.c_str(), baseinfo_->dstaddr.c_str(),
              baseinfo_->srcdb.c_str(), baseinfo_->dstdb.c_str(),
              (int)(starttime_ / 1000), (int)(endtime_ / 1000),
              (int)(endtime_ - starttime_), tablecount_, viewcount_,
              procedurecount_, functioncount_, (rightcount_ + errorcount_),
              rightcount_, errorcount_, insertrightcount_, inserterrorcount_);
      break;
    case MYSQL_DB:
      sprintf(
          tmp,
          "%d, FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), '%s', '%s', '%s', "
          "'%s', FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), "
          "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, %d, %d, %d, %d, "
          "%d, %d, %d, %d, %d",
          baseinfo_->procpid, (int)(baseinfo_->proctime / 1000),
          baseinfo_->srcaddr.c_str(), baseinfo_->dstaddr.c_str(),
          baseinfo_->srcdb.c_str(), baseinfo_->dstdb.c_str(),
          (int)(starttime_ / 1000), (int)(endtime_ / 1000),
          (int)(endtime_ - starttime_), tablecount_, viewcount_,
          procedurecount_, functioncount_, (rightcount_ + errorcount_),
          rightcount_, errorcount_, insertrightcount_, inserterrorcount_);
      break;
    default:
      // 不支持其他类型
      return "";
  }
  ret =
      "insert into db_info_metrics(procpid, proctime, srcaddr, dstaddr, srcdb, "
      "dstdb, starttime, endtime, consumetime, tablecount, viewcount, "
      "procedurecount, functioncount, totalcount, rightcount, "
      "errorcount, insertrightcount, inserterrorcount) values(";
  ret += tmp;
  ret += ")";
  return ret;
}

TimeDBInfo::TimeDBInfo(DBInfo* dbInfo) {
  baseinfo_ = dbInfo->baseinfo_;
  updatetime_ = ProcMetrics::getCurrentTime();
  starttime_ = dbInfo->starttime_;
  endtime_ = dbInfo->endtime_;
  tablecount_ = dbInfo->tablecount_;
  viewcount_ = dbInfo->viewcount_;
  procedurecount_ = dbInfo->procedurecount_;
  functioncount_ = dbInfo->functioncount_;
  rightcount_ = dbInfo->rightcount_;
  errorcount_ = dbInfo->errorcount_;
  insertrightcount_ = dbInfo->insertrightcount_;
  inserterrorcount_ = dbInfo->inserterrorcount_;
}

TimeDBInfo::TimeDBInfo(long long updatetime, DBInfo* dbInfo) {
  baseinfo_ = dbInfo->baseinfo_;
  updatetime_ = updatetime;
  starttime_ = dbInfo->starttime_;
  endtime_ = dbInfo->endtime_;
  tablecount_ = dbInfo->tablecount_;
  viewcount_ = dbInfo->viewcount_;
  procedurecount_ = dbInfo->procedurecount_;
  functioncount_ = dbInfo->functioncount_;
  rightcount_ = dbInfo->rightcount_;
  errorcount_ = dbInfo->errorcount_;
  insertrightcount_ = dbInfo->insertrightcount_;
  inserterrorcount_ = dbInfo->inserterrorcount_;
}

TimeDBInfo::~TimeDBInfo() {}

std::string TimeDBInfo::toSql() {
  std::string ret;
  char tmp[1024];
  switch (sg_dbtype) {
    case OPENGAUSS_DB:
      sprintf(tmp,
              "TO_TIMESTAMP(%d), %d, TO_TIMESTAMP(%d), '%s', '%s', '%s', '%s', "
              "TO_TIMESTAMP(%d), "
              "TO_TIMESTAMP(%d), %d, %d, %d, %d, %d, %d, %d, %d, %d, %d",
              (int)(updatetime_ / 1000), baseinfo_->procpid,
              (int)(baseinfo_->proctime / 1000), baseinfo_->srcaddr.c_str(),
              baseinfo_->dstaddr.c_str(), baseinfo_->srcdb.c_str(),
              baseinfo_->dstdb.c_str(), (int)(starttime_ / 1000),
              (int)(endtime_ / 1000), (int)(updatetime_ - starttime_),
              tablecount_, viewcount_, procedurecount_, functioncount_,
              (rightcount_ + errorcount_), rightcount_, errorcount_,
              insertrightcount_, inserterrorcount_);
      break;
    case MYSQL_DB:
      sprintf(tmp,
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, "
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), '%s', '%s', '%s', "
              "'%s', FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), "
              "FROM_UNIXTIME(%d, '%%Y-%%m-%%d %%H-%%i-%%s'), %d, %d, %d, %d, "
              "%d, %d, %d, %d, %d, %d",
              (int)(updatetime_ / 1000), baseinfo_->procpid,
              (int)(baseinfo_->proctime / 1000), baseinfo_->srcaddr.c_str(),
              baseinfo_->dstaddr.c_str(), baseinfo_->srcdb.c_str(),
              baseinfo_->dstdb.c_str(), (int)(starttime_ / 1000),
              (int)(endtime_ / 1000), (int)(updatetime_ - starttime_),
              tablecount_, viewcount_, procedurecount_, functioncount_,
              (rightcount_ + errorcount_), rightcount_, errorcount_,
              insertrightcount_, inserterrorcount_);
      break;
    default:
      // 不支持其他类型
      return "";
  }
  ret =
      "insert into time_db_info_metrics(updatetime, procpid, proctime, "
      "srcaddr, dstaddr, srcdb, "
      "dstdb, starttime, endtime, consumetime, tablecount, viewcount, "
      "procedurecount, functioncount, totalcount, rightcount, "
      "errorcount, insertrightcount, inserterrorcount) values(";
  ret += tmp;
  ret += ")";
  return ret;
}

MetricInfoSeq::MetricInfoSeq(int maxsize) : maxsize_(maxsize) {}

MetricInfoSeq::~MetricInfoSeq() { destroy(); }

int MetricInfoSeq::getSize() {
  Synchronized sync(lock_);
  return metrics_.size();
}

std::list<MetricInfo*> MetricInfoSeq::pollAllMetrics() {
  std::list<MetricInfo*> ret;
  Synchronized sync(lock_);
  ret = metrics_;
  metrics_ = std::list<MetricInfo*>();
  return ret;
}

bool MetricInfoSeq::add(MetricInfo* info) {
  bool ret = true;
  Synchronized sync(lock_);
  if ((maxsize_ <= 0) || ((int)(metrics_.size()) < maxsize_)) {
    metrics_.push_back(info);
  } else {
    // 达到最大数组容量
    ret = false;
  }
  return ret;
}

void MetricInfoSeq::destroy() {
  Synchronized sync(lock_);
  for (std::list<MetricInfo*>::iterator iter = metrics_.begin();
       iter != metrics_.end(); iter++) {
    delete (*iter);
  }
  metrics_.clear();
}

static void* ProcThread(void* arg) {
  ProcMetrics* procMetrics = (ProcMetrics*)arg;
  procMetrics->saveMetricsToDb();
  return NULL;
}

ProcMetrics::ProcMetrics(const char* metricDatasource, const char* srcaddr,
                         const char* dstaddr, const char* srcdb,
                         const char* dstdb, int seqmaxsize, const char* dbtype)
    : startingDB_(NULL), running_(true), timeDB_(false), timeTable_(false) {
  baseDBInfo_.procpid = getPid();
  baseDBInfo_.proctime = getCurrentTime();
  baseDBInfo_.srcaddr = srcaddr;
  baseDBInfo_.dstaddr = dstaddr;
  baseDBInfo_.srcdb = srcdb;
  baseDBInfo_.dstdb = dstdb;
  metricInfoSeq_ = new MetricInfoSeq(seqmaxsize);

  procOdbc_ = new DMODBC::ProcOdbc(metricDatasource);
  procOdbc_->setLoglevel(5);
  if (strcmp(OPENGAUSS_DBTYPE, dbtype) == 0) {
    sg_dbtype = OPENGAUSS_DB;
  } else if (strcmp(MYSQL_DBTYPE, dbtype) == 0) {
    sg_dbtype = MYSQL_DB;
  } else {
    printf("unsupport db type: %s\n", dbtype);
  }
  if (!procOdbc_->connect()) {
    printf("cannot connect to the datasource %s\n", metricDatasource);
  } else {
    pthread_create(&savingDatasThread_, NULL, ProcThread, this);
  }
}

ProcMetrics::~ProcMetrics() {
  while (hasFinishedInfos()) {
    sleep(1);
  }
  running_ = false;
  pthread_join(savingDatasThread_, NULL);
  if (startingDB_) {
    delete startingDB_;
    startingDB_ = NULL;
  }

  if (metricInfoSeq_) {
    delete metricInfoSeq_;
    metricInfoSeq_ = NULL;
  }

  if (procOdbc_) {
    procOdbc_->disConnect();
    delete procOdbc_;
    procOdbc_ = NULL;
  }
}

void ProcMetrics::startDB() {
  if (startingDB_) {
    delete startingDB_;
  }
  startingDB_ = new DBInfo();
  startingDB_->baseinfo_ = &baseDBInfo_;
  startingDB_->starttime_ = getCurrentTime();
}

void ProcMetrics::startTable(const char* table) {
  startingTable_ = new TableInfo();
  startingTable_->baseinfo_ = &baseDBInfo_;
  startingTable_->table_ = table;
  startingTable_->starttime_ = getCurrentTime();
}

void ProcMetrics::startSql(const char* name, const char* type,
                           const char* sql) {
  startingSql_ = new SqlInfo();
  startingSql_->baseinfo_ = &baseDBInfo_;
  startingSql_->name_ = name;
  startingSql_->type_ = type;
  startingSql_->sql_ = sql;
  startingSql_->starttime_ = getCurrentTime();
}

void ProcMetrics::endSql(const char* name, const char* type, const char* sql,
                         bool error, const char* errordetail, bool isinsert) {
  if (startingSql_) {
    startingSql_->error_ = error;
    startingSql_->errordetail_ = errordetail;
    startingSql_->endtime_ = getCurrentTime();
    {
      Synchronized sync(lock_);
      finishedInfos_.push_back(startingSql_);
    }
    startingSql_ = NULL;
  }

  int insertcount = 0;
  if (isinsert) {
    insertcount = countInsertSql(sql);
  }

  if (startingTable_) {
    if (error) {
      startingTable_->errorcount_++;
      if (insertcount) {
        startingTable_->inserterrorcount_ += insertcount;
      }
    } else {
      startingTable_->rightcount_++;
      if (insertcount) {
        startingTable_->insertrightcount_ += insertcount;
      }
    }

    if (timeTable_) {
      {
        Synchronized sync(lock_);
        finishedInfos_.push_back(new TimeTableInfo(startingTable_));
      }
      timeTable_ = false;
    }
  }

  if (startingDB_) {
    if (error) {
      startingDB_->errorcount_++;
      if (insertcount) {
        startingDB_->inserterrorcount_ += insertcount;
      }
    } else {
      startingDB_->rightcount_++;
      if (insertcount) {
        startingDB_->insertrightcount_ += insertcount;
      }
    }

    if (timeDB_) {
      {
        Synchronized sync(lock_);
        finishedInfos_.push_back(new TimeDBInfo(startingDB_));
      }
      timeDB_ = false;
    }
  }
}

void ProcMetrics::endTable(const char* table) {
  if (startingTable_) {
    startingTable_->endtime_ = getCurrentTime();
    {
      Synchronized sync(lock_);
      finishedInfos_.push_back(startingTable_);
      finishedInfos_.push_back(
          new TimeTableInfo(startingTable_->endtime_, startingTable_));
    }
    startingTable_ = NULL;
  }
}

void ProcMetrics::endDB() {
  if (startingDB_) {
    startingDB_->endtime_ = getCurrentTime();
    {
      Synchronized sync(lock_);
      finishedInfos_.push_back(startingDB_);
      finishedInfos_.push_back(
          new TimeDBInfo(startingDB_->endtime_, startingDB_));
    }
    startingDB_ = NULL;
  }
}

void ProcMetrics::addTablecount(int count) {
  if (startingDB_) {
    startingDB_->tablecount_ += count;
  }
}

void ProcMetrics::addViewcount(int count) {
  if (startingDB_) {
    startingDB_->viewcount_ += count;
  }
}

void ProcMetrics::addProcedurecount(int count) {
  if (startingDB_) {
    startingDB_->procedurecount_ += count;
  }
}

void ProcMetrics::addFunctioncount(int count) {
  if (startingDB_) {
    startingDB_->functioncount_ += count;
  }
}

int ProcMetrics::getPid() {
#ifdef WIN32
  return (int)GetCurrentProcessId();
#else
  return (int)getpid();
#endif
}

long long ProcMetrics::getCurrentTime() {
#ifdef WIN32
  timeb timebuffer;
  ftime(&timebuffer);
  return (long long)(timebuffer.time * 1000ULL + timebuffer.millitm);
#else
  timeval t;
  gettimeofday(&t, 0);
  return (long long)(t.tv_sec * 1000ULL + t.tv_usec / 1000);
#endif
}

std::list<MetricInfo*> ProcMetrics::clearAndGetFinishedInfos() {
  std::list<MetricInfo*> ret;
  Synchronized sync(lock_);
  ret = finishedInfos_;
  finishedInfos_ = std::list<MetricInfo*>();
  return ret;
}

bool ProcMetrics::hasFinishedInfos() {
  bool ret = true;
  Synchronized sync(lock_);
  if (finishedInfos_.empty()) {
    ret = false;
  }
  return ret;
}

void ProcMetrics::saveMetricsToDb() {
  std::string str;
  while (running_) {
    timeDB_ = true;
    timeTable_ = true;
    std::list<MetricInfo*> metricInfos = clearAndGetFinishedInfos();
    for (std::list<MetricInfo*>::iterator iter = metricInfos.begin();
         iter != metricInfos.end(); iter++) {
      str = (*iter)->toSql();
      if (!str.empty()) {
        procOdbc_->executeGeneralSql(str.c_str(), NULL);
      }
      delete *iter;
    }
    sleep(2);
  }
}

};  // namespace DMODBC
