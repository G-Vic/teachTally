package com.teachingtally.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_CSV = 1001;
    private static final int BLUE = Color.rgb(30, 96, 175);
    private static final int BLUE_SOFT = Color.rgb(232, 241, 255);
    private static final int TEAL = Color.rgb(22, 128, 112);
    private static final int TEAL_SOFT = Color.rgb(226, 247, 243);
    private static final int SURFACE = Color.rgb(247, 249, 252);
    private static final int CARD = Color.WHITE;
    private static final int LINE = Color.rgb(221, 226, 234);
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(107, 114, 128);
    private static final int DANGER = Color.rgb(217, 48, 37);
    private static final int DANGER_SOFT = Color.rgb(252, 232, 230);
    private static final int DISABLED = Color.rgb(189, 193, 198);
    private static final int CORNER_RADIUS_DP = 10;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    private Db db;
    private LinearLayout root;
    private String pendingExportCsv;
    private String searchText = "";
    private String screen = "home";
    private Student currentStudent;
    private String statsStart;
    private String statsEnd;
    private String statsLabel = "本月";
    private String statsCalendarMonth;
    private String detailCalendarMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new Db(this);
        DateRange month = monthRange();
        statsStart = month.start;
        statsEnd = month.end;
        statsCalendarMonth = month.start;
        showHome();
    }

    private void setScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        getWindow().setStatusBarColor(SURFACE);
        getWindow().setNavigationBarColor(SURFACE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(SURFACE);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    private void showHome() {
        screen = "home";
        currentStudent = null;
        setScreen();

        LinearLayout header = row();
        header.addView(text("课时统计", 24, INK, true), weightParams());
        Button add = actionButton("添加学生");
        add.setOnClickListener(v -> showStudentForm(null));
        header.addView(add);
        root.addView(header);

        root.addView(navBar(true));

        EditText search = input("搜索学生");
        search.setSingleLine(true);
        search.setText(searchText);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString().trim();
                renderStudentList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(search, matchWrap());

        Stats summary = db.stats();
        root.addView(homeSummary(summary));

        Button export = secondaryButton("导出 CSV 备份");
        export.setOnClickListener(v -> confirmExportCsv());
        root.addView(export, matchWrap());

        LinearLayout list = new LinearLayout(this);
        list.setId(1001);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, matchWrap());
        renderStudentList();
    }

    private LinearLayout navBar(boolean homeSelected) {
        LinearLayout nav = row();
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        nav.setBackground(rounded(CARD, cornerRadius(), LINE, 1));
        Button students = homeSelected ? actionButton("学生") : secondaryButton("学生");
        students.setOnClickListener(v -> showHome());
        nav.addView(students, weightParams());
        Button stat = homeSelected ? secondaryButton("统计") : actionButton("统计");
        stat.setOnClickListener(v -> showStats());
        nav.addView(stat, weightParams());
        return nav;
    }

    private void renderStudentList() {
        LinearLayout list = findViewById(1001);
        if (list == null) return;
        list.removeAllViews();
        List<Student> students = db.students(searchText);
        if (students.isEmpty()) {
            TextView empty = text("暂无学生，点“添加”开始记录。", 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            list.addView(empty, matchWrap());
            return;
        }
        for (int i = 0; i < students.size(); i++) {
            list.addView(studentCard(students.get(i), i, students.size()));
        }
    }

    private View studentCard(Student s, int index, int count) {
        LinearLayout card = card();
        LinearLayout top = row();
        LinearLayout names = column();
        names.addView(text(s.name, 16, INK, true));
        names.addView(text(s.lastDate == null ? "还未上课" : "最近 " + s.lastDate, 12, MUTED, false));
        top.addView(names, weightParams());

        TextView remain = text(s.remaining() + "/" + s.totalLessons + " 节", 14,
                s.remaining() <= 3 ? DANGER : BLUE, true);
        remain.setGravity(Gravity.RIGHT);
        top.addView(remain);
        card.addView(top);

        LinearLayout metrics = row();
        metrics.setPadding(0, dp(10), 0, dp(4));
        metrics.addView(metric("总课时", s.totalLessons + " 节", INK), weightParams());
        metrics.addView(metric("已上", s.usedLessons + " 节", BLUE), weightParams());
        metrics.addView(metric("剩余", s.remaining() + " 节", s.remaining() <= 3 ? DANGER : BLUE), weightParams());
        card.addView(metrics);

        LinearLayout bottom = row();
        bottom.setPadding(0, dp(10), 0, 0);
        Button detail = secondaryButton("详情");
        detail.setOnClickListener(v -> showDetail(s.id));
        bottom.addView(detail, weightParams());

        Button lesson = s.remaining() > 0 ? actionButton("记一节") : disabledButton("已满");
        lesson.setEnabled(s.remaining() > 0);
        lesson.setOnClickListener(v -> recordLesson(s.id, true));
        bottom.addView(lesson, weightParams());
        card.addView(bottom);

        LinearLayout sort = row();
        sort.setPadding(0, dp(4), 0, 0);
        Button up = secondaryButton("上移");
        up.setEnabled(index > 0);
        up.setOnClickListener(v -> {
            db.moveStudent(s.id, -1);
            renderStudentList();
        });
        sort.addView(up, weightParams());

        Button down = secondaryButton("下移");
        down.setEnabled(index < count - 1);
        down.setOnClickListener(v -> {
            db.moveStudent(s.id, 1);
            renderStudentList();
        });
        sort.addView(down, weightParams());
        card.addView(sort);

        if (s.remaining() <= 3) {
            TextView alert = text(s.remaining() <= 0 ? "课时已用完，不能继续计数" : "剩余课时不足", 12, DANGER, true);
            alert.setPadding(0, dp(4), 0, 0);
            card.addView(alert);
        }
        return card;
    }

    private void recordLesson(long studentId, boolean returnHome) {
        Student latest = db.student(studentId);
        if (latest == null) return;
        if (latest.remaining() <= 0) {
            Toast.makeText(this, "课时已用完，不能继续计数", Toast.LENGTH_SHORT).show();
            if (returnHome) showHome(); else showDetail(studentId);
            return;
        }
        showLessonForm(latest, returnHome);
    }

    private void showLessonForm(Student student, boolean returnHome) {
        LinearLayout form = column();
        int pad = dp(10);
        form.setPadding(pad, pad, pad, 0);

        String[] selectedDate = new String[]{today()};
        Button date = dateSelectButton("上课日期 " + selectedDate[0]);
        date.setOnClickListener(v -> pickDate(selectedDate[0], selected -> {
            selectedDate[0] = selected;
            date.setText("上课日期 " + selected);
        }));
        form.addView(date, matchWrap());

        EditText count = compactInput("添加节数");
        count.setInputType(InputType.TYPE_CLASS_NUMBER);
        count.setText("1");
        count.setSelectAllOnFocus(true);
        form.addView(count, matchWrap());

        TextView remain = text("剩余可添加 " + student.remaining() + " 节", 12, MUTED, false);
        remain.setPadding(dp(2), dp(4), dp(2), 0);
        form.addView(remain);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("添加上课记录")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Student latest = db.student(student.id);
            if (latest == null) {
                dialog.dismiss();
                if (returnHome) showHome(); else showDetail(student.id);
                return;
            }
            int lessonCount;
            try {
                lessonCount = Integer.parseInt(count.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "节数格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lessonCount <= 0) {
                Toast.makeText(this, "节数必须大于 0", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDate[0].compareTo(today()) > 0) {
                Toast.makeText(this, "上课日期不能晚于今天", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lessonCount > latest.remaining()) {
                Toast.makeText(this, "最多还能添加 " + latest.remaining() + " 节", Toast.LENGTH_SHORT).show();
                return;
            }
            db.addLessons(student.id, selectedDate[0], lessonCount);
            Toast.makeText(this, latest.name + " 已添加 " + lessonCount + " 节", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (returnHome) showHome(); else showDetail(student.id);
        });
    }

    private void showStudentForm(Student editing) {
        screen = "form";
        setScreen();
        currentStudent = editing;
        addBackHeader(editing == null ? "添加学生" : "编辑学生", () -> {
            if (editing == null) showHome(); else showDetail(editing.id);
        });

        EditText name = input("学生姓名");
        EditText price = input("购买金额");
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText total = input("总课时");
        total.setInputType(InputType.TYPE_CLASS_NUMBER);
        String[] purchaseDateValue = new String[]{editing == null ? today() : safe(editing.purchaseDate)};
        if (purchaseDateValue[0].isEmpty() || !isValidDate(purchaseDateValue[0])) {
            purchaseDateValue[0] = today();
        }
        Button purchaseDate = dateSelectButton("购买日期 " + purchaseDateValue[0]);
        purchaseDate.setOnClickListener(v -> pickDate(purchaseDateValue[0], selected -> {
            purchaseDateValue[0] = selected;
            purchaseDate.setText("购买日期 " + selected);
        }));
        EditText note = input("备注");

        if (editing != null) {
            name.setText(editing.name);
            price.setText(trimMoney(editing.price));
            total.setText(String.valueOf(editing.totalLessons));
            note.setText(editing.note);
        }

        root.addView(name, matchWrap());
        root.addView(price, matchWrap());
        root.addView(total, matchWrap());
        root.addView(purchaseDate, matchWrap());
        root.addView(note, matchWrap());

        Button save = actionButton("保存");
        save.setOnClickListener(v -> saveStudent(editing, name, price, total, purchaseDateValue[0], note));
        root.addView(save, matchWrap());
    }

    private void saveStudent(Student editing, EditText name, EditText price, EditText total, String purchaseDate, EditText note) {
        String n = name.getText().toString().trim();
        String p = price.getText().toString().trim();
        String t = total.getText().toString().trim();
        if (n.isEmpty() || t.isEmpty()) {
            Toast.makeText(this, "姓名和总课时必填", Toast.LENGTH_SHORT).show();
            return;
        }
        int lessons;
        double amount;
        try {
            lessons = Integer.parseInt(t);
            amount = p.isEmpty() ? 0 : Double.parseDouble(p);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额或课时格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lessons <= 0) {
            Toast.makeText(this, "总课时必须大于 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editing != null && lessons < editing.usedLessons) {
            Toast.makeText(this, "总课时不能小于已上课时", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editing == null) {
            db.addStudent(n, amount, lessons, purchaseDate, note.getText().toString().trim());
            showHome();
        } else {
            db.updateStudent(editing.id, n, amount, lessons, purchaseDate, note.getText().toString().trim());
            showDetail(editing.id);
        }
    }

    private void showDetail(long id) {
        Student s = db.student(id);
        if (s == null) {
            showHome();
            return;
        }
        if (currentStudent == null || currentStudent.id != id || detailCalendarMonth == null) {
            detailCalendarMonth = monthStart(s.lastDate == null ? today() : s.lastDate);
        }
        screen = "detail";
        currentStudent = s;
        setScreen();
        addBackHeader(s.name, () -> showHome());

        LinearLayout summary = card();
        summary.addView(text("购买信息", 15, INK, true));
        summary.addView(infoLine("购买金额", trimMoney(s.price) + " 元"));
        summary.addView(infoLine("总课时", s.totalLessons + " 节"));
        summary.addView(infoLine("已上课", s.usedLessons + " 节"));
        summary.addView(infoLine("剩余课时", s.remaining() + " 节", s.remaining() <= 3 ? DANGER : BLUE));
        summary.addView(infoLine("购买日期", safe(s.purchaseDate)));
        if (!safe(s.note).isEmpty()) summary.addView(infoLine("备注", s.note));
        root.addView(summary);

        LinearLayout primaryActions = row();
        primaryActions.setPadding(0, dp(4), 0, 0);
        Button lesson = s.remaining() > 0 ? actionButton("记一节") : disabledButton("课时已满");
        lesson.setEnabled(s.remaining() > 0);
        lesson.setOnClickListener(v -> recordLesson(s.id, false));
        primaryActions.addView(lesson, weightParams());
        root.addView(primaryActions);

        LinearLayout actions = row();
        actions.setPadding(0, dp(2), 0, dp(6));
        Button edit = secondaryButton("编辑");
        edit.setOnClickListener(v -> showStudentForm(s));
        actions.addView(edit, weightParams());
        Button delete = dangerButton("删除");
        delete.setOnClickListener(v -> confirmDeleteStudent(s));
        actions.addView(delete, weightParams());
        root.addView(actions);

        root.addView(lessonCalendar(s));

        TextView logTitle = text("上课明细", 15, INK, true);
        logTitle.setPadding(0, dp(14), 0, dp(6));
        root.addView(logTitle);

        List<Lesson> lessons = db.lessons(s.id);
        if (lessons.isEmpty()) {
            root.addView(text("暂无记录", 13, MUTED, false));
            return;
        }

        Map<String, List<Lesson>> grouped = new LinkedHashMap<>();
        for (Lesson lessonItem : lessons) {
            if (!grouped.containsKey(lessonItem.date)) grouped.put(lessonItem.date, new ArrayList<>());
            grouped.get(lessonItem.date).add(lessonItem);
        }
        LinearLayout list = listContainer();
        int index = 0;
        for (Map.Entry<String, List<Lesson>> entry : grouped.entrySet()) {
            list.addView(lessonDateRow(s.id, entry.getKey(), entry.getValue()));
            if (index < grouped.size() - 1) {
                list.addView(divider());
            }
            index++;
        }
        root.addView(list);
    }

    private View lessonDateRow(long studentId, String date, List<Lesson> lessons) {
        LinearLayout line = column();
        line.setPadding(dp(14), dp(11), dp(14), dp(11));

        LinearLayout top = row();
        LinearLayout info = column();
        info.addView(text(date, 14, INK, true));
        info.addView(text("共 " + lessons.size() + " 节", 12, MUTED, false));
        top.addView(info, weightParams());
        top.addView(statusPill(lessons.size() + " 节", BLUE, BLUE_SOFT));
        line.addView(top);

        LinearLayout actions = row();
        actions.setPadding(0, dp(8), 0, 0);
        Button view = secondaryButton("查看");
        view.setOnClickListener(v -> showLessonDayDialog(studentId, date));
        actions.addView(view, weightParams());

        Button delete = dangerButton("删除 1 节");
        delete.setOnClickListener(v -> confirmDeleteLesson(lessons.get(0).id, studentId));
        actions.addView(delete, weightParams());
        line.addView(actions);
        return line;
    }

    private View lessonCalendar(Student student) {
        LinearLayout calendar = card();
        LinearLayout header = row();

        Button prev = iconButton("<");
        prev.setOnClickListener(v -> {
            detailCalendarMonth = shiftMonth(detailCalendarMonth, -1);
            showDetail(student.id);
        });
        header.addView(prev);

        TextView title = text(monthTitle(detailCalendarMonth), 15, INK, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, weightParams());

        Button next = iconButton(">");
        next.setOnClickListener(v -> {
            detailCalendarMonth = shiftMonth(detailCalendarMonth, 1);
            showDetail(student.id);
        });
        header.addView(next);
        calendar.addView(header);

        LinearLayout weekdays = row();
        weekdays.setPadding(0, dp(8), 0, dp(2));
        String[] labels = new String[]{"一", "二", "三", "四", "五", "六", "日"};
        for (String label : labels) {
            TextView day = text(label, 12, MUTED, true);
            day.setGravity(Gravity.CENTER);
            weekdays.addView(day, calendarCellParams());
        }
        calendar.addView(weekdays);

        Calendar month = calendarFromDate(detailCalendarMonth);
        month.set(Calendar.DAY_OF_MONTH, 1);
        String start = dateFormat.format(month.getTime());
        int firstOffset = mondayOffset(month);
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar end = (Calendar) month.clone();
        end.set(Calendar.DAY_OF_MONTH, daysInMonth);
        Map<String, Integer> counts = db.lessonCounts(student.id, start, dateFormat.format(end.getTime()));

        int dayNumber = 1;
        int totalCells = firstOffset + daysInMonth;
        int rows = (totalCells + 6) / 7;
        for (int r = 0; r < rows; r++) {
            LinearLayout week = row();
            week.setGravity(Gravity.CENTER);
            for (int c = 0; c < 7; c++) {
                int cellIndex = r * 7 + c;
                if (cellIndex < firstOffset || dayNumber > daysInMonth) {
                    week.addView(calendarBlankCell(), calendarCellParams());
                    continue;
                }

                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, dayNumber);
                String dateValue = dateFormat.format(date.getTime());
                int count = counts.containsKey(dateValue) ? counts.get(dateValue) : 0;
                TextView cell = calendarDayCell(dayNumber, count);
                if (count > 0) {
                    cell.setOnClickListener(v -> showLessonDayDialog(student.id, dateValue));
                }
                week.addView(cell, calendarCellParams());
                dayNumber++;
            }
            calendar.addView(week);
        }
        return calendar;
    }

    private void showLessonDayDialog(long studentId, String date) {
        List<Lesson> lessons = db.lessonsOn(studentId, date);
        if (lessons.isEmpty()) {
            Toast.makeText(this, "当天暂无记录", Toast.LENGTH_SHORT).show();
            showDetail(studentId);
            return;
        }

        LinearLayout list = column();
        int pad = dp(10);
        list.setPadding(pad, pad, pad, 0);

        final AlertDialog[] dialogRef = new AlertDialog[1];
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            LinearLayout line = row();
            line.setPadding(0, dp(4), 0, dp(4));
            line.addView(text("第 " + (i + 1) + " 节", 14, INK, true), weightParams());
            Button delete = dangerButton("删除");
            delete.setOnClickListener(v -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                confirmDeleteLesson(lesson.id, studentId);
            });
            line.addView(delete);
            list.addView(line, matchWrap());
        }

        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle(date + " 共 " + lessons.size() + " 节")
                .setView(list)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void confirmDeleteStudent(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除学生？")
                .setMessage("会同时删除该学生的所有上课记录，无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    db.deleteStudent(student.id);
                    Toast.makeText(this, "已删除 " + student.name, Toast.LENGTH_SHORT).show();
                    showHome();
                })
                .show();
    }

    private void confirmDeleteLesson(long lessonId, long studentId) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除这条上课记录？")
                .setMessage("删除后会自动恢复 1 节剩余课时。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    db.deleteLesson(lessonId);
                    showDetail(studentId);
                })
                .show();
    }

    private void showStats() {
        screen = "stats";
        currentStudent = null;
        setScreen();
        LinearLayout header = row();
        header.addView(text("上课统计", 24, INK, true), weightParams());
        root.addView(header);
        root.addView(navBar(false));

        LinearLayout quick1 = row();
        quick1.setPadding(0, dp(4), 0, dp(1));
        quick1.addView(rangeButton("今天", todayRange(), "今天"), weightParams());
        quick1.addView(rangeButton("本周", weekRange(), "本周"), weightParams());
        root.addView(quick1);

        LinearLayout quick2 = row();
        quick2.setPadding(0, dp(1), 0, dp(4));
        quick2.addView(rangeButton("本月", monthRange(), "本月"), weightParams());
        quick2.addView(rangeButton("本年", yearRange(), "本年"), weightParams());
        root.addView(quick2);

        LinearLayout custom = row();
        custom.setPadding(0, dp(4), 0, dp(2));
        Button start = dateSelectButton("开始 " + statsStart);
        start.setOnClickListener(v -> pickDate(statsStart, selected -> {
            statsStart = selected;
            statsLabel = "自定义";
            statsCalendarMonth = monthStart(selected);
            showStats();
        }));
        Button end = dateSelectButton("结束 " + statsEnd);
        end.setOnClickListener(v -> pickDate(statsEnd, selected -> {
            statsEnd = selected;
            statsLabel = "自定义";
            showStats();
        }));
        custom.addView(start, weightParams());
        custom.addView(end, weightParams());
        root.addView(custom);

        Button query = actionButton("查询自定义范围");
        query.setOnClickListener(v -> {
            if (statsStart.compareTo(statsEnd) > 0) {
                Toast.makeText(this, "开始日期不能晚于结束日期", Toast.LENGTH_SHORT).show();
                return;
            }
            statsLabel = "自定义";
            statsCalendarMonth = monthStart(statsStart);
            showStats();
        });
        root.addView(query, matchWrap());

        RangeStats total = db.rangeStats(statsStart, statsEnd);
        LinearLayout totalCard = card();
        totalCard.addView(text(statsLabel + "：" + statsStart + " 至 " + statsEnd, 13, MUTED, false));
        totalCard.addView(text(total.lessons + " 节", 26, BLUE, true));
        totalCard.addView(text("上课学生 " + total.students + " 人，发生上课 " + total.days + " 天", 13, MUTED, false));
        root.addView(totalCard);

        root.addView(statsCalendar());

        TextView listTitle = text("按日期查看", 15, INK, true);
        listTitle.setPadding(0, dp(14), 0, dp(4));
        root.addView(listTitle);

        List<DaySummary> days = db.daySummaries(statsStart, statsEnd);
        if (days.isEmpty()) {
            root.addView(text("这个范围内暂无上课记录", 13, MUTED, false));
            return;
        }
        for (DaySummary day : days) {
            LinearLayout dayCard = card();
            LinearLayout line = row();
            line.addView(text(day.date, 14, INK, true), weightParams());
            line.addView(statusPill(day.lessons + " 节", BLUE, BLUE_SOFT));
            dayCard.addView(line);
            dayCard.addView(text(day.students + " 名学生，点击查看明细", 12, MUTED, false));
            dayCard.setOnClickListener(v -> showDayDetail(day.date));
            root.addView(dayCard);
        }
    }

    private View statsCalendar() {
        if (statsCalendarMonth == null) {
            statsCalendarMonth = monthStart(statsStart == null ? today() : statsStart);
        }

        LinearLayout calendar = card();
        LinearLayout header = row();

        Button prev = iconButton("<");
        prev.setOnClickListener(v -> {
            statsCalendarMonth = shiftMonth(statsCalendarMonth, -1);
            showStats();
        });
        header.addView(prev);

        LinearLayout titleBox = column();
        TextView title = text(monthTitle(statsCalendarMonth), 15, INK, true);
        title.setGravity(Gravity.CENTER);
        TextView hint = text("点击有课日期查看学生", 11, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        titleBox.addView(title);
        titleBox.addView(hint);
        header.addView(titleBox, weightParams());

        Button next = iconButton(">");
        next.setOnClickListener(v -> {
            statsCalendarMonth = shiftMonth(statsCalendarMonth, 1);
            showStats();
        });
        header.addView(next);
        calendar.addView(header);

        LinearLayout weekdays = row();
        weekdays.setPadding(0, dp(8), 0, dp(2));
        String[] labels = new String[]{"一", "二", "三", "四", "五", "六", "日"};
        for (String label : labels) {
            TextView day = text(label, 12, MUTED, true);
            day.setGravity(Gravity.CENTER);
            weekdays.addView(day, calendarCellParams());
        }
        calendar.addView(weekdays);

        Calendar month = calendarFromDate(statsCalendarMonth);
        month.set(Calendar.DAY_OF_MONTH, 1);
        String start = dateFormat.format(month.getTime());
        int firstOffset = mondayOffset(month);
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar end = (Calendar) month.clone();
        end.set(Calendar.DAY_OF_MONTH, daysInMonth);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DaySummary summary : db.daySummaries(start, dateFormat.format(end.getTime()))) {
            counts.put(summary.date, summary.lessons);
        }

        int dayNumber = 1;
        int totalCells = firstOffset + daysInMonth;
        int rows = (totalCells + 6) / 7;
        for (int r = 0; r < rows; r++) {
            LinearLayout week = row();
            week.setGravity(Gravity.CENTER);
            for (int c = 0; c < 7; c++) {
                int cellIndex = r * 7 + c;
                if (cellIndex < firstOffset || dayNumber > daysInMonth) {
                    week.addView(calendarBlankCell(), calendarCellParams());
                    continue;
                }

                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, dayNumber);
                String dateValue = dateFormat.format(date.getTime());
                int count = counts.containsKey(dateValue) ? counts.get(dateValue) : 0;
                TextView cell = calendarDayCell(dayNumber, count);
                if (count > 0) {
                    cell.setOnClickListener(v -> showDayDetail(dateValue));
                }
                week.addView(cell, calendarCellParams());
                dayNumber++;
            }
            calendar.addView(week);
        }
        return calendar;
    }

    private Button rangeButton(String label, DateRange range, String statLabel) {
        Button btn = secondaryButton(label);
        btn.setOnClickListener(v -> {
            statsStart = range.start;
            statsEnd = range.end;
            statsLabel = statLabel;
            statsCalendarMonth = monthStart(range.start);
            showStats();
        });
        return btn;
    }

    private void pickDate(String currentDate, DatePicked callback) {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        try {
            Date parsed = dateFormat.parse(currentDate);
            if (parsed != null) c.setTime(parsed);
        } catch (ParseException ignored) {
        }
        DatePicker picker = new DatePicker(this);
        picker.setCalendarViewShown(false);
        picker.setSpinnersShown(true);
        picker.init(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH),
                null
        );
        int pad = dp(12);
        picker.setPadding(pad, pad, pad, 0);

        new AlertDialog.Builder(this)
                .setTitle("选择日期")
                .setView(picker)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    Calendar selected = Calendar.getInstance(Locale.CHINA);
                    selected.set(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
                    callback.onDatePicked(dateFormat.format(selected.getTime()));
                })
                .show();
    }

    private void showDayDetail(String date) {
        screen = "day";
        setScreen();
        addBackHeader(date, () -> showStats());
        RangeStats total = db.rangeStats(date, date);
        LinearLayout summary = card();
        summary.addView(text(date + " 共 " + total.lessons + " 节", 17, BLUE, true));
        summary.addView(text("上课学生 " + total.students + " 人", 13, MUTED, false));
        root.addView(summary);

        List<StudentLesson> rows = db.studentLessonsOn(date);
        LinearLayout list = listContainer();
        for (int i = 0; i < rows.size(); i++) {
            StudentLesson row = rows.get(i);
            list.addView(compactStudentLessonRow(row));
            if (i < rows.size() - 1) {
                list.addView(divider());
            }
        }
        root.addView(list);
    }

    private void confirmExportCsv() {
        new AlertDialog.Builder(this)
                .setTitle("导出 CSV 备份？")
                .setMessage("将生成当前所有学生和上课明细的 CSV 文件。")
                .setNegativeButton("取消", null)
                .setPositiveButton("导出", (dialog, which) -> exportCsv())
                .show();
    }

    private void exportCsv() {
        try {
            String fileName = "课时统计-" + today() + ".csv";
            pendingExportCsv = db.exportCsv();
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, REQUEST_EXPORT_CSV);
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeCsvBytes(OutputStream out, String csv) throws Exception {
        out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
        out.write(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT_CSV) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingExportCsv = null;
            Toast.makeText(this, "已取消导出", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = data.getData();
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("无法写入所选文件");
            writeCsvBytes(out, pendingExportCsv == null ? "" : pendingExportCsv);
            Toast.makeText(this, "导出成功", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pendingExportCsv = null;
        }
    }

    @Override
    public void onBackPressed() {
        if ("detail".equals(screen) || "stats".equals(screen)) showHome();
        else if ("form".equals(screen) && currentStudent != null) showDetail(currentStudent.id);
        else if ("day".equals(screen)) showStats();
        else if ("form".equals(screen)) showHome();
        else super.onBackPressed();
    }

    private void addBackHeader(String title, Runnable backAction) {
        LinearLayout header = row();
        Button back = iconButton("←");
        back.setOnClickListener(v -> backAction.run());
        header.addView(back);
        header.addView(text(title, 21, INK, true), weightParams());
        root.addView(header);
    }

    private TextView infoLine(String label, String value) {
        return infoLine(label, value, MUTED);
    }

    private TextView infoLine(String label, String value, int color) {
        TextView view = text(label + "：" + safe(value), 13, color, color != MUTED);
        view.setPadding(0, dp(4), 0, dp(2));
        return view;
    }

    private View homeSummary(Stats summary) {
        LinearLayout box = card();

        LinearLayout top = row();
        top.addView(statTile("学生", String.valueOf(summary.students), BLUE, BLUE_SOFT), weightParams());
        top.addView(statTile("总课时", summary.totalLessons + " 节", INK, SURFACE), weightParams());
        box.addView(top);

        LinearLayout bottom = row();
        bottom.addView(statTile("已上", summary.usedLessons + " 节", TEAL, TEAL_SOFT), weightParams());
        bottom.addView(statTile("剩余", summary.remaining + " 节", summary.remaining <= 3 ? DANGER : BLUE,
                summary.remaining <= 3 ? DANGER_SOFT : BLUE_SOFT), weightParams());
        box.addView(bottom);

        TextView month = text("本月已上 " + summary.monthLessons + " 节", 12, MUTED, false);
        month.setGravity(Gravity.CENTER);
        month.setPadding(0, dp(6), 0, 0);
        box.addView(month);
        return box;
    }

    private LinearLayout statTile(String label, String value, int valueColor, int fillColor) {
        LinearLayout view = column();
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(9), dp(8), dp(9));
        view.setBackground(rounded(fillColor, cornerRadius(), 0, 0));
        TextView labelView = text(label, 11, MUTED, false);
        labelView.setGravity(Gravity.CENTER);
        TextView valueView = text(value, 16, valueColor, true);
        valueView.setGravity(Gravity.CENTER);
        view.addView(labelView);
        view.addView(valueView);
        return view;
    }

    private LinearLayout metric(String label, String value, int valueColor) {
        LinearLayout view = column();
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(6), dp(8), dp(6), dp(8));
        view.setBackground(rounded(SURFACE, cornerRadius(), 0, 0));
        TextView labelView = text(label, 11, MUTED, false);
        labelView.setGravity(Gravity.CENTER);
        TextView valueView = text(value, 15, valueColor, true);
        valueView.setGravity(Gravity.CENTER);
        view.addView(labelView);
        view.addView(valueView);
        return view;
    }

    private TextView statusPill(String value, int color, int fill) {
        TextView view = text(value, 12, color, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(9), dp(5), dp(9), dp(5));
        view.setBackground(rounded(fill, cornerRadius(), 0, 0));
        return view;
    }

    private TextView calendarDayCell(int day, int lessons) {
        String value = lessons > 0 ? day + "\n" + lessons + "节" : String.valueOf(day);
        TextView view = text(value, lessons > 0 ? 10 : 13, lessons > 0 ? BLUE : INK, lessons > 0);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(50));
        view.setIncludeFontPadding(false);
        view.setPadding(dp(2), dp(4), dp(2), dp(4));
        view.setBackground(rounded(lessons > 0 ? BLUE_SOFT : CARD, cornerRadius(), lessons > 0 ? BLUE : LINE, 1));
        return view;
    }

    private TextView calendarBlankCell() {
        TextView view = text("", 12, MUTED, false);
        view.setMinHeight(dp(50));
        return view;
    }

    private LinearLayout card() {
        LinearLayout view = column();
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(rounded(CARD, cornerRadius(), LINE, 1));
        view.setElevation(dp(1));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(7), 0, dp(7));
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout listContainer() {
        LinearLayout view = column();
        view.setPadding(0, dp(2), 0, dp(2));
        view.setBackground(rounded(CARD, cornerRadius(), LINE, 1));
        view.setElevation(dp(1));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(7), 0, dp(7));
        view.setLayoutParams(params);
        return view;
    }

    private View compactStudentLessonRow(StudentLesson row) {
        LinearLayout line = row();
        line.setPadding(dp(14), dp(11), dp(14), dp(11));
        line.addView(text(row.name, 14, INK, true), weightParams());
        line.addView(statusPill(row.lessons + " 节", BLUE, BLUE_SOFT));
        return line;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(LINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(1)));
        params.setMargins(dp(14), 0, dp(14), 0);
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private EditText input(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextColor(INK);
        view.setHintTextColor(MUTED);
        view.setTextSize(14);
        view.setSingleLine(false);
        view.setMinHeight(dp(44));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setBackground(rounded(CARD, cornerRadius(), LINE, 1));
        return view;
    }

    private EditText compactInput(String hint) {
        EditText view = input(hint);
        view.setSingleLine(true);
        view.setTextSize(13);
        return view;
    }

    private Button actionButton(String label) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(13);
        view.setAllCaps(false);
        view.setSingleLine(false);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(42));
        view.setMinWidth(0);
        view.setMinimumWidth(0);
        view.setPadding(dp(10), 0, dp(10), 0);
        view.setBackground(rounded(BLUE, cornerRadius(), 0, 0));
        return view;
    }

    private Button secondaryButton(String label) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(BLUE);
        view.setTextSize(13);
        view.setAllCaps(false);
        view.setSingleLine(false);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(42));
        view.setMinWidth(0);
        view.setMinimumWidth(0);
        view.setPadding(dp(10), 0, dp(10), 0);
        view.setBackground(rounded(BLUE_SOFT, cornerRadius(), 0, 0));
        return view;
    }

    private Button dateSelectButton(String label) {
        Button view = secondaryButton(label);
        view.setGravity(Gravity.CENTER);
        view.setBackground(rounded(CARD, cornerRadius(), LINE, 1));
        return view;
    }

    private Button dangerButton(String label) {
        Button view = secondaryButton(label);
        view.setTextColor(DANGER);
        view.setBackground(rounded(DANGER_SOFT, cornerRadius(), 0, 0));
        return view;
    }

    private Button disabledButton(String label) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(13);
        view.setAllCaps(false);
        view.setSingleLine(false);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(42));
        view.setMinWidth(0);
        view.setMinimumWidth(0);
        view.setPadding(dp(10), 0, dp(10), 0);
        view.setBackground(rounded(DISABLED, cornerRadius(), 0, 0));
        return view;
    }

    private Button iconButton(String label) {
        Button view = secondaryButton(label);
        view.setTextSize(15);
        view.setMinWidth(dp(36));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(4), 0, dp(4));
        return params;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        return params;
    }

    private LinearLayout.LayoutParams calendarCellParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int cornerRadius() {
        return dp(CORNER_RADIUS_DP);
    }

    private GradientDrawable rounded(int fill, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private String today() {
        return dateFormat.format(new Date());
    }

    private Calendar calendarFromDate(String value) {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        try {
            Date parsed = dateFormat.parse(value);
            if (parsed != null) c.setTime(parsed);
        } catch (ParseException ignored) {
        }
        return c;
    }

    private String monthStart(String date) {
        Calendar c = calendarFromDate(date);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return dateFormat.format(c.getTime());
    }

    private String shiftMonth(String monthStart, int amount) {
        Calendar c = calendarFromDate(monthStart);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, amount);
        return dateFormat.format(c.getTime());
    }

    private String monthTitle(String monthStart) {
        Calendar c = calendarFromDate(monthStart);
        return c.get(Calendar.YEAR) + "年" + (c.get(Calendar.MONTH) + 1) + "月";
    }

    private int mondayOffset(Calendar date) {
        return (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    private boolean isValidDate(String value) {
        try {
            dateFormat.setLenient(false);
            dateFormat.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private DateRange todayRange() {
        String today = today();
        return new DateRange(today, today);
    }

    private DateRange weekRange() {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String start = dateFormat.format(c.getTime());
        c.add(Calendar.DAY_OF_MONTH, 6);
        return new DateRange(start, dateFormat.format(c.getTime()));
    }

    private DateRange monthRange() {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        c.set(Calendar.DAY_OF_MONTH, 1);
        String start = dateFormat.format(c.getTime());
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new DateRange(start, dateFormat.format(c.getTime()));
    }

    private DateRange yearRange() {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        c.set(Calendar.DAY_OF_YEAR, 1);
        String start = dateFormat.format(c.getTime());
        c.set(Calendar.DAY_OF_YEAR, c.getActualMaximum(Calendar.DAY_OF_YEAR));
        return new DateRange(start, dateFormat.format(c.getTime()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimMoney(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    static class DateRange {
        String start;
        String end;

        DateRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    interface DatePicked {
        void onDatePicked(String date);
    }

    static class Student {
        long id;
        String name;
        double price;
        int totalLessons;
        int usedLessons;
        String purchaseDate;
        String note;
        int sortOrder;
        String lastDate;

        int remaining() {
            return totalLessons - usedLessons;
        }
    }

    static class Lesson {
        long id;
        String date;
    }

    static class Stats {
        int students;
        int totalLessons;
        int usedLessons;
        int remaining;
        int monthLessons;
    }

    static class RangeStats {
        int lessons;
        int students;
        int days;
    }

    static class DaySummary {
        String date;
        int lessons;
        int students;
    }

    static class StudentLesson {
        String name;
        int lessons;
    }

    static class Db extends SQLiteOpenHelper {
        Db(Context context) {
            super(context, "teaching_tally.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE students (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "price REAL NOT NULL DEFAULT 0," +
                    "total_lessons INTEGER NOT NULL," +
                    "purchase_date TEXT," +
                    "note TEXT," +
                    "sort_order INTEGER NOT NULL," +
                    "created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE lessons (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "student_id INTEGER NOT NULL," +
                    "lesson_date TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        long addStudent(String name, double price, int totalLessons, String purchaseDate, String note) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = studentValues(name, price, totalLessons, purchaseDate, note);
            values.put("sort_order", nextSortOrder(db));
            values.put("created_at", System.currentTimeMillis());
            return db.insert("students", null, values);
        }

        void updateStudent(long id, String name, double price, int totalLessons, String purchaseDate, String note) {
            getWritableDatabase().update("students", studentValues(name, price, totalLessons, purchaseDate, note),
                    "id=?", new String[]{String.valueOf(id)});
        }

        void deleteStudent(long id) {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete("lessons", "student_id=?", new String[]{String.valueOf(id)});
                db.delete("students", "id=?", new String[]{String.valueOf(id)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        private ContentValues studentValues(String name, double price, int totalLessons, String purchaseDate, String note) {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("price", price);
            values.put("total_lessons", totalLessons);
            values.put("purchase_date", purchaseDate);
            values.put("note", note);
            return values;
        }

        void addLessons(long studentId, String date, int count) {
            Student s = student(studentId);
            if (s == null || count <= 0 || s.remaining() < count) return;
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                long now = System.currentTimeMillis();
                for (int i = 0; i < count; i++) {
                    ContentValues values = new ContentValues();
                    values.put("student_id", studentId);
                    values.put("lesson_date", date);
                    values.put("created_at", now + i);
                    db.insert("lessons", null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        void deleteLesson(long id) {
            getWritableDatabase().delete("lessons", "id=?", new String[]{String.valueOf(id)});
        }

        List<Student> students(String query) {
            String where = "";
            String[] args = new String[]{};
            if (query != null && !query.isEmpty()) {
                where = "WHERE s.name LIKE ?";
                args = new String[]{"%" + query + "%"};
            }
            return readStudents("SELECT s.id,s.name,s.price,s.total_lessons,s.purchase_date,s.note,s.sort_order," +
                    "COUNT(l.id) used_lessons, MAX(l.lesson_date) last_date " +
                    "FROM students s LEFT JOIN lessons l ON l.student_id=s.id " +
                    where + " GROUP BY s.id ORDER BY s.sort_order ASC, s.id ASC", args);
        }

        Student student(long id) {
            List<Student> rows = readStudents("SELECT s.id,s.name,s.price,s.total_lessons,s.purchase_date,s.note,s.sort_order," +
                    "COUNT(l.id) used_lessons, MAX(l.lesson_date) last_date " +
                    "FROM students s LEFT JOIN lessons l ON l.student_id=s.id " +
                    "WHERE s.id=? GROUP BY s.id", new String[]{String.valueOf(id)});
            return rows.isEmpty() ? null : rows.get(0);
        }

        List<Lesson> lessons(long studentId) {
            List<Lesson> rows = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT id,lesson_date FROM lessons WHERE student_id=? ORDER BY lesson_date DESC, created_at DESC",
                    new String[]{String.valueOf(studentId)})) {
                while (c.moveToNext()) {
                    Lesson row = new Lesson();
                    row.id = c.getLong(0);
                    row.date = c.getString(1);
                    rows.add(row);
                }
            }
            return rows;
        }

        List<Lesson> lessonsOn(long studentId, String date) {
            List<Lesson> rows = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT id,lesson_date FROM lessons WHERE student_id=? AND lesson_date=? ORDER BY created_at DESC",
                    new String[]{String.valueOf(studentId), date})) {
                while (c.moveToNext()) {
                    Lesson row = new Lesson();
                    row.id = c.getLong(0);
                    row.date = c.getString(1);
                    rows.add(row);
                }
            }
            return rows;
        }

        Map<String, Integer> lessonCounts(long studentId, String start, String end) {
            Map<String, Integer> rows = new LinkedHashMap<>();
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT lesson_date, COUNT(*) FROM lessons " +
                            "WHERE student_id=? AND lesson_date BETWEEN ? AND ? GROUP BY lesson_date",
                    new String[]{String.valueOf(studentId), start, end})) {
                while (c.moveToNext()) {
                    rows.put(c.getString(0), c.getInt(1));
                }
            }
            return rows;
        }

        void moveStudent(long id, int direction) {
            List<Student> all = students("");
            int index = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id == id) index = i;
            }
            int target = index + direction;
            if (index < 0 || target < 0 || target >= all.size()) return;
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                setSort(db, all.get(index).id, all.get(target).sortOrder);
                setSort(db, all.get(target).id, all.get(index).sortOrder);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        Stats stats() {
            Stats s = new Stats();
            List<Student> students = students("");
            s.students = students.size();
            for (Student student : students) {
                s.totalLessons += student.totalLessons;
                s.usedLessons += student.usedLessons;
                s.remaining += Math.max(student.remaining(), 0);
            }
            String month = new SimpleDateFormat("yyyy-MM", Locale.CHINA).format(new Date()) + "%";
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM lessons WHERE lesson_date LIKE ?", new String[]{month})) {
                if (c.moveToFirst()) s.monthLessons = c.getInt(0);
            }
            return s;
        }

        RangeStats rangeStats(String start, String end) {
            RangeStats stats = new RangeStats();
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(*), COUNT(DISTINCT student_id), COUNT(DISTINCT lesson_date) " +
                            "FROM lessons WHERE lesson_date BETWEEN ? AND ?",
                    new String[]{start, end})) {
                if (c.moveToFirst()) {
                    stats.lessons = c.getInt(0);
                    stats.students = c.getInt(1);
                    stats.days = c.getInt(2);
                }
            }
            return stats;
        }

        List<DaySummary> daySummaries(String start, String end) {
            List<DaySummary> rows = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT lesson_date, COUNT(*), COUNT(DISTINCT student_id) " +
                            "FROM lessons WHERE lesson_date BETWEEN ? AND ? " +
                            "GROUP BY lesson_date ORDER BY lesson_date DESC",
                    new String[]{start, end})) {
                while (c.moveToNext()) {
                    DaySummary row = new DaySummary();
                    row.date = c.getString(0);
                    row.lessons = c.getInt(1);
                    row.students = c.getInt(2);
                    rows.add(row);
                }
            }
            return rows;
        }

        List<StudentLesson> studentLessonsOn(String date) {
            List<StudentLesson> rows = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT s.name, COUNT(l.id) FROM lessons l JOIN students s ON s.id=l.student_id " +
                            "WHERE l.lesson_date=? GROUP BY s.id,s.name ORDER BY s.sort_order ASC,s.name ASC",
                    new String[]{date})) {
                while (c.moveToNext()) {
                    StudentLesson row = new StudentLesson();
                    row.name = c.getString(0);
                    row.lessons = c.getInt(1);
                    rows.add(row);
                }
            }
            return rows;
        }

        String exportCsv() {
            StringBuilder out = new StringBuilder();
            out.append("学生姓名,购买金额,总课时,已上课时,剩余课时,购买日期,最近上课,备注\n");
            for (Student s : students("")) {
                out.append(csv(s.name)).append(',')
                        .append(s.price).append(',')
                        .append(s.totalLessons).append(',')
                        .append(s.usedLessons).append(',')
                        .append(s.remaining()).append(',')
                        .append(csv(s.purchaseDate)).append(',')
                        .append(csv(s.lastDate)).append(',')
                        .append(csv(s.note)).append('\n');
            }
            out.append("\n上课明细\n学生姓名,上课日期\n");
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT s.name,l.lesson_date FROM lessons l JOIN students s ON s.id=l.student_id " +
                            "ORDER BY l.lesson_date DESC,l.created_at DESC", null)) {
                while (c.moveToNext()) {
                    out.append(csv(c.getString(0))).append(',').append(csv(c.getString(1))).append('\n');
                }
            }
            return out.toString();
        }

        private List<Student> readStudents(String sql, String[] args) {
            List<Student> rows = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
                while (c.moveToNext()) {
                    Student row = new Student();
                    row.id = c.getLong(0);
                    row.name = c.getString(1);
                    row.price = c.getDouble(2);
                    row.totalLessons = c.getInt(3);
                    row.purchaseDate = c.getString(4);
                    row.note = c.getString(5);
                    row.sortOrder = c.getInt(6);
                    row.usedLessons = c.getInt(7);
                    row.lastDate = c.getString(8);
                    rows.add(row);
                }
            }
            return rows;
        }

        private int nextSortOrder(SQLiteDatabase db) {
            try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(sort_order),0)+1 FROM students", null)) {
                return c.moveToFirst() ? c.getInt(0) : 1;
            }
        }

        private void setSort(SQLiteDatabase db, long id, int sort) {
            ContentValues values = new ContentValues();
            values.put("sort_order", sort);
            db.update("students", values, "id=?", new String[]{String.valueOf(id)});
        }

        private String csv(String value) {
            if (value == null) return "";
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
    }
}
