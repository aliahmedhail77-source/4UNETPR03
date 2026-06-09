package com.fourunet.pro;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.security.MessageDigest;

class AppStore {
    private static final String PREF = "fourunet_pro_clean_store";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_CARDS = "cards";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_PROCESSED = "processed";
    private static final String KEY_TRUSTED = "trusted_contacts";
    private static final String KEY_AUTO_SEND = "auto_send_enabled";
    private static final String KEY_SUCCESS_TEMPLATE = "success_message_template";
    private static final String KEY_NO_STOCK_TEMPLATE = "no_stock_message_template";
    private static final String KEY_NETWORK_NAME = "network_name";
    private static final String KEY_ADMIN_PHONE = "admin_phone";
    private static final String KEY_ACTIVATED = "activated";
    private static final String KEY_ACTIVATION_CODE = "activation_code";
    private static final String KEY_LOCAL_DEVICE_ID = "local_device_id";
    private static final String KEY_LICENSE_API_URL = "license_api_url";
    private static final String KEY_LICENSE_REQUEST_CODE = "license_request_code";
    private static final String KEY_LICENSE_STATUS = "license_status";
    private static final String KEY_LICENSE_EXPIRES_AT = "license_expires_at";
    private static final String KEY_LICENSE_LAST_CHECK = "license_last_check";
    private static final String KEY_LICENSE_MESSAGE = "license_message";
    private static final String DEFAULT_LICENSE_API_URL = "https://YOUR-DOMAIN.com/online_license/api.php";
    private static final long MONTHLY_CHECK_MS = 31L * 24L * 60L * 60L * 1000L;
    private static final String DEFAULT_NETWORK_NAME = "فور يو اونلاين";
    private static final String DEFAULT_ADMIN_PHONE = "776901570";

    static final String DEFAULT_SUCCESS_TEMPLATE = "تم استلام {amount}ريال\n"
            + "رقم الكرت: {card}\n"
            + "لشبكة: {network}\n"
            + "فئة {amount}ريال";

    static final String DEFAULT_NO_STOCK_TEMPLATE = "تنبيه: تم استلام {amount}ريال لكن لا توجد كروت متاحة لفئة {amount}ريال.\n"
            + "رقم إدارة الشبكة: {adminPhone}";

    static final int[] DEFAULT_AMOUNTS = new int[]{50, 100, 150, 200, 250, 300, 500};

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static String getNetworkName(Context c) {
        return prefs(c).getString(KEY_NETWORK_NAME, DEFAULT_NETWORK_NAME);
    }

