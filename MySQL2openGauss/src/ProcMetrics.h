#ifndef _PROCMETRICS_H_
#define _PROCMETRICS_H_

#include <list>
#include <map>
#include <string>
#ifdef WIN32
#include <Windows.h>
#else
#include <pthread.h>
#endif
#include "ProcOdbc.h"

namespace DMODBC {

struct BaseDBInfo {
  int procpid;
  long long proctime;
  std::string srcaddr;
  std::string dstaddr;
  std::string srcdb;
  std::string dstdb;
};

class Mutex {
 public:
  Mutex();

  virtual ~Mutex();

  void lock();

  void unlock();

 private:
#ifdef WIN32
  CRITICAL_SECTION lock_;
#else
  pthread_mutex_t lock_;
#endif
};

class Synchronized {
 public:
  Synchronized(Mutex& m);

  virtual ~Synchronized();

 private:
  Mutex* mutex_;
};

class MetricInfo {
 public:
  virtual ~MetricInfo();
  virtual std::string toSql() = 0;
};

class SqlInfo : public MetricInfo {
 public:
  SqlInfo();
  virtual ~SqlInfo();

  virtual std::string toSql();

  BaseDBInfo* baseinfo_;
  std::string name_;
  std::string type_;
  std::string sql_;
  long long starttime_;
  long long endtime_;
  bool error_;
  std::string errordetail_;

 private:
  void formatSql(const char* in, int inlen, char* out);
};

class TableInfo : public MetricInfo {
 public:
  TableInfo();
  virtual ~TableInfo();

  virtual std::string toSql();

  BaseDBInfo* baseinfo_;
  std::string table_;
  long long starttime_;
  long long endtime_;
  int rightcount_;
  int errorcount_;
  int insertrightcount_;
  int inserterrorcount_;
};

class TimeTableInfo : public MetricInfo {
 public:
  TimeTableInfo(TableInfo* tableInfo);
  TimeTableInfo(long long updatetime_, TableInfo* tableInfo);
  virtual ~TimeTableInfo();

  virtual std::string toSql();

  BaseDBInfo* baseinfo_;
  long long updatetime_;
  std::string table_;
  long long starttime_;
  long long endtime_;
  int rightcount_;
  int errorcount_;
  int insertrightcount_;
  int inserterrorcount_;
};

class DBInfo : public MetricInfo {
 public:
  DBInfo();
  virtual ~DBInfo();

  virtual std::string toSql();

  BaseDBInfo* baseinfo_;
  long long starttime_;
  long long endtime_;
  int tablecount_;
  int viewcount_;
  int procedurecount_;
  int functioncount_;
  int rightcount_;
  int errorcount_;
  int insertrightcount_;
  int inserterrorcount_;
};

class TimeDBInfo : public MetricInfo {
 public:
  TimeDBInfo(DBInfo* dbInfo);
  TimeDBInfo(long long updatetime, DBInfo* dbInfo);
  virtual ~TimeDBInfo();

  virtual std::string toSql();

  BaseDBInfo* baseinfo_;
  long long updatetime_;
  long long starttime_;
  long long endtime_;
  int tablecount_;
  int viewcount_;
  int procedurecount_;
  int functioncount_;
  int rightcount_;
  int errorcount_;
  int insertrightcount_;
  int inserterrorcount_;
};

class MetricInfoSeq {
 public:
  MetricInfoSeq(int maxsize);
  virtual ~MetricInfoSeq();
  int getSize();
  std::list<MetricInfo*> pollAllMetrics();
  bool add(MetricInfo* info);

 private:
  void destroy();

 private:
  int maxsize_;
  Mutex lock_;
  std::list<MetricInfo*> metrics_;
};

class ProcMetrics {
 public:
  ProcMetrics(const char* metricDatasource, const char* srcaddr,
              const char* dstaddr, const char* srcdb, const char* dstdb,
              int seqmaxsize, const char* dbtype);
  virtual ~ProcMetrics();

  void startDB();
  void startTable(const char* table);
  void startSql(const char* name, const char* type, const char* sql);
  void endSql(const char* name, const char* type, const char* sql, bool error,
              const char* errordetail, bool isinsert);
  void endTable(const char* table);
  void endDB();
  void addTablecount(int count);
  void addViewcount(int count);
  void addProcedurecount(int count);
  void addFunctioncount(int count);

  void saveMetricsToDb();

  static long long getCurrentTime();

 private:
  int getPid();
  std::list<MetricInfo*> clearAndGetFinishedInfos();
  bool hasFinishedInfos();

 private:
  ProcOdbc* procOdbc_;
  BaseDBInfo baseDBInfo_;
  DBInfo* startingDB_;
  TableInfo* startingTable_;
  SqlInfo* startingSql_;
  int size_;
  MetricInfoSeq* metricInfoSeq_;
  Mutex lock_;
  std::list<MetricInfo*> finishedInfos_;
  bool running_;
  bool timeDB_;
  bool timeTable_;
  pthread_t savingDatasThread_;
};

};  // namespace DMODBC

#endif
