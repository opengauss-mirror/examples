#ifndef _PROCODBC_H_
#define _PROCODBC_H_

#include <list>
#include <string>

#define MYSQL_TO_OPENGAUSS_HOME_DIR "MYSQL_TO_OPENGAUSS_HOME"

namespace DMODBC {

struct DBError {
  bool alreadyExistError;
  std::string error;
};

struct MysqlInfo {
  std::string host;
  int port;
  std::string username;
  std::string password;
  std::string database;
  std::string dsthost;
  int dstport;
  std::string dstdatabase;
  int loglevel;
};

class ProcOdbc {
 public:
  ProcOdbc(const char* datasource);
  virtual ~ProcOdbc();

  static struct MysqlInfo getMysqlInfo();

  bool isConnected();

  bool connect();
  void disConnect();
  bool reConnect();

  std::list<std::string> procCreateTableSql(const char* sql,
                                            const char* incrementkey);
  std::string procProcedureFunctionSql(const char* sql, bool& isfunction);

  bool executeCreateTableSql(const char* sql, DBError* dbError);
  bool executeInsertSql(const char* sql, DBError* dbError);
  bool executeCreateViewSql(const char* sql, DBError* dbError);
  bool executeGeneralSql(const char* sql, DBError* dbError);

  std::list<std::string> clearAndGetErrCreateTableSqls();
  bool executeLeftErrorSqls();
  bool hasErrorSqls();
  void printErrorSqls();
  char** getCurrentArgv();
  int getCurrentArgc();
  void setLoglevel(int loglevel);

 private:
  std::string& replaceCreateTableSql(std::string& sql);
  std::list<std::string> procCreateTableSqlLine(
      const std::string& sql, const std::string& incrementkey);
  std::string& formatByteaAtCreateTableSql(std::string& sql);
  std::string& formatSetAtCreateTableSql(std::string& sql);
  bool executeLeftErrorCreateTableSqls();
  bool hasErrorCreateTableSqls();
  void printErrorCreateTableSqls();
  void freeCurrentArgv();
  bool executeInsertSql(const char* sql, const char* replaceNullStr, DBError* dbError);
  int execSqlStably(const char* sql);
  static bool isBinaryBlobChar(unsigned char ch);

 private:
  char datasource_[256];
  bool isconn_;
  void* dbinfo_;
  char** currentArgv_;
  int currentArgc_;
  std::list<std::string> errcreatetablesqls_;
  int loglevel_;
};

};  // namespace DMODBC

#endif
