package com.fourunet.pro;

import android.content.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PaymentParser {
    static boolean trustedSender(Context context, String sender, String body) {
        if (trustedSender(sender)) return true;
        return AppStore.matchesAnyWalletKeyword(context, sender, body);
    }

    static boolean trustedSender(String sender) {
        if (sender == null) return false;
        String s = sender.toLowerCase();
        return s.contains("jawali")
                || s.contains("jaib")
                || s.contains("one cash")
                || s.contains("onecash")
                || sender.contains("جوالي")
                || sender.contains("جيب")
                || sender.contains("ون كاش");
    }

    static ParsedPayment parse(Context context, String sender, String body) {
        if (body == null) return null;

        ParsedPayment p = null;
        if (trustedSender(sender)) {
            p = parseJawali(body);
            if (p != null) return p;

            p = parseJaib(body);
            if (p != null) return p;

            p = parseOneCash(body);
            if (p != null) return p;

            p = parseFallback(sender, body);
            if (p != null) return p;
        }

        p = parseCustomWallet(context, sender, body);
        return p;
    }

    static ParsedPayment parse(String sender, String body) {
        if (!trustedSender(sender) || body == null) return null;

        ParsedPayment p = parseJawali(body);
        if (p != null) return p;

        p = parseJaib(body);
        if (p != null) return p;

        p = parseOneCash(body);
        if (p != null) return p;

        return parseFallback(sender, body);
    }

    private static ParsedPayment parseJawali(String body) {
        Pattern pattern = Pattern.compile("استلمت\\s+مبلغ\\s+(\\d+(?:[\\.,]\\d+)?)\\s*YER\\s+من\\s+(\\d+)");
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            int amount = toIntAmount(m.group(1));
            String phone = m.group(2);
            if (amount > 0 && phone.length() >= 7) return new ParsedPayment("Jawali", amount, phone, "");
        }
        return null;
    }

    private static ParsedPayment parseJaib(String body) {
        Pattern pattern = Pattern.compile("اضيف\\s+(\\d+(?:[\\.,]\\d+)?)\\s*ر\\.?ي.*?من\\s+(.+?)-(\\d+)");
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            int amount = toIntAmount(m.group(1));
            String name = m.group(2).trim();
            String phone = m.group(3).trim();
            if (amount > 0 && phone.length() >= 7) return new ParsedPayment("Jaib", amount, phone, name);
        }
        return null;
    }

    private static ParsedPayment parseOneCash(String body) {
        Pattern pattern = Pattern.compile("استلمت\\s+(\\d+(?:[\\.,]\\d+)?).*?\\n\\s*من\\s+(.+?)\\s*\\n\\s*رصيدك", Pattern.DOTALL);
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            int amount = toIntAmount(m.group(1));
            String name = cleanName(m.group(2));
            if (amount > 0 && !name.isEmpty()) return new ParsedPayment("ONE Cash", amount, "", name);
        }

        Pattern flexible = Pattern.compile("استلمت\\s+(\\d+(?:[\\.,]\\d+)?).*?من\\s+(.+?)\\s+رصيدك", Pattern.DOTALL);
        Matcher mf = flexible.matcher(body);
        if (mf.find()) {
            int amount = toIntAmount(mf.group(1));
            String name = cleanName(mf.group(2));
            if (amount > 0 && !name.isEmpty()) return new ParsedPayment("ONE Cash", amount, "", name);
        }
        return null;
    }

    private static ParsedPayment parseCustomWallet(Context context, String sender, String body) {
        if (!AppStore.matchesAnyWalletKeyword(context, sender, body)) return null;
        int amount = extractAmount(body);
        if (amount <= 0) return null;

        TrustedContact direct = AppStore.findWalletContactInBody(context, sender, body);
        if (direct != null) return new ParsedPayment(direct.walletName, amount, direct.phone, direct.fullName);

        String name = extractNameAfterFrom(body);
        TrustedContact byName = AppStore.findWalletContact(context, name, sender, body);
        if (byName != null) return new ParsedPayment(byName.walletName, amount, byName.phone, byName.fullName);

        if (name != null && !name.trim().isEmpty()) {
            String lowSender = sender == null ? "" : sender.toLowerCase();
            return new ParsedPayment(lowSender.isEmpty() ? "محفظة" : sender, amount, "", cleanName(name));
        }
        return null;
    }

    private static ParsedPayment parseFallback(String sender, String body) {
        int amount = extractAmount(body);
        String phone = "";

        Pattern phonePattern = Pattern.compile("(7\\d{8}|\\d{9,12})");
        Matcher mp = phonePattern.matcher(body);
        String last = "";
        while (mp.find()) last = mp.group(1);
        phone = last;

        if (amount > 0 && phone.length() >= 7) {
            String low = sender == null ? "" : sender.toLowerCase();
            String provider = "Jawali";
            if (low.contains("jaib") || (sender != null && sender.contains("جيب"))) provider = "Jaib";
            if (low.contains("one") || (sender != null && sender.contains("ون كاش"))) provider = "ONE Cash";
            return new ParsedPayment(provider, amount, phone, "");
        }
        return null;
    }

    private static int extractAmount(String body) {
        String[] patterns = new String[]{
                "(?:مبلغ|اضيف|أضيف|استلمت|استلام|استلم|وصل|تحويل)\\s*(\\d+(?:[\\.,]\\d+)?)",
                "(\\d+(?:[\\.,]\\d+)?)\\s*(?:ريال|ر\\.?ي|YER|YR|Rial)"
        };
        for (String pat : patterns) {
            Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(body);
            if (m.find()) {
                int amount = toIntAmount(m.group(1));
                if (amount > 0) return amount;
            }
        }
        return 0;
    }

    private static String extractNameAfterFrom(String body) {
        if (body == null) return "";
        Pattern[] patterns = new Pattern[]{
                Pattern.compile("(?:من|From)\\s*[:：-]?\\s*(.+?)(?:\\n|رصيدك|المبلغ|رقم|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
                Pattern.compile("المرسل\\s*[:：-]?\\s*(.+?)(?:\\n|$)", Pattern.DOTALL),
                Pattern.compile("اسم\\s+المرسل\\s*[:：-]?\\s*(.+?)(?:\\n|$)", Pattern.DOTALL)
        };
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(body);
            if (m.find()) return cleanName(m.group(1));
        }
        return "";
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[0-9٠-٩]+", " ")
                .replaceAll("[\\-_:؛;،,]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static int toIntAmount(String value) {
        try {
            String v = value.replace(",", ".");
            double d = Double.parseDouble(v);
            return (int) Math.round(d);
        } catch (Exception e) {
            return 0;
        }
    }
}
