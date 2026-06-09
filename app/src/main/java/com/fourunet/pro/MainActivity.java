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
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONObject;

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
        if (Build.VERSION.SDK_INT >= 21) getWindow().setStatusBarColor(purpleLight);
        AppStore.ensureDefaultCategories(this);
        requestPermissionsIfNeeded();
        buildLayout();
        showHome();
    }

    private void showActivationScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(bg);
        setContentView(page);

        TextView top = new TextView(this);
        top.setText("ONLINE");
        top.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, dp(24), 0);
        top.setTextSize(22);
        top.setTypeface(appTypeface(true));
        top.setTextColor(Color.WHITE);
        top.setBackgroundColor(purpleLight);
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(56)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(24), dp(30), dp(24), dp(28));
        scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView lock = tv("🔐", 76, purpleLight, false);
        lock.setGravity(Gravity.CENTER);
        body.addView(lock, new LinearLayout.LayoutParams(-1, -2));

        TextView title = tv("تفعيل ONLINE", 30, text, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.setMargins(0, dp(18), 0, dp(8));
        body.addView(title, titleLp);

        TextView note = tv("أرسل كود الطلب للإدارة، وبعد تفعيله من الموقع اضغط تفعيل / تحديث من الإنترنت.", 15, muted, false);
        note.setGravity(Gravity.CENTER);
        body.addView(note);

        LinearLayout info = cardBox();
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(24), 0, dp(16));
        info.setLayoutParams(infoLp);
        info.addView(tv("كود طلب التفعيل", 16, text, true));
        TextView req = tv(AppStore.getRequestCode(this), 20, purpleLight, true);
        req.setGravity(Gravity.LEFT);
        req.setPadding(0, dp(8), 0, dp(10));
        info.addView(req);
        info.addView(separator());
        info.addView(tv("الرقم التسلسلي (Serial Number)", 16, text, true));
        TextView serial = tv(AppStore.getSerialNumber(this), 17, text, false);
        serial.setGravity(Gravity.LEFT);
        serial.setPadding(0, dp(8), 0, dp(10));
        info.addView(serial);
        info.addView(separator());
        info.addView(tv("معرف الجهاز (Device ID)", 16, text, true));
        TextView device = tv(AppStore.getDeviceId(this), 13, text, false);
        device.setGravity(Gravity.LEFT);
        device.setPadding(0, dp(8), 0, 0);
        info.addView(device);
        body.addView(info);

        TextView license = small(AppStore.licenseSummary(this));
        license.setGravity(Gravity.CENTER);
        LinearLayout licenseBox = cardBox();
        licenseBox.addView(tv("حالة الاشتراك", 17, text, true));
        licenseBox.addView(license);
        body.addView(licenseBox);

        EditText networkName = activationInput("اسم الشبكة مثل: فور يو");
        networkName.setText(AppStore.getNetworkName(this));
        body.addView(networkName, new LinearLayout.LayoutParams(-1, dp(58)));

        Space sp1 = new Space(this);
        body.addView(sp1, new LinearLayout.LayoutParams(-1, dp(12)));

        EditText apiUrl = activationInput("رابط سيرفر التفعيل API");
        apiUrl.setText(AppStore.getLicenseApiUrl(this));
        apiUrl.setSingleLine(false);
        apiUrl.setMinLines(1);
        apiUrl.setMaxLines(2);
        body.addView(apiUrl, new LinearLayout.LayoutParams(-1, dp(70)));

        TextView apiHint = small("بعد تجهيز موقع الإدارة نضع هنا رابط api.php. إذا كان الرابط YOUR-DOMAIN فالتفعيل لن يعمل حتى يتم رفع الموقع وتعديل الرابط.");
        apiHint.setGravity(Gravity.RIGHT);
        apiHint.setPadding(0, dp(8), 0, dp(12));
        body.addView(apiHint);

        Button activate = action("تفعيل / تحديث من الإنترنت", purpleLight, Color.rgb(56, 48, 78), v -> {
            String name = networkName.getText().toString().trim();
            String url = apiUrl.getText().toString().trim();
            if (name.isEmpty()) {
                toast("اكتب اسم الشبكة أولًا");
                return;
            }
            if (url.isEmpty() || url.contains("YOUR-DOMAIN")) {
                toast("أدخل رابط سيرفر التفعيل الصحيح أولًا");
                return;
            }
            AppStore.setNetworkName(this, name);
            AppStore.setLicenseApiUrl(this, url);
            performOnlineLicenseCheck(true);
        });
        body.addView(activate, new LinearLayout.LayoutParams(-1, dp(58)));

        Button copyText = action("عرض النص الذي ترسله للإدارة", card2, text, v -> showActivationRequestText());
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(-1, dp(54));
        copyLp.setMargins(0, dp(12), 0, 0);
        body.addView(copyText, copyLp);

        TextView hint = tv("التطبيق سيحتاج اتصالًا بالإنترنت مرة كل شهر لتجديد التحقق. إذا لم يتصل خلال شهر، يتوقف الإرسال التلقائي حتى يتم تحديث الترخيص.", 12, muted, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(14), 0, 0);
        body.addView(hint);
    }

    private void showActivationRequestText() {
        String textToSend = "طلب تفعيل تطبيق ONLINE\n"
                + "اسم الشبكة: " + AppStore.getNetworkName(this) + "\n"
                + "كود الطلب: " + AppStore.getRequestCode(this) + "\n"
                + "Serial: " + AppStore.getSerialNumber(this) + "\n"
                + "Device ID: " + AppStore.getDeviceId(this);
        new AlertDialog.Builder(this)
                .setTitle("أرسل هذا النص للإدارة")
                .setMessage(textToSend)
                .setPositiveButton("تم", null)
                .show();
    }

    private void performOnlineLicenseCheck(boolean fromActivationScreen) {
        toast("جاري الاتصال بسيرفر التفعيل...");
        new Thread(() -> {
            boolean ok = false;
            String msg = "";
            String status = "inactive";
            String expires = "";
            String network = AppStore.getNetworkName(this);
            try {
                String api = AppStore.getLicenseApiUrl(this);
                URL url = new URL(api);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                String data = "action=" + enc("verify")
                        + "&app=" + enc("ONLINE")
                        + "&request_code=" + enc(AppStore.getRequestCode(this))
                        + "&device_id=" + enc(AppStore.getDeviceId(this))
                        + "&serial=" + enc(AppStore.getSerialNumber(this))
                        + "&network_name=" + enc(network);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                writer.write(data);
                writer.flush();
                writer.close();

                InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONObject json = new JSONObject(sb.toString());
                ok = json.optBoolean("ok", false);
                status = json.optString("status", ok ? "active" : "inactive");
                expires = json.optString("expires_at", "");
                msg = json.optString("message", ok ? "تم التحقق من الترخيص" : "لم يتم قبول الترخيص");
                String serverNetwork = json.optString("network_name", "");
                if (!serverNetwork.trim().isEmpty()) network = serverNetwork.trim();
            } catch (Exception e) {
                msg = "تعذر الاتصال بسيرفر التفعيل: " + e.getMessage();
            }

            final boolean finalOk = ok;
            final String finalMsg = msg;
            final String finalStatus = status;
            final String finalExpires = expires;
            final String finalNetwork = network;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalOk && "active".equalsIgnoreCase(finalStatus)) {
                    AppStore.setNetworkName(this, finalNetwork);
                    AppStore.saveOnlineLicense(this, finalStatus, finalExpires, finalMsg, AppStore.getRequestCode(this));
                    toast("تم تفعيل / تحديث الاشتراك بنجاح");
                    requestPermissionsIfNeeded();
                    buildLayout();
                    showHome();
                } else {
                    toast(finalMsg);
                    if (fromActivationScreen) showActivationScreen();
                }
            });
        }).start();
    }

    private String enc(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }


    private EditText activationInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setTextColor(text);
        input.setHintTextColor(muted);
        input.setTextSize(16);
        input.setTypeface(appTypeface(false));
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(bg, dp(8), Color.argb(150, 255,255,255), dp(1)));
        return input;
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

        TextView title = tv(AppStore.getNetworkName(this), 24, Color.WHITE, true);
        title.setGravity(Gravity.RIGHT);
        h.addView(title);

        TextView sub = tv("إدارة وبيع كروت الإنترنت تلقائيًا", 12, Color.argb(230, 255,255,255), false);
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
        b.setTypeface(appTypeface(false));
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

    private Typeface appTypeface(boolean bold) {
        return Typeface.create(bold ? "sans-serif-medium" : "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    private TextView tv(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTypeface(appTypeface(bold));
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
        b.setTypeface(appTypeface(true));
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
        c.drawText("تقرير مبيعات كروت " + AppStore.getNetworkName(this), w - margin, y, p); y += 28;
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


        LinearLayout appName = cardBox();
        appName.addView(tv("اسم الشبكة داخل التطبيق", 17, text, true));
        appName.addView(small("الاسم الحالي: " + AppStore.getNetworkName(this) + "\nإذا كتبت: فور يو، سيحفظه التطبيق تلقائيًا: فور يو اونلاين. اسم التطبيق تحت الأيقونة أصبح: ONLINE."));
        appName.addView(action("تعديل اسم الشبكة", purple, Color.WHITE, v -> showNetworkNameDialog()));
        content.addView(appName);

        LinearLayout auto = cardBox();
        Switch sw = new Switch(this);
        sw.setText("تشغيل الإرسال التلقائي");
        sw.setTextColor(text);
        sw.setTextSize(17);
        sw.setTypeface(appTypeface(true));
        sw.setChecked(AppStore.isAutoSendEnabled(this));
        sw.setOnCheckedChangeListener((b, enabled) -> AppStore.setAutoSendEnabled(this, enabled));
        auto.addView(sw);
        auto.addView(small("الرسائل الموثوقة: Jawali / Jaib / ONE Cash + أي محفظة تضيفها من إدارة المحافظ والأسماء الموثوقة."));
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
        trusted.addView(tv("إدارة المحافظ والأسماء الموثوقة", 17, text, true));
        trusted.addView(small("أضف أي محفظة بنفس نظام ون كاش مثل floosk أو كاش: اسم المحفظة + كلمات التعرف + الاسم الظاهر في رسالة المحفظة + رقم إرسال الكرت."));
        trusted.addView(action("فتح إدارة المحافظ", purple, Color.WHITE, v -> showTrustedContacts()));
        content.addView(trusted);

        LinearLayout danger = cardBox();
        danger.addView(tv("حذف البيانات", 17, text, true));
        danger.addView(action("حذف كل البيانات", Color.rgb(82,30,42), Color.WHITE, v -> new AlertDialog.Builder(this)
                .setTitle("تأكيد")
                .setMessage("هل تريد حذف كل البيانات؟")
                .setPositiveButton("نعم", (d,w) -> { AppStore.clearAll(this); AppStore.ensureDefaultCategories(this); buildLayout(); showHome(); })
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
                .setTitle("تعديل اسم التطبيق والشبكة")
                .setView(input)
                .setPositiveButton("حفظ", (d,w) -> {
                    AppStore.setNetworkName(this, input.getText().toString());
                    toast("تم حفظ الاسم: " + AppStore.getNetworkName(this));
                    buildLayout();
                    showSettings();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }


    private void showApiUrlDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setGravity(Gravity.LEFT);
        input.setText(AppStore.getLicenseApiUrl(this));
        new AlertDialog.Builder(this)
                .setTitle("رابط سيرفر التفعيل")
                .setMessage("ضع رابط ملف api.php بعد رفع موقع الإدارة. مثال: https://domain.com/online_license/api.php")
                .setView(input)
                .setPositiveButton("حفظ", (d,w) -> {
                    AppStore.setLicenseApiUrl(this, input.getText().toString());
                    toast("تم حفظ رابط السيرفر");
                    showSettings();
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
        content.addView(title("المحافظ والأسماء الموثوقة"));

        LinearLayout add = cardBox();
        add.addView(tv("إضافة محفظة أو اسم موثوق", 17, text, true));
        add.addView(small("هذه الصفحة تعرّف التطبيق على المحافظ التي لا تعطي رقم العميل داخل الرسالة. اكتب اسم المحفظة، وكلمات تظهر في المرسل أو نص الرسالة، والاسم كما يظهر في المحفظة، ورقم الزبون الذي سيستلم الكرت."));
        add.addView(action("إضافة محفظة / اسم", purple, Color.WHITE, v -> showTrustedDialog()));
        content.addView(add);

        ArrayList<TrustedContact> list = AppStore.loadTrustedContacts(this);
        if (list.isEmpty()) {
            LinearLayout empty = cardBox();
            empty.addView(small("لا توجد أسماء موثوقة بعد. أضف اسمًا لمحافظ مثل ONE Cash أو floosk أو كاش."));
            content.addView(empty);
        }
        for (TrustedContact contact : list) {
            LinearLayout box = cardBox();
            box.addView(tv(contact.walletName + " - " + contact.fullName, 18, text, true));
            box.addView(small("كلمات التعرف: " + (contact.senderKeywords == null || contact.senderKeywords.trim().isEmpty() ? contact.walletName : contact.senderKeywords)
                    + "\nالاسم الثلاثي: " + contact.tripleName
                    + "\nرقم استلام الكرت: " + contact.phone));
            box.addView(action("حذف", Color.rgb(82,30,42), Color.WHITE, v -> { AppStore.deleteTrustedContact(this, contact.id); showTrustedContacts(); }));
            content.addView(box);
        }

        LinearLayout back = cardBox();
        back.addView(action("رجوع للإعدادات", card2, text, v -> showSettings()));
        content.addView(back);
    }

    private void showTrustedDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView help = small("مثال: اسم المحفظة floosk، كلمات التعرف: floosk, فلوسك، الاسم الظاهر: محمد أحمد علي، رقم استلام الكرت: 77xxxxxxx. يمكن إضافة أكثر من عميل لنفس المحفظة.");
        EditText wallet = new EditText(this); wallet.setHint("اسم المحفظة مثل floosk أو ONE Cash أو كاش"); wallet.setGravity(Gravity.RIGHT); wallet.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText keywords = new EditText(this); keywords.setHint("كلمات التعرف بالرسالة مفصولة بفاصلة مثل floosk, فلوسك, cash"); keywords.setGravity(Gravity.RIGHT); keywords.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText name = new EditText(this); name.setHint("الاسم كما يظهر في رسالة المحفظة"); name.setGravity(Gravity.RIGHT); name.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText phone = new EditText(this); phone.setHint("رقم استلام الكرت"); phone.setGravity(Gravity.RIGHT); phone.setInputType(InputType.TYPE_CLASS_PHONE);

        wallet.setText("ONE Cash");
        keywords.setText("one cash, onecash, ون كاش");

        layout.addView(help);
        layout.addView(wallet);
        layout.addView(keywords);
        layout.addView(name);
        layout.addView(phone);
        new AlertDialog.Builder(this)
                .setTitle("إضافة محفظة / اسم موثوق")
                .setView(layout)
                .setPositiveButton("حفظ", (d,w) -> {
                    if (wallet.getText().toString().trim().isEmpty() || name.getText().toString().trim().isEmpty() || phone.getText().toString().trim().isEmpty()) {
                        toast("اسم المحفظة والاسم الظاهر ورقم الاستلام مطلوبة");
                        return;
                    }
                    AppStore.addWalletContact(this, wallet.getText().toString().trim(), keywords.getText().toString().trim(), name.getText().toString().trim(), phone.getText().toString().trim());
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
