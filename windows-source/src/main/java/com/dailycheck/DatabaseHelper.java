package com.dailycheck;

import com.dailycheck.models.ExpenseRecord;
import com.dailycheck.models.StudyRecord;
import com.dailycheck.models.Task;
import com.dailycheck.utils.DateUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 数据库助手
 * 负责建立连接、初始化表结构，并对 tasks / study_records / expense_records 三张表提供 CRUD。
 * 表结构与 Android Room 实体保持一致：
 *   tasks(id, title, date, isCompleted, createdAt)
 *   study_records(id, subject, content, durationMinutes, date, createdAt)
 *   expense_records(id, type, category, amount, note, date, createdAt)
 *
 * 数据库文件存放于用户目录：~/.dailycheck/dailycheck.db
 */
public class DatabaseHelper {

    private static final String DB_DIR = System.getProperty("user.home") + "/.dailycheck";
    private static final String DB_PATH = DB_DIR + "/dailycheck.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static volatile DatabaseHelper INSTANCE;
    private Connection connection;

    private DatabaseHelper() {
        init();
    }

    public static synchronized DatabaseHelper get() {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseHelper();
        }
        return INSTANCE;
    }

    /** 初始化目录、连接与表结构 */
    private void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            // 建目录
            new java.io.File(DB_DIR).mkdirs();
            connection = DriverManager.getConnection(DB_URL);
            // 启用外键、调整锁
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            createTables();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("未找到 SQLite JDBC 驱动", e);
        } catch (SQLException e) {
            throw new RuntimeException("初始化数据库失败: " + DB_PATH, e);
        }
    }

    /** 创建表结构（若不存在） */
    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // tasks
            st.execute(
                    "CREATE TABLE IF NOT EXISTS tasks (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  title TEXT NOT NULL, " +
                    "  date TEXT NOT NULL, " +
                    "  isCompleted INTEGER NOT NULL DEFAULT 0, " +
                    "  createdAt INTEGER NOT NULL DEFAULT 0)");
            // study_records
            st.execute(
                    "CREATE TABLE IF NOT EXISTS study_records (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  subject TEXT NOT NULL, " +
                    "  content TEXT, " +
                    "  durationMinutes INTEGER NOT NULL DEFAULT 0, " +
                    "  date TEXT NOT NULL, " +
                    "  createdAt INTEGER NOT NULL DEFAULT 0)");
            // expense_records
            st.execute(
                    "CREATE TABLE IF NOT EXISTS expense_records (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  type INTEGER NOT NULL DEFAULT 0, " +
                    "  category TEXT NOT NULL, " +
                    "  amount REAL NOT NULL DEFAULT 0, " +
                    "  note TEXT, " +
                    "  date TEXT NOT NULL, " +
                    "  createdAt INTEGER NOT NULL DEFAULT 0)");
            // 常用索引
            st.execute("CREATE INDEX IF NOT EXISTS idx_tasks_date ON tasks(date)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_study_date ON study_records(date)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_expense_date ON expense_records(date)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_expense_type ON expense_records(type)");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // ============================================================
    //  Tasks CRUD
    // ============================================================

    /** 插入任务，返回新记录主键 */
    public long insertTask(Task task) {
        String sql = "INSERT INTO tasks(title, date, isCompleted, createdAt) VALUES(?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.title);
            ps.setString(2, task.date);
            ps.setInt(3, task.isCompleted ? 1 : 0);
            ps.setLong(4, task.createdAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    task.id = rs.getLong(1);
                    return task.id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** 删除任务 */
    public void deleteTask(long id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 切换任务完成状态 */
    public void setTaskCompleted(long id, boolean completed) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE tasks SET isCompleted = ? WHERE id = ?")) {
            ps.setInt(1, completed ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 获取指定日期的全部任务（已完成在后、按创建时间升序） */
    public List<Task> getTasksByDate(String date) {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE date = ? ORDER BY isCompleted ASC, createdAt ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTask(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取已完成任务的日期（去重，按日期降序） */
    public List<String> getCompletedDates() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT date FROM tasks WHERE isCompleted = 1 ORDER BY date DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取已存在任务的日期（去重，按日期升序） */
    public List<String> getAllTaskDates() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT date FROM tasks ORDER BY date ASC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 指定日期已完成任务数 */
    public int getCompletedCount(String date) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM tasks WHERE date = ? AND isCompleted = 1")) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 指定日期任务总数 */
    public int getTotalCount(String date) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM tasks WHERE date = ?")) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 日期区间内已完成任务数 */
    public int getCompletedCountRange(String start, String end) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM tasks WHERE date BETWEEN ? AND ? AND isCompleted = 1")) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 日期区间内任务总数 */
    public int getTotalCountRange(String start, String end) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM tasks WHERE date BETWEEN ? AND ?")) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 计算当前连续打卡天数 */
    public int getStreak() {
        List<String> dates = getCompletedDates();
        return DateUtils.calculateStreak(dates);
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        return new Task(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("date"),
                rs.getInt("isCompleted") == 1,
                rs.getLong("createdAt"));
    }

    // ============================================================
    //  StudyRecord CRUD
    // ============================================================

    /** 插入学习记录，返回新记录主键 */
    public long insertStudyRecord(StudyRecord record) {
        String sql = "INSERT INTO study_records(subject, content, durationMinutes, date, createdAt) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.subject);
            ps.setString(2, record.content);
            ps.setInt(3, record.durationMinutes);
            ps.setString(4, record.date);
            ps.setLong(5, record.createdAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.id = rs.getLong(1);
                    return record.id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** 删除学习记录 */
    public void deleteStudyRecord(long id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM study_records WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 获取指定日期的全部学习记录 */
    public List<StudyRecord> getStudyRecordsByDate(String date) {
        List<StudyRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM study_records WHERE date = ? ORDER BY createdAt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStudy(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取日期范围内的学习记录 */
    public List<StudyRecord> getStudyRecordsRange(String start, String end) {
        List<StudyRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM study_records WHERE date BETWEEN ? AND ? ORDER BY date DESC, createdAt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStudy(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 统计日期范围内的学习总时长（分钟） */
    public int getStudyTotalMinutes(String start, String end) {
        String sql = "SELECT COALESCE(SUM(durationMinutes), 0) FROM study_records WHERE date BETWEEN ? AND ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 按科目统计时长（按总时长降序） */
    public List<SubjectTotal> getStudyMinutesBySubject(String start, String end) {
        List<SubjectTotal> list = new ArrayList<>();
        String sql = "SELECT subject, COALESCE(SUM(durationMinutes), 0) AS total " +
                     "FROM study_records WHERE date BETWEEN ? AND ? GROUP BY subject ORDER BY total DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SubjectTotal t = new SubjectTotal();
                    t.subject = rs.getString("subject");
                    t.total = rs.getInt("total");
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private StudyRecord mapStudy(ResultSet rs) throws SQLException {
        return new StudyRecord(
                rs.getLong("id"),
                rs.getString("subject"),
                rs.getString("content"),
                rs.getInt("durationMinutes"),
                rs.getString("date"),
                rs.getLong("createdAt"));
    }

    /** 按科目聚合的时长结果 */
    public static class SubjectTotal {
        public String subject;
        public int total;
    }

    // ============================================================
    //  ExpenseRecord CRUD
    // ============================================================

    /** 插入收支记录，返回新记录主键 */
    public long insertExpenseRecord(ExpenseRecord record) {
        String sql = "INSERT INTO expense_records(type, category, amount, note, date, createdAt) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.type);
            ps.setString(2, record.category);
            ps.setDouble(3, record.amount);
            ps.setString(4, record.note);
            ps.setString(5, record.date);
            ps.setLong(6, record.createdAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.id = rs.getLong(1);
                    return record.id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** 删除收支记录 */
    public void deleteExpenseRecord(long id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM expense_records WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 获取指定日期的收支记录 */
    public List<ExpenseRecord> getExpenseRecordsByDate(String date) {
        List<ExpenseRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM expense_records WHERE date = ? ORDER BY createdAt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExpense(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 获取日期范围内的收支记录 */
    public List<ExpenseRecord> getExpenseRecordsRange(String start, String end) {
        List<ExpenseRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM expense_records WHERE date BETWEEN ? AND ? ORDER BY date DESC, createdAt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExpense(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 统计日期范围内支出总额 */
    public double getTotalExpense(String start, String end) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expense_records WHERE type = 0 AND date BETWEEN ? AND ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 统计日期范围内收入总额 */
    public double getTotalIncome(String start, String end) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expense_records WHERE type = 1 AND date BETWEEN ? AND ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 按分类统计支出（按金额降序） */
    public List<CategoryTotal> getExpenseByCategory(String start, String end) {
        List<CategoryTotal> list = new ArrayList<>();
        String sql = "SELECT category, COALESCE(SUM(amount), 0) AS total " +
                     "FROM expense_records WHERE type = 0 AND date BETWEEN ? AND ? " +
                     "GROUP BY category ORDER BY total DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CategoryTotal t = new CategoryTotal();
                    t.category = rs.getString("category");
                    t.total = rs.getDouble("total");
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 当日结余 = 当日收入 - 当日支出 */
    public double getDailyBalance(String date) {
        return getTotalIncome(date, date) - getTotalExpense(date, date);
    }

    private ExpenseRecord mapExpense(ResultSet rs) throws SQLException {
        return new ExpenseRecord(
                rs.getLong("id"),
                rs.getInt("type"),
                rs.getString("category"),
                rs.getDouble("amount"),
                rs.getString("note"),
                rs.getString("date"),
                rs.getLong("createdAt"));
    }

    /** 按分类聚合的金额结果 */
    public static class CategoryTotal {
        public String category;
        public double total;
    }

    /** 关闭连接 */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // 忽略
            }
        }
    }
}
