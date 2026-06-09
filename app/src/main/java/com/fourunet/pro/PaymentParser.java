package com.fourunet.pro;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PaymentParser {
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
            String name = m.group(2).trim();
            if (amount > 0 && !name.isEmpty()) return new ParsedPayment("ONE Cash", amount, "", name);
        }

        Pattern flexible = Pattern.compile("استلمت\\s+(\\d+(?:[\\.,]\\d+)?).*?من\\s+(.+?)\\s+رصيدك", Pattern.DOTALL);
        Matcher mf = flexible.matcher(body);
        if (mf.find()) {
            int amount = toIntAmount(mf.group(1));
            String name = mf.group(2).trim();
            if (amount > 0 && !name.isEmpty()) return new ParsedPayment("ONE Cash", amount, "", name);
        }
        return null;
    }

    private static ParsedPayment parseFallback(String sender, String body) {
        int amount = 0;
        String phone = "";

        Pattern amountPattern = Pattern.compile("(?:مبلغ|اضيف|استلمت)\\s*(\\d+(?:[\\.,]\\d+)?)");
        Matcher ma = amountPattern.matcher(body);
        if (ma.find()) amount = toIntAmount(ma.group(1));

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
