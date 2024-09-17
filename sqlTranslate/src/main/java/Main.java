
import Generator.OpenGaussGenerator;
import Lexer.OracleLexer;
import Parser.AST.ASTNode;
import Parser.OracleParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    public static void main(String[] args) {
//        String sql = "CREATE TABLE employees (\n" +
//                "    employee_id NUMBER PRIMARY KEY,\n" +
//                "    first_name VARCHAR2(20) Check(first_name > '221'),\n" +
//                "    last_name VARCHAR2(25) Check (last_name != '12813@163.com'),\n" +
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
//        String sql = "ALTER TABLE employees ADD email VARCHAR2(100) Check (email != '12813@163.com');";
//        String sql = "CASE WHEN column2 > 0 THEN 'Positive' ELSE 'Non-positive' END";
//        String sql = "CREATE OR REPLACE VIEW emp_dept_info AS SELECT e.first_name, e.last_name, d.department_name FROM employees e JOIN departments d Using e.department_id = d.department_id;";
//        String sql = "IF v_salary >= 100000 THEN\n" +
//                "                 v_bonus := v_salary * 0.1;\n" +
//                "             ELSIF v_salary >= 50000 THEN\n" +
//                "                 v_bonus := v_salary * 0.08;\n" +
//                "             ELSIF v_salary >= 30000 THEN\n" +
//                "                 v_bonus := v_salary * 0.05;\n" +
//                "             ELSE\n" +
//                "                 v_bonus := v_salary * 0.03;\n" +
//                "             END IF;";
//        String sql = "LOOP\n" +
//                "        DBMS_OUTPUT.PUT_LINE(v_counter);\n" +
//                "        Continue;\n" +
//                "        v_counter := v_counter + 1;\n" +
//                "        EXIT WHEN v_counter > 10;\n" +
//                "    END LOOP;";
//        String sql = "EXCEPTION\n" +
//                "    WHEN e_custom_exception THEN\n" +
//                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: Custom exception raised');\n" +
//                "    WHEN ZERO_DIVIDE THEN\n" +
//                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: Division by zero');\n" +
//                "    WHEN INVALID_NUMBER THEN\n" +
//                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: Invalid number');\n" +
//                "    WHEN OTHERS THEN\n" +
//                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: ' || SQLERRM);";
        String sql = "CREATE OR REPLACE PROCEDURE update_salary (\n" +
                "             employee_id IN NUMBER,\n" +
                "             new_salary IN OUT NUMBER\n" +
                "         ) IS\n" +
                "             v_employee employees%ROWTYPE;\n" +
                "         BEGIN\n" +
                "             SELECT * INTO v_employee FROM employees WHERE employee_id = employee_id;\n" +
                "\n" +
                "             IF new_salary < 3000 THEN\n" +
                "                 new_salary := new_salary * 1.1;\n" +
                "             ELSE\n" +
                "                 new_salary := new_salary * 1.05;\n" +
                "             END IF;\n" +
                "\n" +
                "             UPDATE employees SET salary = new_salary WHERE employee_id = employee_id;\n" +
                "             COMMIT;\n" +
                "\n" +
                "             DBMS_OUTPUT.PUT_LINE('Updated Salary: ' || new_salary);\n" +
                "         EXCEPTION\n" +
                "             WHEN NO_DATA_FOUND THEN\n" +
                "                 DBMS_OUTPUT.PUT_LINE('Employee not found.');\n" +
                "             WHEN TOO_MANY_ROWS THEN\n" +
                "                 DBMS_OUTPUT.PUT_LINE('Multiple employees found.');\n" +
                "             WHEN OTHERS THEN\n" +
                "                 DBMS_OUTPUT.PUT_LINE('An error occurred: ' || SQLERRM);\n" +
                "         END update_salary;\n" +
                "         /";
        OracleLexer lexer = new OracleLexer(sql);
        lexer.printTokens();
        OracleParser parser = new OracleParser(lexer);
        ASTNode root = parser.parse();
//        ASTNode root = OracleParser.parseException(lexer.getTokens());
//        ASTNode root = OracleParser.parseLoop(lexer.getTokens());
//        ASTNode root = OracleParser.parseIFELSE(lexer.getTokens());
//        ASTNode root = OracleParser.parseCaseWhen(lexer.getTokens());
//        ASTNode root = OracleParser.parseJoin(lexer.getTokens());
        System.out.println(root.toQueryString());
        System.out.println(root.getASTString());
        OpenGaussGenerator generator = new OpenGaussGenerator(root);
        System.out.println(generator.generate());
    }
}
