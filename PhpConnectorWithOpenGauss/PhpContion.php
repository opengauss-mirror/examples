<?php

class PhpContion
{
    protected $sql_select = "";
    protected $sql_where = "";
    protected $sql_order = "";
    public $conn = "";

    public function __construct($host, $user, $pass, $dbname, $port = '8888')
    {
        $c_host = sprintf("host=%s", $host);
        $c_port = sprintf("port=%s", $port);
        $c_dbname = sprintf("dbname=%s", $dbname);
        $c_credentials = sprintf("user=%s password=%s", $user, $pass);
        $conn = pg_connect("$c_host $c_port $c_dbname $c_credentials");
        $this->conn = $conn;
        if (!$conn) {
            echo "数据库连接失败";
            return false;
        } else {
            return $conn;
        }
    }

    public function __destruct()
    {
        pg_close($this->conn);
    }

    function Select(...$args)
    {
        $sql = "select ";
        for ($i = 0; $i < count($args); $i++) {
            if ($i < count($args) - 1) {
                $sql = $sql . $args[$i] . ",";
                continue;
            }
            $sql = $sql . $args[$i] . " ";
        }
        $this->sql_select = $sql;
    }

    function Where($query, $value = "")
    {
        if (gettype($value) == 'integer') {
            $sql = str_replace("?", $value, $query);
        } else {
            $sql = str_replace("?", "'" . $value . "'", $query);
        }
        if ($this->sql_where == "") {
            $this->sql_where = "where " . $sql;
            return;
        }
        $this->sql_where = $this->sql_where . " and " . $sql;
    }

    function Find($table)
    {
        if ($this->sql_select == '' or $this->sql_where == '') {
            exit("error:not use function Select or function Where");
        }
        $sql = $this->sql_select . "from " . $table . " " . $this->sql_where;
        if ($this->sql_order != "") {
            $sql .= $this->sql_order;
        }
        $data = $this->getAll($sql);
        $this->sql_select = '';
        $this->sql_where = '';
        return $data;
    }

    function First($table)
    {
        if ($this->sql_select == '' or $this->sql_where == '') {
            exit("error:not use function Select or function Where");
        }
        $sql = $this->sql_select . "from " . $table . " " . $this->sql_where . " limit 1";
        $data = $this->getOne($sql);
        $this->sql_select = '';
        $this->sql_where = '';
        return $data;
    }

    function Order($value)
    {
        $this->sql_order = " order by " . $value;
    }

    function Delete($table)
    {
        if ($this->sql_where == '') {
            exit("error:not use function Where");
        }
        $sql = 'delete from ' . $table . ' ' . $this->sql_where;
        $this->sql_where = "";
        $res = $this->query($sql);
        if ($res) {
            return true;
        } else {
            return false;
        }

    }

    function Create($table, $data)
    {
        $sql = "INSERT INTO " . $table . " (" . implode(",", array_keys($data)) . ") VALUES ('" . implode("','", $data) . "')";
        $this->query($sql);
    }

    function Update($table, $data)
    {
        if ($this->sql_where == '') {
            exit("error:not use function Where");
        }
        $sql = "UPDATE " . $table . " SET ";
        foreach ($data as $key => $value) {
            $sql .= "{$key} ='{$value}',";
        }
        $sql = rtrim($sql, ',');
        $sql = $sql . $this->sql_where;
        $this->sql_where = "";
        $res = $this->query($sql);
        if ($res) {
            return true;
        } else {
            return false;
        }
    }

    function getOne($sql)
    {
        $result = $this->query($sql);
        $data = array();
        if ($result) {
            $data = pg_fetch_assoc($result);
        }
        return $data;
    }

    function getAll($sql)
    {

        $result = $this->query($sql);
        $data = array();
        if ($result) {
            try {
                while ($row = pg_fetch_assoc($result)) {
                    $data[] = $row;
                }
                return $data;
            } catch (Exception $e) {
                echo $e->getMessage();
            }

        }
        return "";
    }

    function query($sql)
    {
        return pg_query($this->conn, $sql);
    }

    function numRows($result)
    {
        return pg_num_rows($result);
    }

    function affactedRows($result)
    {
        return pg_affected_rows($result);
    }

    function connReset()
    {
        return pg_connection_reset($this->conn);
    }

    function dbName()
    {
        return pg_dbname($this->conn);
    }

    function fetch_all($result)
    {
        return pg_fetch_all($result);
    }

    function fetch_array($result)
    {
        return pg_fetch_array($result);
    }

    function fetch_assos($result)
    {
        return pg_fetch_assoc($result);
    }
}