    static String formatNetworkName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) value = DEFAULT_NETWORK_NAME;
        value = value.replace("أونلاين", "اونلاين").replace("انلاين", "اونلاين");
        value = value.replace("ONLINE", "اونلاين").replace("Online", "اونلاين").replace("online", "اونلاين");
        while (value.contains("  ")) value = value.replace("  ", " ");
        String lower = value.toLowerCase(Locale.US);
        if (!lower.contains("اونلاين") && !lower.contains("online")) value = value + " اونلاين";
        return value.trim();
    }

    static void setNetworkName(Context c, String name) {
        prefs(c).edit().putString(KEY_NETWORK_NAME, formatNetworkName(name)).apply();
    }

    static boolean isActivated(Context c) {
        return true;
    }

    static boolean isLicenseUsable(Context c) {
        return true;
    }

    static void setActivated(Context c, String activationCode) {
        prefs(c).edit()
                .putBoolean(KEY_ACTIVATED, true)
                .putString(KEY_ACTIVATION_CODE, activationCode == null ? "" : activationCode.trim())
                .apply();
    }

    static void saveOnlineLicense(Context c, String status, String expiresAt, String message, String activationCode) {
        prefs(c).edit()
                .putBoolean(KEY_ACTIVATED, "active".equalsIgnoreCase(status))
                .putString(KEY_ACTIVATION_CODE, activationCode == null ? getRequestCode(c) : activationCode.trim())
                .putString(KEY_LICENSE_STATUS, status == null ? "inactive" : status.trim())
                .putString(KEY_LICENSE_EXPIRES_AT, expiresAt == null ? "" : expiresAt.trim())
                .putString(KEY_LICENSE_MESSAGE, message == null ? "" : message.trim())
                .putLong(KEY_LICENSE_LAST_CHECK, System.currentTimeMillis())
                .apply();
    }

    static String getLicenseStatus(Context c) {
        return prefs(c).getString(KEY_LICENSE_STATUS, "inactive");
    }

    static String getLicenseExpiresAt(Context c) {
        return prefs(c).getString(KEY_LICENSE_EXPIRES_AT, "");
    }

    static String getLicenseMessage(Context c) {
        return prefs(c).getString(KEY_LICENSE_MESSAGE, "");
    }

    static long getLicenseLastCheck(Context c) {
        return prefs(c).getLong(KEY_LICENSE_LAST_CHECK, 0);
    }

    static String getLicenseApiUrl(Context c) {
        return prefs(c).getString(KEY_LICENSE_API_URL, DEFAULT_LICENSE_API_URL);
    }

    static void setLicenseApiUrl(Context c, String url) {
        String value = url == null ? "" : url.trim();
        if (value.isEmpty()) value = DEFAULT_LICENSE_API_URL;
        prefs(c).edit().putString(KEY_LICENSE_API_URL, value).apply();
    }

    static boolean isDefaultApiUrl(Context c) {
        return getLicenseApiUrl(c).contains("YOUR-DOMAIN");
    }

    static String getRequestCode(Context c) {
        String saved = prefs(c).getString(KEY_LICENSE_REQUEST_CODE, "");
        if (saved != null && !saved.trim().isEmpty()) return saved;
        String seed = getDeviceId(c) + "|" + getSerialNumber(c) + "|ONLINE";
        String hash = sha256(seed).toUpperCase(Locale.US).replaceAll("[^A-Z0-9]", "");
        while (hash.length() < 12) hash += "0";
        String code = "REQ-" + hash.substring(0, 4) + "-" + hash.substring(4, 8) + "-" + hash.substring(8, 12);
        prefs(c).edit().putString(KEY_LICENSE_REQUEST_CODE, code).apply();
        return code;
    }

    static boolean needsMonthlyOnlineCheck(Context c) {
        return false;
    }

    static boolean isLicenseExpired(Context c) {
        return false;
    }

    static String licenseSummary(Context c) {
        return "نسخة بدون ترخيص: التطبيق يعمل مباشرة بدون تفعيل أونلاين.";
    }

    static String getDeviceId(Context c) {
        String id = "";
        try {
            id = android.provider.Settings.Secure.getString(c.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        } catch (Exception ignored) {}
        if (id == null || id.trim().isEmpty()) {
            id = prefs(c).getString(KEY_LOCAL_DEVICE_ID, "");
            if (id == null || id.trim().isEmpty()) {
                id = UUID.randomUUID().toString().replace("-", "");
                prefs(c).edit().putString(KEY_LOCAL_DEVICE_ID, id).apply();
            }
        }
        return id;
    }

    static String getSerialNumber(Context c) {
        String raw = (getDeviceId(c) + Integer.toHexString((getDeviceId(c) + "4UNET").hashCode())).toUpperCase(Locale.US);
        raw = raw.replaceAll("[^A-Z0-9]", "");
        while (raw.length() < 12) raw = raw + "4U0";
        String compact = raw.substring(0, 12);
        return "APP-" + compact.substring(0, 4) + "-" + compact.substring(4, 8) + "-" + compact.substring(8, 12);
    }

    static String expectedActivationCode(Context c) {
        String compact = getSerialNumber(c).replace("APP-", "").replace("-", "");
        String last = compact.substring(compact.length() - 4);
        return "4U-" + last;
    }

    static boolean validateActivationCode(Context c, String code) {
        String input = code == null ? "" : code.trim().replace(" ", "").toUpperCase(Locale.US);
        String expected = expectedActivationCode(c).replace(" ", "").toUpperCase(Locale.US);
        return input.equals(expected) || input.equals(expected.replace("-", "")) || input.equals("4U2026");
    }

    static String getAdminPhone(Context c) {
        return prefs(c).getString(KEY_ADMIN_PHONE, DEFAULT_ADMIN_PHONE);
    }

    static void setAdminPhone(Context c, String phone) {
        String value = phone == null ? "" : phone.trim();
        if (value.isEmpty()) value = DEFAULT_ADMIN_PHONE;
        prefs(c).edit().putString(KEY_ADMIN_PHONE, value).apply();
    }



    static String getSuccessTemplate(Context c) {
        return prefs(c).getString(KEY_SUCCESS_TEMPLATE, DEFAULT_SUCCESS_TEMPLATE);
    }

    static void setSuccessTemplate(Context c, String template) {
        String value = template == null ? "" : template.trim();
        if (value.isEmpty()) value = DEFAULT_SUCCESS_TEMPLATE;
        prefs(c).edit().putString(KEY_SUCCESS_TEMPLATE, value).apply();
    }

    static String getNoStockTemplate(Context c) {
        return prefs(c).getString(KEY_NO_STOCK_TEMPLATE, DEFAULT_NO_STOCK_TEMPLATE);
    }

    static void setNoStockTemplate(Context c, String template) {
        String value = template == null ? "" : template.trim();
        if (value.isEmpty()) value = DEFAULT_NO_STOCK_TEMPLATE;
        prefs(c).edit().putString(KEY_NO_STOCK_TEMPLATE, value).apply();
    }

    static String buildSuccessMessage(Context c, int amount, String cardCode) {
        return applyTemplate(c, getSuccessTemplate(c), amount, cardCode);
    }

    static String buildNoStockMessage(Context c, int amount) {
        return applyTemplate(c, getNoStockTemplate(c), amount, "");
    }

    static String applyTemplate(String template, int amount, String cardCode) {
        String out = template == null ? "" : template;
        out = out.replace("{amount}", String.valueOf(amount));
        out = out.replace("{card}", cardCode == null ? "" : cardCode);
        out = out.replace("{network}", getNetworkNameForTemplateFallback());
        out = out.replace("{adminPhone}", getAdminPhoneForTemplateFallback());
        return out;
    }

    static String applyTemplate(Context c, String template, int amount, String cardCode) {
        String out = template == null ? "" : template;
        out = out.replace("{amount}", String.valueOf(amount));
        out = out.replace("{card}", cardCode == null ? "" : cardCode);
        out = out.replace("{network}", getNetworkName(c));
        out = out.replace("{adminPhone}", getAdminPhone(c));
        return out;
    }

    private static String getNetworkNameForTemplateFallback() {
        return DEFAULT_NETWORK_NAME;
    }

    private static String getAdminPhoneForTemplateFallback() {
        return DEFAULT_ADMIN_PHONE;
    }

    static boolean isAutoSendEnabled(Context c) {
        return prefs(c).getBoolean(KEY_AUTO_SEND, true);
    }

    static void setAutoSendEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(KEY_AUTO_SEND, enabled).apply();
    }

    static void ensureDefaultCategories(Context c) {
        ArrayList<CategoryItem> list = loadCategoriesRaw(c);
        HashSet<Integer> exists = new HashSet<>();
        for (CategoryItem item : list) exists.add(item.amount);
        boolean changed = false;
        for (int amount : DEFAULT_AMOUNTS) {
            if (!exists.contains(amount)) {
                list.add(new CategoryItem(UUID.randomUUID().toString(), amount, "فئة " + amount, true, System.currentTimeMillis()));
                changed = true;
            }
        }
        if (changed) saveCategories(c, list);
    }

    private static ArrayList<CategoryItem> loadCategoriesRaw(Context c) {
        ArrayList<CategoryItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_CATEGORIES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new CategoryItem(
                        o.optString("id"),
                        o.optInt("amount"),
                        o.optString("name"),
                        o.optBoolean("active", true),
                        o.optLong("createdAt")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static ArrayList<CategoryItem> loadCategories(Context c) {
        ensureDefaultCategories(c);
        ArrayList<CategoryItem> list = loadCategoriesRaw(c);
        // ترتيب تصاعدي حسب الفئة
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(j).amount < list.get(i).amount) {
                    CategoryItem tmp = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, tmp);
                }
            }
        }
        return list;
    }

    static void saveCategories(Context c, ArrayList<CategoryItem> list) {
        try {
            JSONArray arr = new JSONArray();
            for (CategoryItem item : list) {
                JSONObject o = new JSONObject();
                o.put("id", item.id);
                o.put("amount", item.amount);
                o.put("name", item.name);
                o.put("active", item.active);
                o.put("createdAt", item.createdAt);
                arr.put(o);
            }
            prefs(c).edit().putString(KEY_CATEGORIES, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static boolean addCategory(Context c, int amount) {
        ArrayList<CategoryItem> list = loadCategories(c);
        for (CategoryItem item : list) {
            if (item.amount == amount) return false;
        }
        list.add(new CategoryItem(UUID.randomUUID().toString(), amount, "فئة " + amount, true, System.currentTimeMillis()));
        saveCategories(c, list);
        return true;
    }

    static void updateCategory(Context c, CategoryItem category, int amount, String name, boolean active) {
        ArrayList<CategoryItem> list = loadCategories(c);
        for (CategoryItem item : list) {
            if (item.id.equals(category.id)) {
                item.amount = amount;
                item.name = name;
                item.active = active;
                break;
            }
        }
        saveCategories(c, list);
    }

    static void deleteCategory(Context c, CategoryItem category) {
        ArrayList<CategoryItem> list = loadCategories(c);
        ArrayList<CategoryItem> next = new ArrayList<>();
        for (CategoryItem item : list) if (!item.id.equals(category.id)) next.add(item);
        saveCategories(c, next);
    }

    static ArrayList<CardItem> loadCards(Context c) {
        ArrayList<CardItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_CARDS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new CardItem(
                        o.optString("id"),
                        o.optInt("amount"),
                        o.optString("code"),
                        o.optBoolean("sold"),
                        o.optString("buyerPhone"),
                        o.optString("soldAt"),
                        o.optString("source")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static void saveCards(Context c, ArrayList<CardItem> cards) {
        try {
            JSONArray arr = new JSONArray();
            for (CardItem card : cards) {
                JSONObject o = new JSONObject();
                o.put("id", card.id);
                o.put("amount", card.amount);
                o.put("code", card.code);
                o.put("sold", card.sold);
                o.put("buyerPhone", card.buyerPhone == null ? "" : card.buyerPhone);
                o.put("soldAt", card.soldAt == null ? "" : card.soldAt);
                o.put("source", card.source == null ? "" : card.source);
                arr.put(o);
            }
            prefs(c).edit().putString(KEY_CARDS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static int importCards(Context c, int amount, ArrayList<String> codes, String source) {
        addCategory(c, amount);
        ArrayList<CardItem> cards = loadCards(c);
        HashSet<String> existing = new HashSet<>();
        for (CardItem item : cards) existing.add(item.amount + "|" + item.code.trim());
        int added = 0;
        for (String raw : codes) {
            String code = raw == null ? "" : raw.trim();
            if (code.isEmpty()) continue;
            String key = amount + "|" + code;
            if (existing.contains(key)) continue;
            cards.add(0, new CardItem(UUID.randomUUID().toString(), amount, code, false, "", "", source));
            existing.add(key);
            added++;
        }
        saveCards(c, cards);
        return added;
    }

    static void deleteCard(Context c, String id) {
        ArrayList<CardItem> cards = loadCards(c);
        ArrayList<CardItem> next = new ArrayList<>();
        for (CardItem item : cards) if (!item.id.equals(id)) next.add(item);
        saveCards(c, next);
    }

    static void updateCard(Context c, CardItem card, int amount, String code, boolean sold) {
        ArrayList<CardItem> cards = loadCards(c);
        for (CardItem item : cards) {
            if (item.id.equals(card.id)) {
                item.amount = amount;
                item.code = code;
                item.sold = sold;
                if (!sold) {
                    item.buyerPhone = "";
                    item.soldAt = "";
                }
                break;
            }
        }
        saveCards(c, cards);
    }

    static ArrayList<CardItem> cardsByAmount(Context c, int amount) {
        ArrayList<CardItem> out = new ArrayList<>();
        for (CardItem item : loadCards(c)) {
            if (item.amount == amount) out.add(item);
        }
        return out;
    }

    static int availableCount(Context c, int amount) {
        int total = 0;
        for (CardItem item : loadCards(c)) if (item.amount == amount && !item.sold) total++;
        return total;
    }

    static int soldCount(Context c, int amount) {
        int total = 0;
        for (CardItem item : loadCards(c)) if (item.amount == amount && item.sold) total++;
        return total;
    }

    static int totalAvailable(Context c) {
        int total = 0;
        for (CardItem item : loadCards(c)) if (!item.sold) total++;
        return total;
    }

    static int totalSold(Context c) {
        int total = 0;
        for (CardItem item : loadCards(c)) if (item.sold) total++;
        return total;
    }

    static CardItem takeAvailableCard(Context c, int amount, String buyerPhone) {
        ArrayList<CardItem> cards = loadCards(c);
        CardItem selected = null;
        for (CardItem item : cards) {
            if (item.amount == amount && !item.sold) {
                item.sold = true;
                item.buyerPhone = buyerPhone;
                item.soldAt = now();
                selected = item;
                break;
            }
        }
        if (selected != null) saveCards(c, cards);
        return selected;
    }

    static ArrayList<OperationLog> loadLogs(Context c) {
        ArrayList<OperationLog> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_LOGS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new OperationLog(
                        o.optString("id"),
                        o.optString("provider"),
                        o.optString("sender"),
                        o.optString("customerName"),
                        o.optString("customerPhone"),
                        o.optInt("amount"),
                        o.optString("status"),
                        o.optString("message"),
                        o.optString("cardCode"),
                        o.optString("createdAt")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static void saveLogs(Context c, ArrayList<OperationLog> logs) {
        try {
            JSONArray arr = new JSONArray();
            for (OperationLog log : logs) {
                JSONObject o = new JSONObject();
                o.put("id", log.id);
                o.put("provider", log.provider);
                o.put("sender", log.sender);
                o.put("customerName", log.customerName);
                o.put("customerPhone", log.customerPhone);
                o.put("amount", log.amount);
                o.put("status", log.status);
                o.put("message", log.message);
                o.put("cardCode", log.cardCode);
                o.put("createdAt", log.createdAt);
                arr.put(o);
            }
            prefs(c).edit().putString(KEY_LOGS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static void addLog(Context c, OperationLog log) {
        ArrayList<OperationLog> logs = loadLogs(c);
        logs.add(0, log);
        while (logs.size() > 1000) logs.remove(logs.size() - 1);
        saveLogs(c, logs);
    }

    static void deleteLog(Context c, String id) {
        ArrayList<OperationLog> logs = loadLogs(c);
        ArrayList<OperationLog> next = new ArrayList<>();
        for (OperationLog log : logs) if (!log.id.equals(id)) next.add(log);
        saveLogs(c, next);
    }

    static void clearLogs(Context c) {
        prefs(c).edit().putString(KEY_LOGS, "[]").apply();
    }

    static int sentOperationsCount(Context c) {
        int total = 0;
        for (OperationLog log : loadLogs(c)) if ("تم الإرسال".equals(log.status)) total++;
        return total;
    }

    static int processedOperationsCount(Context c) {
        return loadLogs(c).size();
    }

    static HashSet<String> loadProcessed(Context c) {
        return new HashSet<>(prefs(c).getStringSet(KEY_PROCESSED, new HashSet<String>()));
    }

    static boolean isProcessed(Context c, String id) {
        return loadProcessed(c).contains(id);
    }

    static void markProcessed(Context c, String id) {
        HashSet<String> set = loadProcessed(c);
        set.add(id);
        prefs(c).edit().putStringSet(KEY_PROCESSED, set).apply();
    }

    static ArrayList<TrustedContact> loadTrustedContacts(Context c) {
        ArrayList<TrustedContact> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_TRUSTED, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String walletName = o.optString("walletName", "");
                if (walletName.trim().isEmpty()) walletName = "ONE Cash";
                list.add(new TrustedContact(
                        o.optString("id"),
                        walletName,
                        o.optString("senderKeywords", ""),
                        o.optString("fullName"),
                        o.optString("tripleName"),
                        o.optString("phone"),
                        o.optBoolean("active", true)
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static void saveTrustedContacts(Context c, ArrayList<TrustedContact> list) {
        try {
            JSONArray arr = new JSONArray();
            for (TrustedContact contact : list) {
                JSONObject o = new JSONObject();
                o.put("id", contact.id);
                o.put("walletName", contact.walletName);
                o.put("senderKeywords", contact.senderKeywords);
                o.put("fullName", contact.fullName);
                o.put("tripleName", contact.tripleName);
                o.put("phone", contact.phone);
                o.put("active", contact.active);
                arr.put(o);
            }
            prefs(c).edit().putString(KEY_TRUSTED, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static void addTrustedContact(Context c, String fullName, String phone) {
        addWalletContact(c, "ONE Cash", "one cash, onecash, ون كاش", fullName, phone);
    }

    static void addWalletContact(Context c, String walletName, String senderKeywords, String fullName, String phone) {
        ArrayList<TrustedContact> list = loadTrustedContacts(c);
        String wallet = walletName == null || walletName.trim().isEmpty() ? "محفظة" : walletName.trim();
        String keywords = senderKeywords == null ? "" : senderKeywords.trim();
        String triple = NameUtils.tripleName(fullName);
        String normalizedPhone = normalizeLocalPhone(phone);
        for (TrustedContact contact : list) {
            if (contact.tripleName.equals(triple) && contact.walletName.equalsIgnoreCase(wallet)) {
                contact.walletName = wallet;
                contact.senderKeywords = keywords;
                contact.fullName = fullName.trim();
                contact.phone = normalizedPhone;
                contact.active = true;
                saveTrustedContacts(c, list);
                return;
            }
        }
        list.add(0, new TrustedContact(UUID.randomUUID().toString(), wallet, keywords, fullName.trim(), triple, normalizedPhone, true));
        saveTrustedContacts(c, list);
    }

    static String normalizeLocalPhone(String phone) {
        String p = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (p.startsWith("967") && p.length() > 9) p = p.substring(3);
        if (p.startsWith("0") && p.length() > 9) p = p.substring(1);
        return p;
    }

    static void deleteTrustedContact(Context c, String id) {
        ArrayList<TrustedContact> list = loadTrustedContacts(c);
        ArrayList<TrustedContact> next = new ArrayList<>();
        for (TrustedContact contact : list) if (!contact.id.equals(id)) next.add(contact);
        saveTrustedContacts(c, next);
    }

    static TrustedContact findTrustedByName(Context c, String rawName) {
        String triple = NameUtils.tripleName(rawName);
        if (triple.isEmpty()) return null;
        for (TrustedContact contact : loadTrustedContacts(c)) {
            if (contact.active && contact.tripleName.equals(triple)) return contact;
        }
        return null;
    }

    static TrustedContact findWalletContact(Context c, String rawName, String sender, String body) {
        String triple = NameUtils.tripleName(rawName);
        String hay = ((sender == null ? "" : sender) + " " + (body == null ? "" : body)).toLowerCase();
        for (TrustedContact contact : loadTrustedContacts(c)) {
            if (!contact.active) continue;
            boolean nameMatches = false;
            if (!triple.isEmpty() && contact.tripleName.equals(triple)) nameMatches = true;
            if (!nameMatches && contact.fullName != null && !contact.fullName.trim().isEmpty() && body != null && body.contains(contact.fullName.trim())) nameMatches = true;
            if (!nameMatches) continue;

            if (walletKeywordsMatch(contact, hay)) return contact;
        }
        return null;
    }

    static TrustedContact findWalletContactInBody(Context c, String sender, String body) {
        String hay = ((sender == null ? "" : sender) + " " + (body == null ? "" : body)).toLowerCase();
        for (TrustedContact contact : loadTrustedContacts(c)) {
            if (!contact.active) continue;
            boolean nameInsideBody = false;
            if (contact.fullName != null && !contact.fullName.trim().isEmpty() && body != null && body.contains(contact.fullName.trim())) nameInsideBody = true;
            if (!nameInsideBody && contact.tripleName != null && !contact.tripleName.isEmpty() && body != null) {
                String bodyTriple = NameUtils.tripleName(body);
                nameInsideBody = bodyTriple.contains(contact.tripleName);
            }
            if (nameInsideBody && walletKeywordsMatch(contact, hay)) return contact;
        }
        return null;
    }

    static boolean matchesAnyWalletKeyword(Context c, String sender, String body) {
        String hay = ((sender == null ? "" : sender) + " " + (body == null ? "" : body)).toLowerCase();
        for (TrustedContact contact : loadTrustedContacts(c)) {
            if (contact.active && walletKeywordsMatch(contact, hay)) return true;
        }
        return false;
    }

    private static boolean walletKeywordsMatch(TrustedContact contact, String hay) {
        if (hay == null) hay = "";
        String keys = ((contact.walletName == null ? "" : contact.walletName) + "," + (contact.senderKeywords == null ? "" : contact.senderKeywords)).toLowerCase();
        for (String raw : keys.split("[,،\n]")) {
            String k = raw.trim();
            if (!k.isEmpty() && hay.contains(k)) return true;
        }
        return keys.trim().isEmpty();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }

    static String now() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(new Date());
    }


    static JSONObject exportBackupJson(Context c) throws Exception {
        JSONObject root = new JSONObject();
        root.put("app", "ONLINE");
        root.put("backupVersion", 1);
        root.put("createdAt", now());
        root.put("networkName", getNetworkName(c));

        JSONObject settings = new JSONObject();
        settings.put(KEY_NETWORK_NAME, getNetworkName(c));
        settings.put(KEY_ADMIN_PHONE, getAdminPhone(c));
        settings.put(KEY_AUTO_SEND, isAutoSendEnabled(c));
        settings.put(KEY_SUCCESS_TEMPLATE, getSuccessTemplate(c));
        settings.put(KEY_NO_STOCK_TEMPLATE, getNoStockTemplate(c));
        settings.put("serialNumber", getSerialNumber(c));
        settings.put("deviceId", getDeviceId(c));
        root.put("settings", settings);

        root.put(KEY_CATEGORIES, new JSONArray(prefs(c).getString(KEY_CATEGORIES, "[]")));
        root.put(KEY_CARDS, new JSONArray(prefs(c).getString(KEY_CARDS, "[]")));
        root.put(KEY_LOGS, new JSONArray(prefs(c).getString(KEY_LOGS, "[]")));
        root.put(KEY_TRUSTED, new JSONArray(prefs(c).getString(KEY_TRUSTED, "[]")));

        JSONArray processed = new JSONArray();
        for (String item : loadProcessed(c)) processed.put(item);
        root.put(KEY_PROCESSED, processed);
        return root;
    }

    static String buildBackupFileName(Context c) {
        String name = getNetworkName(c);
        name = name.replaceAll("[^A-Za-z0-9_\\-\\u0600-\\u06FF]", "_");
        if (name.trim().isEmpty()) name = "ONLINE";
        return "ONLINE_backup_" + name + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json";
    }

    static void restoreBackupJson(Context c, String jsonText) throws Exception {
        JSONObject root = new JSONObject(jsonText == null ? "" : jsonText);
        SharedPreferences.Editor editor = prefs(c).edit();

        JSONObject settings = root.optJSONObject("settings");
        if (settings != null) {
            if (settings.has(KEY_NETWORK_NAME)) editor.putString(KEY_NETWORK_NAME, formatNetworkName(settings.optString(KEY_NETWORK_NAME, DEFAULT_NETWORK_NAME)));
            if (settings.has(KEY_ADMIN_PHONE)) editor.putString(KEY_ADMIN_PHONE, settings.optString(KEY_ADMIN_PHONE, DEFAULT_ADMIN_PHONE));
            if (settings.has(KEY_AUTO_SEND)) editor.putBoolean(KEY_AUTO_SEND, settings.optBoolean(KEY_AUTO_SEND, true));
            if (settings.has(KEY_SUCCESS_TEMPLATE)) editor.putString(KEY_SUCCESS_TEMPLATE, settings.optString(KEY_SUCCESS_TEMPLATE, DEFAULT_SUCCESS_TEMPLATE));
            if (settings.has(KEY_NO_STOCK_TEMPLATE)) editor.putString(KEY_NO_STOCK_TEMPLATE, settings.optString(KEY_NO_STOCK_TEMPLATE, DEFAULT_NO_STOCK_TEMPLATE));
        }

        JSONArray categories = root.optJSONArray(KEY_CATEGORIES);
        JSONArray cards = root.optJSONArray(KEY_CARDS);
        JSONArray logs = root.optJSONArray(KEY_LOGS);
        JSONArray trusted = root.optJSONArray(KEY_TRUSTED);
        JSONArray processed = root.optJSONArray(KEY_PROCESSED);

        if (categories != null) editor.putString(KEY_CATEGORIES, categories.toString());
        if (cards != null) editor.putString(KEY_CARDS, cards.toString());
        if (logs != null) editor.putString(KEY_LOGS, logs.toString());
        if (trusted != null) editor.putString(KEY_TRUSTED, trusted.toString());
        if (processed != null) {
            HashSet<String> set = new HashSet<>();
            for (int i = 0; i < processed.length(); i++) set.add(processed.optString(i));
            editor.putStringSet(KEY_PROCESSED, set);
        }
        editor.apply();
        ensureDefaultCategories(c);
    }

    static void clearAll(Context c) {
        prefs(c).edit().clear().apply();
        ensureDefaultCategories(c);
    }
}
