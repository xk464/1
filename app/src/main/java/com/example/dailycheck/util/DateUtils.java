package com.example.dailycheck.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * 日期工具类
 */
public final class DateUtils {

    public static final String PATTERN = "yyyy-MM-dd";
    private static final SimpleDateFormat SDF = new SimpleDateFormat(PATTERN, Locale.CHINA);

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    private DateUtils() {
    }

    /** 今天的日期字符串 */
    public static String today() {
        return SDF.format(new Date());
    }

    /** 格式化 Date 为字符串 */
    public static String format(Date date) {
        return SDF.format(date);
    }

    /** 由年月日构造日期字符串 */
    public static String from(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        return SDF.format(c.getTime());
    }

    /** 解析日期字符串 */
    public static Date parse(String dateStr) {
        try {
            return SDF.parse(dateStr);
        } catch (ParseException e) {
            return new Date();
        }
    }

    /** 获取中文显示格式 yyyy年MM月dd日 */
    public static String toChinese(String dateStr) {
        Date d = parse(dateStr);
        return new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(d);
    }

    /** 获取月份显示格式 yyyy年MM月 */
    public static String toMonthChinese(String dateStr) {
        Date d = parse(dateStr);
        return new SimpleDateFormat("yyyy年MM月", Locale.CHINA).format(d);
    }

    /** 星期几（中文） */
    public static String weekdayChinese(String dateStr) {
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        Calendar c = Calendar.getInstance();
        c.setTime(parse(dateStr));
        return weeks[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    /** 拆分日期字符串为年月日（month 为 Calendar 的 0-11） */
    public static DateParts split(String dateStr) {
        Calendar c = Calendar.getInstance();
        c.setTime(parse(dateStr));
        return new DateParts(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    /** 相对今天偏移 n 天的日期字符串 */
    public static String plusDays(String dateStr, int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(parse(dateStr));
        c.add(Calendar.DAY_OF_MONTH, n);
        return SDF.format(c.getTime());
    }

    /** 返回当月起止日期 */
    public static String[] monthRange(String anyDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(parse(anyDate));
        c.set(Calendar.DAY_OF_MONTH, 1);
        String start = SDF.format(c.getTime());
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        String end = SDF.format(c.getTime());
        return new String[]{start, end};
    }

    /** 返回当前所在周的起止日期（周一到周日） */
    public static String[] weekRange(String anyDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(parse(anyDate));
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String start = SDF.format(c.getTime());
        c.add(Calendar.DAY_OF_MONTH, 6);
        String end = SDF.format(c.getTime());
        return new String[]{start, end};
    }

    /**
     * 计算连续打卡天数。
     * 从今天往前数，若今天有记录则从今天开始；若今天没记录但昨天有，则从昨天开始（保持当前连续状态不中断）。
     */
    public static int calculateStreak(List<String> completedDateList) {
        if (completedDateList == null || completedDateList.isEmpty()) {
            return 0;
        }
        Set<String> set = new LinkedHashSet<>(completedDateList);
        String cursor = today();
        // 若今天没完成，则从昨天开始计连续（避免今天还没打卡就归零）
        if (!set.contains(cursor)) {
            cursor = plusDays(cursor, -1);
            if (!set.contains(cursor)) {
                return 0;
            }
        }
        int streak = 0;
        while (set.contains(cursor)) {
            streak++;
            cursor = plusDays(cursor, -1);
        }
        return streak;
    }

    /** 获取某月日历所需的日期网格（含前导空位与当月天数） */
    public static List<CalendarCell> buildMonthCells(int year, int month /* 0-11 */) {
        List<CalendarCell> cells = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1);
        int firstDayOfWeek = c.get(Calendar.DAY_OF_WEEK); // 1=周日
        // 转换为以周一为首日：周一=0
        int leading = (firstDayOfWeek + 5) % 7;
        for (int i = 0; i < leading; i++) {
            cells.add(new CalendarCell(null, false, false));
        }
        int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        String today = today();
        for (int day = 1; day <= daysInMonth; day++) {
            String dateStr = from(year, month, day);
            cells.add(new CalendarCell(dateStr, dateStr.equals(today), false));
        }
        // 补齐到 42 格（6 行）
        while (cells.size() < 42) {
            cells.add(new CalendarCell(null, false, false));
        }
        return cells;
    }

    /** 日期拆分结果（month 为 0-11） */
    public static class DateParts {
        public final int year, month, day;

        public DateParts(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    /** 日历单元格数据 */
    public static class CalendarCell {
        public String date;       // null 表示空位
        public boolean isToday;
        public boolean hasData;   // 当天是否有打卡数据
        public int taskTotal;     // 当天任务总数
        public int taskDone;     // 当天已完成任务数

        public CalendarCell(String date, boolean isToday, boolean hasData) {
            this.date = date;
            this.isToday = isToday;
            this.hasData = hasData;
        }
    }
}
