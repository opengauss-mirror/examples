package org.opengauss.dao;

import org.opengauss.entity.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 JDBC 手写 SQL 的 DAO，演示 DBCP 连接池下的 CRUD / 分页 / 批量操作。
 * 所有连接均从连接池（DataSource）获取，用完即还，由 DBCP 统一管理。
 */
public class UserDao {

    private final DataSource dataSource;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 新增用户，并回填自增主键（AUTO_INCREMENT）
     */
    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO `user`(`name`,`age`) VALUES (?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setInt(2, user.getAge());
            int rows = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return rows;
        }
    }

    /**
     * 按 id 查询
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT id,name,age FROM `user` WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * 全表查询
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id,name,age FROM `user` ORDER BY id";
        List<User> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(toUser(rs));
            }
        }
        return list;
    }

    /**
     * 模糊查询（LIKE）
     */
    public List<User> findByNameLike(String keyword) throws SQLException {
        String sql = "SELECT id,name,age FROM `user` WHERE name LIKE ? ORDER BY id";
        List<User> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toUser(rs));
                }
            }
        }
        return list;
    }

    /**
     * 分页查询（LIMIT/OFFSET）
     */
    public List<User> findByPage(int limit, int offset) throws SQLException {
        String sql = "SELECT id,name,age FROM `user` ORDER BY id LIMIT ? OFFSET ?";
        List<User> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toUser(rs));
                }
            }
        }
        return list;
    }

    /**
     * 更新用户
     */
    public int update(User user) throws SQLException {
        String sql = "UPDATE `user` SET name=?, age=? WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setInt(2, user.getAge());
            ps.setInt(3, user.getId());
            return ps.executeUpdate();
        }
    }

    /**
     * 删除用户
     */
    public int delete(int id) throws SQLException {
        String sql = "DELETE FROM `user` WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * 批量插入（批量执行 + 自增主键回填）
     */
    public int[] batchInsert(List<User> users) throws SQLException {
        String sql = "INSERT INTO `user`(`name`,`age`) VALUES (?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (User user : users) {
                ps.setString(1, user.getName());
                ps.setInt(2, user.getAge());
                ps.addBatch();
            }
            int[] rows = ps.executeBatch();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int i = 0;
                while (keys.next()) {
                    if (i < users.size()) {
                        users.get(i).setId(keys.getInt(1));
                    }
                    i++;
                }
            }
            return rows;
        }
    }

    private User toUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setAge(rs.getInt("age"));
        return user;
    }
}
