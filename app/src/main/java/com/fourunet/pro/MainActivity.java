package com.fourunet.pro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 20;
    private static final int REQ_PDF = 30;

    LinearLayout root;
    LinearLayout content;
    LinearLayout nav;
    String activeTab = "home";
    int selectedAmount = 50;
    int pendingReportAmount = -1;
    boolean pendingReportSummaryOnly = false;

    final int purple = Color.rgb(109, 75, 179);
    final int purpleLight = Color.rgb(200, 179, 255);
    final int bg = Color.rgb(17, 16, 22);
    final int card = Color.rgb(30, 27, 41);
    final int card2 = Color.rgb(40, 35, 55);
    final int text = Color.rgb(244, 241, 255);
    final int muted = Color.rgb(185, 179, 201);
    final int green = Color.rgb(66, 245, 138);
    final int orange = Color.rgb(255, 189, 89);
    final int red = Color.rgb(255, 99, 122);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppStore.ensureDefaultCategories(this);
        requestPermissionsIfNeeded();
        buildLayout();
        showHome();
    }

    private void requestPermissionsIfNeeded() {
        ArrayList<String> perms = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS);
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_SMS);
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!perms.isEmpty()) requestPermissions(perms.toArray(new String[0]), 10);
    }

    private void buildLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        setContentView(root);

        root.addView(header(), new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(14));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(card);
        nav.setPadding(dp(4), dp(6), dp(4), dp(6));
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        rebuildNav();
    }

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(dp(16), dp(24), dp(16), dp(14));
        h.setBackgroundColor(purpleLight);

        TextView title = tv("حاسب اونلاين", 24, Color.WHITE, true);
        title.setGravity(Gravity.RIGHT);
        h.addView(title);

        TextView sub = tv("بيع كروت فور يو نت تلقائيًا", 12, Color.argb(230, 255,255,255), false);
        sub.setGravity(Gravity.RIGHT);
        h.addView(sub);
        return h;
    }

    private void rebuildNav() {
        nav.removeAllViews();
        nav.addView(navButton("الإعدادات", "settings", "⚙", v -> showSettings()));
        nav.addView(navButton("السجل", "logs", "◷", v -> showLogs()));
        nav.addView(navButton("استيراد", "import", "⇧", v -> showImport()));
        nav.addView(navButton("الفئات", "categories", "▦", v -> showCategories()));
        nav.addView(navButton("الرئيسية", "home", "⌂", v -> showHome()));
    }

    private Button navButton(String label, String key, String icon, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(icon + "\n" + label);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(key.equals(activeTab) ? purpleLight : muted);
        b.setBackground(round(key.equals(activeTab) ? Color.rgb(48, 42, 66) : card, dp(16), Color.TRANSPARENT, 0));
        b.setOnClickListener(listener);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, dp(58), 1));
        return b;
    }

    private void setTab(String key) { activeTab = key; rebuildNav(); }
    private void clear() { content.removeAllViews(); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        if (strokeWidth > 0) gd.setStroke(strokeWidth, strokeColor);
        return gd;
    }

    private TextView tv(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout cardBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(round(card, dp(18), Color.argb(35, 255,255,255), dp(1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        return box;
    }

    private View separator() {
        View line = new View(this);
        line.setBackgroundColor(Color.argb(42, 255,255,255));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(8), 0, dp(8));
        line.setLayoutParams(lp);
        return line;
    }

    private TextView title(String s) {
        TextView t = tv(s, 20, text, true);
        t.setPadding(0, 0, 0, dp(12));
        return t;
    }

    private TextView small(String s) {
        TextView t = tv(s, 13, muted, false);
        t.setLineSpacing(2, 1.1f);
        return t;
    }

    private TextView badge(String s, int color) {
        TextView b = tv(s, 12, color, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(5), dp(10), dp(5));
        b.setBackground(round(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)), dp(18), color, dp(1)));
        return b;
    }

    private Button action(String label, int bgColor, int fgColor, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(fgColor);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(round(bgColor, dp(16), Color.TRANSPARENT, 0));
        b.setOnClickListener(listener);
        return b;
    }

    private void showHome() {
        setTab("home");
        clear();

        LinearLayout status = cardBox();
        status.addView(tv(AppStore.isAutoSendEnabled(this) ? "النظام يعمل تلقائيًا" : "الإرسال التلقائي متوقف", 17, text, true));
        status.addView(small(AppStore.isAutoSendEnabled(this) ? "يستقبل رسائل Jawali / Jaib / ONE Cash ويرسل الكرت حسب الفئة." : "شغّل الإرسال التلقائي من الإعدادات عند الحاجة."));
        content.addView(status);

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        View sent = stat("كروت أرسلت", String.valueOf(AppStore.sentOperationsCount(this)));
        View processed = stat("تمت معالجتها", String.valueOf(AppStore.processedOperationsCount(this)));
        summary.addView(sent, new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(processed, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(summary);

        content.addView(title("الفئات والكميات"));
        ArrayList<CategoryItem> cats = AppStore.loadCategories(this);
        LinearLayout row = null;
        int col = 0;
        for (CategoryItem c : cats) {
            if (col == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                content.addView(row);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(4), dp(4), dp(4), dp(8));
            row.addView(categorySummary(c), lp);
            col++;
            if (col == 2) col = 0;
        }
    }

    private View stat(String label, String value) {
        LinearLayout box = cardBox();
        TextView l = tv(label, 12, muted, false); l.setGravity(Gravity.CENTER);
        TextView v = tv(value, 28, text, true); v.setGravity(Gravity.CENTER);
        box.addView(l); box.addView(v);
        return box;
    }

    private View categorySummary(CategoryItem c) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(round(card, dp(18), Color.argb(55, 255,255,255), dp(1)));

        TextView amount = tv(String.valueOf(c.amount), 26, purpleLight, true);
        amount.setGravity(Gravity.RIGHT);
        box.addView(amount);
        box.addView(small(c.amount + " ر.ي"));
        box.addView(separator());
        box.addView(small("إجمالي الكروت: " + AppStore.cardsByAmount(this, c.amount).size()));
        box.addView(small("كروت أرسلت: " + AppStore.soldCount(this, c.amount)));
        box.addView(small("الكروت المتبقية: " + AppStore.availableCount(this, c.amount)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(action("🗑", Color.TRANSPARENT, red, v -> confirmDeleteCategory(c)), new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(action("✎", Color.TRANSPARENT, Color.rgb(41, 167, 255), v -> showCategoryDialog(c)), new LinearLayout.LayoutParams(0, dp(44), 1));
        box.addView(actions);
        return box;
    }

    private void showCategories() {
        setTab("categories");
        clear();
        content.addView(title("إدارة الفئات"));

        LinearLayout add = cardBox();
        add.addView(tv("إضافة فئة جديدة", 17, text, true));
        add.addView(small("أي فئة تضيفها ستظهر مع الفئات الأساسية وتصبح قابلة للإدارة."));
        add.addView(action("إضافة فئة", purple, Color.WHITE, v -> showCategoryDialog(null)));
        content.addView(add);

        for (CategoryItem c : AppStore.loadCategories(this)) {
            LinearLayout box = cardBox();
            box.addView(tv(c.name + " - " + c.amount + " ريال", 18, text, true));
            box.addView(small("الحالة: " + (c.active ? "مفعلة" : "موقفة") + "\nالمتاح: " + AppStore.availableCount(this, c.amount) + " | المباع: " + AppStore.soldCount(this, c.amount)));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.addView(action("عرض الكروت", card2, text, v -> showCardsForCategory(c)), new LinearLayout.LayoutParams(0, -2, 1));
            actions.addView(action("تعديل", purple, Color.WHITE, v -> showCategoryDialog(c)), new LinearLayout.LayoutParams(0, -2, 1));
            box.addView(actions);

            Button del = action("حذف الفئة", Color.rgb(82,30,42), Color.WHITE, v -> confirmDeleteCategory(c));
            box.addView(del);
            content.addView(box);
        }
    }

    private void showCategoryDialog(CategoryItem category) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(8), dp(8), dp(8), dp(8));

        EditText amount = new EditText(this);
        amount.setHint("قيمة الفئة مثل 100");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        amount.setText(category == null ? "" : String.valueOf(category.amount));

        EditText name = new EditText(this);
        name.setHint("اسم الفئة");
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setText(category == null ? "" : category.name);

        CheckBox active = new CheckBox(this);
        active.setText("الفئة مفعلة");
        active.setChecked(category == null || category.active);

        layout.addView(amount);
        layout.addView(name);
        layout.addView(active);

        new AlertDialog.Builder(this)
                .setTitle(category == null ? "إضافة فئة" : "تعديل فئة")
                .setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    int a = 0;
                    try { a = Integer.parseInt(amount.getText().toString().trim()); } catch (Exception ignored) {}
                    if (a <= 0) { toast("أدخل فئة صحيحة"); return; }
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) n = "فئة " + a;
                    if (category == null) {
                        boolean ok = AppStore.addCategory(this, a);
                        if (!ok) toast("الفئة موجودة مسبقًا");
                    } else {
                        AppStore.updateCategory(this, category, a, n, active.isChecked());
                    }
                    showCategories();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void confirmDeleteCategory(CategoryItem c) {
        new AlertDialog.Builder(this)
                .setTitle("حذف الفئة")
                .setMessage("سيتم حذف الفئة من القائمة فقط، ولن يتم حذف الكروت المخزنة إلا إذا حذفتها يدويًا.")
                .setPositiveButton("حذف", (d,w) -> { AppStore.deleteCategory(this, c); showCategories(); })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showCardsForCategory(CategoryItem c) {
        setTab("categories");
        clear();
        content.addView(title("كروت " + c.name));

        LinearLayout tools = cardBox();
        tools.addView(tv("إدارة كروت الفئة", 17, text, true));
        tools.addView(action("إضافة كروت يدويًا", purple, Color.WHITE, v -> { selectedAmount = c.amount; showManualAddDialog(); }));
        tools.addView(action("استيراد TXT لهذه الفئة", card2, text, v -> { selectedAmount = c.amount; openTxtFile(); }));
        content.addView(tools);

        ArrayList<CardItem> list = AppStore.cardsByAmount(this, c.amount);
        if (list.isEmpty()) {
            LinearLayout empty = cardBox();
            empty.addView(small("لا توجد كروت في هذه الفئة."));
            content.addView(empty);
            return;
        }

        for (CardItem item : list) {
            LinearLayout box = cardBox();
            box.addView(badge(item.sold ? "مباع" : "متاح", item.sold ? orange : green));
            box.addView(tv(item.code, 17, text, true));
            if (item.sold) box.addView(small("أرسل إلى: " + item.buyerPhone + "\nالوقت: " + item.soldAt));
            LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.addView(action("تعديل", purple, Color.WHITE, v -> showCardEditDialog(item)), new LinearLayout.LayoutParams(0, -2, 1));
            actions.addView(action("حذف", Color.rgb(82,30,42), Color.WHITE, v -> { AppStore.deleteCard(this, item.id); showCardsForCategory(c); }), new LinearLayout.LayoutParams(0, -2, 1));
            box.addView(actions);
            content.addView(box);
        }
    }

    private void showCardEditDialog(CardItem card) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText amount = new EditText(this); amount.setInputType(InputType.TYPE_CLASS_NUMBER); amount.setText(String.valueOf(card.amount));
        EditText code = new EditText(this); code.setInputType(InputType.TYPE_CLASS_TEXT); code.setText(card.code);
        CheckBox sold = new CheckBox(this); sold.setText("مباع"); sold.setChecked(card.sold);
        layout.addView(amount); layout.addView(code); layout.addView(sold);

        new AlertDialog.Builder(this)
                .setTitle("تعديل كرت")
                .setView(layout)
                .setPositiveButton("حفظ", (d,w) -> {
                    int a = 0;
                    try { a = Integer.parseInt(amount.getText().toString().trim()); } catch(Exception ignored) {}
                    if (a <= 0 || code.getText().toString().trim().isEmpty()) { toast("بيانات غير صحيحة"); return; }
                    AppStore.updateCard(this, card, a, code.getText().toString().trim(), sold.isChecked());
                    showCategories();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showImport() {
        setTab("import");
        clear();
        content.addView(title("استيراد الكروت"));

        LinearLayout picker = cardBox();
        picker.addView(tv("اختر الفئة", 17, text, true));
        ArrayList<CategoryItem> cats = AppStore.loadCategories(this);
        String[] labels = new String[cats.size()];
        for (int i = 0; i < cats.size(); i++) labels[i] = String.valueOf(cats.get(i).amount);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { selectedAmount = Integer.parseInt(labels[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        picker.addView(spinner);
        content.addView(picker);

        LinearLayout ops = cardBox();
        ops.addView(tv("إضافة الكروت", 17, text, true));
        ops.addView(small("كل سطر في ملف TXT يساوي كرت واحد. يتم تجاهل المكرر والفارغ."));
        ops.addView(action("استيراد من ملف TXT", purple, Color.WHITE, v -> openTxtFile()));
        ops.addView(action("إضافة يدوية", card2, text, v -> showManualAddDialog()));
        content.addView(ops);
    }

    private void showLogs() {
        setTab("logs");
        clear();
        content.addView(title("السجلات والتقارير"));

        LinearLayout report = cardBox();
        report.addView(tv("تقرير PDF", 17, text, true));
        report.addView(small("يمكن تنزيل تقرير إجمالي مفصل مقسم إلى مربعات لكل فئة، أو تقرير مختصر، أو تقرير لفئة واحدة فقط."));
        report.addView(action("تقرير إجمالي مفصل حسب الفئات", purple, Color.WHITE, v -> startPdfDownload(-1, false)));
        report.addView(action("تقرير إجمالي مختصر", card2, text, v -> startPdfDownload(-1, true)));
        report.addView(action("تقرير PDF لفئة محددة", card2, text, v -> showPdfCategoryDialog()));
        report.addView(action("حذف كل إشعارات السداد", Color.rgb(82,30,42), Color.WHITE, v -> confirmClearLogs()));
        content.addView(report);

        ArrayList<OperationLog> logs = AppStore.loadLogs(this);
        if (logs.isEmpty()) {
            LinearLayout empty = cardBox();
            empty.addView(small("لا توجد عمليات حتى الآن."));
            content.addView(empty);
            return;
        }

        for (OperationLog log : logs) {
            LinearLayout box = cardBox();
            int color = log.status.contains("تم") ? green : (log.status.contains("معلق") ? orange : red);
            box.addView(badge(log.status, color));
            box.addView(tv(log.amount + " ريال", 18, text, true));
            box.addView(small("الدفع: " + log.provider + "\nالرقم: " + (log.customerPhone.isEmpty() ? "-" : log.customerPhone) + "\nالاسم: " + (log.customerName.isEmpty() ? "-" : log.customerName) + "\nالكرت: " + (log.cardCode.isEmpty() ? "-" : log.cardCode) + "\nالوقت: " + log.createdAt + "\nملاحظة: " + log.message));
            box.addView(action("حذف إشعار السداد", Color.rgb(82,30,42), Color.WHITE, v -> confirmDeleteLog(log)));
            content.addView(box);
        }
    }


    private void showPdfCategoryDialog() {
        ArrayList<CategoryItem> cats = AppStore.loadCategories(this);
        if (cats.isEmpty()) { toast("لا توجد فئات"); return; }
        String[] labels = new String[cats.size()];
        for (int i = 0; i < cats.size(); i++) labels[i] = cats.get(i).amount + " ريال";
        new AlertDialog.Builder(this)
                .setTitle("اختر الفئة للتقرير")
                .setItems(labels, (d, which) -> showPdfTypeDialog(cats.get(which).amount))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showPdfTypeDialog(int amount) {
        String[] items = new String[]{"تقرير مفصل لهذه الفئة", "تقرير مختصر لهذه الفئة"};
        new AlertDialog.Builder(this)
                .setTitle("نوع التقرير")
                .setItems(items, (d, which) -> startPdfDownload(amount, which == 1))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void startPdfDownload(int amount, boolean summaryOnly) {
        pendingReportAmount = amount;
        pendingReportSummaryOnly = summaryOnly;
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
        String mode = summaryOnly ? "summary" : "details";
        String name = amount < 0 ? "4U_NET_report_all_" + mode + "_" + stamp + ".pdf" : "4U_NET_report_" + amount + "_" + mode + "_" + stamp + ".pdf";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, REQ_PDF);
    }

    private void confirmDeleteLog(OperationLog log) {
        new AlertDialog.Builder(this)
                .setTitle("حذف إشعار السداد")
                .setMessage("سيتم حذف الإشعار من السجل فقط، ولن يتم إرجاع الكرت إلى المتاح.")
                .setPositiveButton("حذف", (d,w) -> { AppStore.deleteLog(this, log.id); showLogs(); })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void confirmClearLogs() {
        new AlertDialog.Builder(this)
                .setTitle("حذف كل إشعارات السداد")
                .setMessage("سيتم حذف سجلات العمليات فقط. الكروت المباعة ستبقى مباعة حتى لا يحدث تكرار بيع.")
                .setPositiveButton("حذف", (d,w) -> { AppStore.clearLogs(this); showLogs(); })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void writePdfReport(Uri uri, int amountFilter) {
        writePdfReport(uri, amountFilter, pendingReportSummaryOnly);
    }

    private void writePdfReport(Uri uri, int amountFilter, boolean summaryOnly) {
        PdfDocument pdf = new PdfDocument();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        int w = 595, h = 842, margin = 36, y = 44;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(w, h, 1).create());
        Canvas c = page.getCanvas();

        ArrayList<OperationLog> all = AppStore.loadLogs(this);
        ArrayList<OperationLog> logs = new ArrayList<>();
        for (OperationLog log : all) if (amountFilter < 0 || log.amount == amountFilter) logs.add(log);

        int soldCards = 0, availableCards = 0, totalCards = 0, totalAmount = 0, sentOps = 0;
        for (CardItem cardItem : AppStore.loadCards(this)) {
            if (amountFilter >= 0 && cardItem.amount != amountFilter) continue;
            totalCards++;
            if (cardItem.sold) soldCards++; else availableCards++;
        }
        for (OperationLog log : logs) {
            if ("تم الإرسال".equals(log.status)) {
                sentOps++;
                totalAmount += log.amount;
            }
        }

        p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(Color.BLACK);
        p.setTextSize(20); p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("تقرير مبيعات كروت فور يو نت", w - margin, y, p); y += 28;
        p.setTextSize(12); p.setTypeface(Typeface.DEFAULT);
        String range = amountFilter < 0 ? "إجمالي كل الفئات" : "فئة " + amountFilter + " ريال";
        c.drawText("النطاق: " + range, w - margin, y, p); y += 20;
        c.drawText("نوع التقرير: " + (summaryOnly ? "مختصر" : "مفصل") + " | تاريخ التقرير: " + AppStore.now(), w - margin, y, p); y += 28;

        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(14);
        c.drawText("الملخص", w - margin, y, p); y += 22;
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(12);
        String[] summary = new String[]{
                "إجمالي الكروت: " + totalCards,
                "الكروت المباعة: " + soldCards,
                "الكروت المتبقية: " + availableCards,
                "عمليات الإرسال الناجحة: " + sentOps,
                "إجمالي مبالغ العمليات الناجحة: " + totalAmount + " ريال",
                "عدد إشعارات السداد في التقرير: " + logs.size()
        };
        for (String line : summary) { c.drawText(line, w - margin, y, p); y += 18; }
        y += 12;

        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(14);
        c.drawText(amountFilter < 0 ? "ملخص الفئات" : "ملخص الفئة", w - margin, y, p); y += 18;
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(10);
        int pageNo = 1;
        ArrayList<CategoryItem> catsForReport = AppStore.loadCategories(this);
        for (CategoryItem cat : catsForReport) {
            if (amountFilter >= 0 && cat.amount != amountFilter) continue;
            if (y > h - 130) {
                p.setTextAlign(Paint.Align.CENTER); p.setTextSize(9);
                c.drawText("صفحة " + pageNo, w/2, h - 24, p);
                pdf.finishPage(page);
                pageNo++;
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNo).create());
                c = page.getCanvas();
                y = 44;
                p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(10); p.setTypeface(Typeface.DEFAULT);
            }
            int catTotal = AppStore.cardsByAmount(this, cat.amount).size();
            int catSold = AppStore.soldCount(this, cat.amount);
            int catAvailable = AppStore.availableCount(this, cat.amount);
            int catOps = 0;
            int catIncome = 0;
            for (OperationLog lg : logs) {
                if (lg.amount == cat.amount && "تم الإرسال".equals(lg.status)) {
                    catOps++;
                    catIncome += lg.amount;
                }
            }
            float left = margin;
            float top = y;
            float right = w - margin;
            float bottom = y + 88;
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.rgb(135, 108, 190));
            p.setStrokeWidth(1.2f);
            c.drawRoundRect(new RectF(left, top, right, bottom), 10, 10, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.BLACK);
            p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(12);
            c.drawText("فئة " + cat.amount + " ريال", w - margin - 12, y + 18, p);
            p.setTypeface(Typeface.DEFAULT); p.setTextSize(10);
            c.drawText("إجمالي الكروت: " + catTotal + "   |   المباعة: " + catSold + "   |   المتبقية: " + catAvailable, w - margin - 12, y + 38, p);
            c.drawText("عمليات الإرسال الناجحة: " + catOps + "   |   إجمالي مبلغ الفئة: " + catIncome + " ريال", w - margin - 12, y + 58, p);
            c.drawText("ملاحظة: تفاصيل السداد والكروت تظهر أسفل التقرير عند اختيار التقرير المفصل.", w - margin - 12, y + 76, p);
            y += 104;
        }

        if (summaryOnly) {
            p.setTextAlign(Paint.Align.CENTER); p.setTextSize(9); p.setTypeface(Typeface.DEFAULT);
            c.drawText("صفحة " + pageNo, w/2, h - 24, p);
            pdf.finishPage(page);
            try {
                OutputStream out = getContentResolver().openOutputStream(uri);
                pdf.writeTo(out);
                if (out != null) out.close();
                toast("تم حفظ تقرير PDF بنجاح");
            } catch (Exception e) {
                toast("فشل حفظ التقرير: " + e.getMessage());
            } finally {
                pdf.close();
            }
            return;
        }

        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(14); p.setColor(Color.BLACK);
        c.drawText("تفاصيل العمليات", w - margin, y, p); y += 22;
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(10);
        if (logs.isEmpty()) {
            c.drawText("لا توجد عمليات ضمن هذا النطاق.", w - margin, y, p); y += 18;
        }

        for (OperationLog log : logs) {
            if (y > h - 70) {
                p.setTextAlign(Paint.Align.CENTER); p.setTextSize(9);
                c.drawText("صفحة " + pageNo, w/2, h - 24, p);
                pdf.finishPage(page);
                pageNo++;
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(w, h, pageNo).create());
                c = page.getCanvas();
                y = 44;
                p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(10); p.setTypeface(Typeface.DEFAULT);
            }
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(log.createdAt + " | " + log.amount + " ريال | " + log.status, w - margin, y, p); y += 16;
            p.setTypeface(Typeface.DEFAULT);
            c.drawText("الدفع: " + safe(log.provider) + " | الرقم: " + safe(log.customerPhone), w - margin, y, p); y += 15;
            c.drawText("الكرت: " + (log.cardCode == null || log.cardCode.isEmpty() ? "-" : log.cardCode), w - margin, y, p); y += 15;
            c.drawText("ملاحظة: " + safe(log.message), w - margin, y, p); y += 22;
        }
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(9); p.setTypeface(Typeface.DEFAULT);
        c.drawText("صفحة " + pageNo, w/2, h - 24, p);
        pdf.finishPage(page);

        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            pdf.writeTo(out);
            if (out != null) out.close();
            toast("تم حفظ تقرير PDF بنجاح");
        } catch (Exception e) {
            toast("فشل حفظ التقرير: " + e.getMessage());
        } finally {
            pdf.close();
        }
    }

    private String safe(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v.trim();
    }

    private void showSettings() {
        setTab("settings");
        clear();
        content.addView(title("الإعدادات"));

        LinearLayout auto = cardBox();
        Switch sw = new Switch(this);
        sw.setText("تشغيل الإرسال التلقائي");
        sw.setTextColor(text);
        sw.setTextSize(17);
        sw.setTypeface(null, Typeface.BOLD);
        sw.setChecked(AppStore.isAutoSendEnabled(this));
        sw.setOnCheckedChangeListener((b, enabled) -> AppStore.setAutoSendEnabled(this, enabled));
        auto.addView(sw);
        auto.addView(small("الرسائل الموثوقة: Jawali / Jaib / ONE Cash."));
        content.addView(auto);

        LinearLayout messages = cardBox();
        messages.addView(tv("مركز رسائل الزبائن", 17, text, true));
        messages.addView(small("من هنا تعدّل رسالة الكرت ورسالة نفاد الفئة بشكل واضح، وتضيف العروض أو الملاحظات قبل الإرسال."));
        messages.addView(separator());
        messages.addView(small("معاينة رسالة الكرت الحالية:"));
        messages.addView(messagePreviewText(AppStore.buildSuccessMessage(this, 100, "1547013174")));
        messages.addView(action("فتح مركز تعديل الرسائل", purple, Color.WHITE, v -> showMessageTemplates()));
        content.addView(messages);

        LinearLayout trusted = cardBox();
        trusted.addView(tv("الأسماء الموثوقة لوَن كاش", 17, text, true));
        trusted.addView(small("أضف الاسم كما يظهر في ONE Cash مع رقم الإرسال. المطابقة تكون على الاسم الثلاثي."));
        trusted.addView(action("إدارة الأسماء الموثوقة", purple, Color.WHITE, v -> showTrustedContacts()));
        content.addView(trusted);

        LinearLayout danger = cardBox();
        danger.addView(tv("حذف البيانات", 17, text, true));
        danger.addView(action("حذف كل البيانات", Color.rgb(82,30,42), Color.WHITE, v -> new AlertDialog.Builder(this)
                .setTitle("تأكيد")
                .setMessage("هل تريد حذف كل البيانات؟")
                .setPositiveButton("نعم", (d,w) -> { AppStore.clearAll(this); showHome(); })
                .setNegativeButton("إلغاء", null)
                .show()));
        content.addView(danger);
    }

    private void showMessageTemplates() {
        setTab("settings");
        clear();
        content.addView(title("مركز تعديل رسائل الزبائن"));

        LinearLayout intro = cardBox();
        intro.addView(tv("طريقة التعديل", 17, text, true));
        intro.addView(small("اكتب الرسالة كما تريد أن تصل للزبون. يمكن إضافة عرض أو تنبيه أو ملاحظة في أي سطر. المتغيرات تتبدل تلقائيًا وقت الإرسال."));
        intro.addView(separator());
        intro.addView(badge("{amount} = مبلغ الفئة", purpleLight));
        intro.addView(badge("{card} = رقم الكرت", green));
        intro.addView(badge("{network} = اسم الشبكة", Color.rgb(41, 167, 255)));
        intro.addView(badge("{adminPhone} = رقم إدارة الشبكة", orange));
        content.addView(intro);

        LinearLayout info = cardBox();
        info.addView(tv("بيانات ثابتة داخل الرسائل", 17, text, true));
        info.addView(small("اسم الشبكة الحالي: " + AppStore.getNetworkName(this)));
        info.addView(small("رقم إدارة الشبكة الحالي: " + AppStore.getAdminPhone(this)));
        LinearLayout infoActions = new LinearLayout(this);
        infoActions.setOrientation(LinearLayout.HORIZONTAL);
        infoActions.addView(action("تعديل اسم الشبكة", purple, Color.WHITE, v -> showNetworkNameDialog()), new LinearLayout.LayoutParams(0, -2, 1));
        infoActions.addView(action("تعديل رقم الإدارة", card2, text, v -> showAdminPhoneDialog()), new LinearLayout.LayoutParams(0, -2, 1));
        info.addView(infoActions);
        content.addView(info);

        content.addView(templateBox(true));
        content.addView(templateBox(false));

        LinearLayout back = cardBox();
        back.addView(action("رجوع للإعدادات", card2, text, v -> showSettings()));
        content.addView(back);
    }

    private TextView messagePreviewText(String value) {
        TextView preview = tv(value, 14, text, false);
        preview.setGravity(Gravity.RIGHT);
        preview.setLineSpacing(4, 1.1f);
        preview.setPadding(dp(12), dp(10), dp(12), dp(10));
        preview.setBackground(round(card2, dp(12), Color.argb(35, 255,255,255), dp(1)));
        return preview;
    }

    private LinearLayout templateBox(boolean successMessage) {
        LinearLayout box = cardBox();
        String titleText = successMessage ? "رسالة إرسال الكرت" : "رسالة نفاد الفئة";
        String desc = successMessage
                ? "هذه الرسالة تصل عندما يتم العثور على كرت وإرساله للزبون."
                : "هذه الرسالة تصل عندما يتم استلام المبلغ لكن لا توجد كروت متاحة من نفس الفئة.";
        String sample = successMessage
                ? AppStore.buildSuccessMessage(this, 100, "1547013174")
                : AppStore.buildNoStockMessage(this, 100);

        box.addView(tv(titleText, 18, text, true));
        box.addView(small(desc));
        box.addView(separator());
        box.addView(small("معاينة على فئة 100 ريال:"));
        box.addView(messagePreviewText(sample));

        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.addView(action("تعديل النص", purple, Color.WHITE, v -> showMessageTemplateDialog(successMessage)), new LinearLayout.LayoutParams(0, -2, 1));
        actionsRow.addView(action("استعادة الافتراضي", Color.rgb(82,30,42), Color.WHITE, v -> confirmResetTemplate(successMessage)), new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(actionsRow);
        return box;
    }

    private void confirmResetTemplate(boolean successMessage) {
        new AlertDialog.Builder(this)
                .setTitle("استعادة النص الافتراضي")
                .setMessage("سيتم استبدال النص الحالي بالنص الافتراضي.")
                .setPositiveButton("استعادة", (d,w) -> {
                    if (successMessage) AppStore.setSuccessTemplate(this, AppStore.DEFAULT_SUCCESS_TEMPLATE);
                    else AppStore.setNoStockTemplate(this, AppStore.DEFAULT_NO_STOCK_TEMPLATE);
                    toast("تمت استعادة النص الافتراضي");
                    showMessageTemplates();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showNetworkNameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setGravity(Gravity.RIGHT);
        input.setText(AppStore.getNetworkName(this));
        new AlertDialog.Builder(this)
                .setTitle("تعديل اسم الشبكة")
                .setView(input)
                .setPositiveButton("حفظ", (d,w) -> {
                    AppStore.setNetworkName(this, input.getText().toString());
                    toast("تم حفظ اسم الشبكة");
                    showMessageTemplates();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showAdminPhoneDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setGravity(Gravity.RIGHT);
        input.setText(AppStore.getAdminPhone(this));
        new AlertDialog.Builder(this)
                .setTitle("تعديل رقم إدارة الشبكة")
                .setView(input)
                .setPositiveButton("حفظ", (d,w) -> {
                    AppStore.setAdminPhone(this, input.getText().toString());
                    toast("تم حفظ رقم الإدارة");
                    showMessageTemplates();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showMessageTemplateDialog(boolean successMessage) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView help = small(successMessage
                ? "عدّل رسالة الكرت كما تريد. يمكنك إضافة عرض في آخر الرسالة مثل: عرض خاص: عند شراء كرتين تحصل على خصم."
                : "عدّل رسالة نفاد الفئة. استخدم {adminPhone} حتى يظهر رقم إدارة الشبكة تلقائيًا.");

        TextView vars = small("المتغيرات المتاحة: {amount}  {card}  {network}  {adminPhone}");

        EditText input = new EditText(this);
        input.setMinLines(9);
        input.setGravity(Gravity.TOP | Gravity.RIGHT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(successMessage ? AppStore.getSuccessTemplate(this) : AppStore.getNoStockTemplate(this));

        TextView previewTitle = small("المعاينة:");
        TextView preview = messagePreviewText(successMessage
                ? AppStore.applyTemplate(this, input.getText().toString(), 100, "1547013174")
                : AppStore.applyTemplate(this, input.getText().toString(), 100, ""));

        Button previewBtn = action("تحديث المعاينة", card2, text, v -> {
            preview.setText(successMessage
                    ? AppStore.applyTemplate(this, input.getText().toString(), 100, "1547013174")
                    : AppStore.applyTemplate(this, input.getText().toString(), 100, ""));
        });

        Button offerBtn = action("إضافة سطر عرض", Color.rgb(50, 70, 55), Color.WHITE, v -> {
            input.append("\n\nعرض خاص: ");
            preview.setText(successMessage
                    ? AppStore.applyTemplate(this, input.getText().toString(), 100, "1547013174")
                    : AppStore.applyTemplate(this, input.getText().toString(), 100, ""));
        });

        layout.addView(help);
        layout.addView(vars);
        layout.addView(input);
        layout.addView(previewBtn);
        if (successMessage) layout.addView(offerBtn);
        layout.addView(previewTitle);
        layout.addView(preview);

        new AlertDialog.Builder(this)
                .setTitle(successMessage ? "تعديل رسالة إرسال الكرت" : "تعديل رسالة نفاد الفئة")
                .setView(layout)
                .setPositiveButton("حفظ", (d,w) -> {
                    String value = input.getText().toString();
                    if (successMessage) AppStore.setSuccessTemplate(this, value);
                    else AppStore.setNoStockTemplate(this, value);
                    toast("تم حفظ نص الرسالة");
                    showMessageTemplates();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showTrustedContacts() {
        setTab("settings");
        clear();
        content.addView(title("الأسماء الموثوقة"));

        LinearLayout add = cardBox();
        add.addView(action("إضافة اسم موثوق", purple, Color.WHITE, v -> showTrustedDialog()));
        add.addView(small("بعد اعتماد الاسم مرة واحدة، أي تحويل لاحق بنفس الاسم الثلاثي سيتم اعتماده تلقائيًا."));
        content.addView(add);

        ArrayList<TrustedContact> list = AppStore.loadTrustedContacts(this);
        for (TrustedContact contact : list) {
            LinearLayout box = cardBox();
            box.addView(tv(contact.fullName, 18, text, true));
            box.addView(small("الاسم الثلاثي: " + contact.tripleName + "\nرقم الإرسال: " + contact.phone));
            box.addView(action("حذف", Color.rgb(82,30,42), Color.WHITE, v -> { AppStore.deleteTrustedContact(this, contact.id); showTrustedContacts(); }));
            content.addView(box);
        }
    }

    private void showTrustedDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(this); name.setHint("الاسم كما يظهر في ONE Cash");
        EditText phone = new EditText(this); phone.setHint("رقم الإرسال"); phone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(name); layout.addView(phone);
        new AlertDialog.Builder(this)
                .setTitle("إضافة اسم موثوق")
                .setView(layout)
                .setPositiveButton("حفظ", (d,w) -> {
                    if (name.getText().toString().trim().isEmpty() || phone.getText().toString().trim().isEmpty()) { toast("الاسم والرقم مطلوبان"); return; }
                    AppStore.addTrustedContact(this, name.getText().toString().trim(), phone.getText().toString().trim());
                    showTrustedContacts();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showManualAddDialog() {
        EditText input = new EditText(this);
        input.setMinLines(7);
        input.setGravity(Gravity.TOP | Gravity.RIGHT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("كل سطر = كرت واحد");
        new AlertDialog.Builder(this)
                .setTitle("إضافة كروت فئة " + selectedAmount)
                .setView(input)
                .setPositiveButton("حفظ", (d,w) -> {
                    ArrayList<String> lines = new ArrayList<>();
                    for (String line : input.getText().toString().split("\\r?\\n")) lines.add(line);
                    int added = AppStore.importCards(this, selectedAmount, lines, "manual");
                    toast("تمت إضافة " + added + " كرت");
                    showHome();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void openTxtFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQ_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == RESULT_OK && data != null) {
            importFromUri(data.getData());
        }
        if (requestCode == REQ_PDF && resultCode == RESULT_OK && data != null) {
            writePdfReport(data.getData(), pendingReportAmount, pendingReportSummaryOnly);
        }
    }

    private void importFromUri(Uri uri) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            reader.close();
            int added = AppStore.importCards(this, selectedAmount, lines, getFileName(uri));
            toast("تم استيراد " + added + " كرت");
            showHome();
        } catch (Exception e) {
            toast("فشل قراءة الملف: " + e.getMessage());
        }
    }

    private String getFileName(Uri uri) {
        String result = "txt";
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
                cursor.close();
            }
        } catch(Exception ignored) {}
        return result;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
