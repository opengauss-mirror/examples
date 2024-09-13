
import Generator.OpenGaussGenerator;
import Lexer.OracleLexer;
import Parser.AST.ASTNode;
import Parser.OracleParser;


public class Main {
    public static void main(String[] args) {
//        String sql = "CREATE TABLE employees (\n" +
//                "    employee_id NUMBER PRIMARY KEY,\n" +
//                "    first_name VARCHAR2(20) Check(first_name > '221'),\n" +
//                "    last_name VARCHAR2(25),\n" +
//                "    email VARCHAR2(25),\n" +
//                "    hire_date DATE,\n" +
//                "    CONSTRAINT chk_example CHECK (employee_id > 0)" +
//                ");";
//        String sql = "INSERT INTO employees (first_name, last_name, email) VALUES ('Jane', 'Smith', 'janesm@example.com');";
//        String sql = "DROP TABLE employees CASCADE CONSTRAINTS;";
//        String sql = "SELECT e.first_name, d.department_name FROM employees e JOIN departments d Using e.department_id = d.department_id;";
//        String sql = "employees e JOIN departments d ON e.department_id = d.department_id;";
//        String sql = "UPDATE employees e\n" +
//                "JOIN departments d using e.department_id = d.department_id\n" +
//                "SET e.salary = e.salary * 1.10,\n" +
//                "    d.budget = d.budget * 1.10\n" +
//                "WHERE d.department_name = 'Sales';";
//        String sql = "DELETE FROM employees e\n" +
//                "WHERE e.department_id IN (\n" +
//                "    SELECT d.department_id\n" +
//                "    FROM departments d\n" +
//                "    WHERE d.department_name = 'Sales'\n" +
//                ");";
        String sql = "ALTER TABLE employees ADD email VARCHAR2(100) Check (email != '12813@163.com');";
//        String sql = "ALTER TABLE employees ADD CONSTRAINT emp_pk PRIMARY KEY (employee_id);";
        OracleLexer lexer = new OracleLexer(sql);
        lexer.printTokens();
        OracleParser parser = new OracleParser(lexer);
        ASTNode root = parser.parse();
//        ASTNode root = OracleParser.parseJoin(lexer.getTokens());
        System.out.println(root.toQueryString());
        System.out.println(root.getASTString());
        OpenGaussGenerator generator = new OpenGaussGenerator(root);
        System.out.println(generator.generate());
    }
}
