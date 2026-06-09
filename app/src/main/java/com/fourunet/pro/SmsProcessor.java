package com.fourunet.pro;

import android.content.Context;
import android.telephony.SmsManager;

import java.security.MessageDigest;
import java.util.UUID;

class SmsProcessor {
    static void processIncomingSms(Context context, String sender, String body) {
        if (!AppStore.isAutoSendEnabled(context)) return;
        if (!PaymentParser.trustedSender(sender)) return;

        ParsedPayment payment = PaymentParser.parse(sender, body);
        if (payment == null) {
            addLog(context, "", sender, "", "", 0, "مرفوض", "تعذر فهم الرسالة", "");
            return;
        }

        String eventId = sha256(sender + "|" + body);
        if (AppStore.isProcessed(context, eventId)) return;
        AppStore.markProcessed(context, eventId);

        String receiver = payment.customerPhone;
        String messageNote = "تمت المعالجة تلقائيًا";

        if ((receiver == null || receiver.trim().isEmpty()) && "ONE Cash".equals(payment.provider)) {
            TrustedContact contact = AppStore.findTrustedByName(context, payment.customerName);
            if (contact == null) {
                addLog(context, payment.provider, sender, payment.customerName, "", payment.amount, "معلق",
                        "ONE Cash: الاسم الثلاثي غير موجود في الأسماء الموثوقة: " + NameUtils.tripleName(payment.customerName), "");
                return;
            }
            receiver = contact.phone;
            messageNote = "ONE Cash: تم اعتماد الاسم الثلاثي " + contact.tripleName;
        }

        if (receiver == null || receiver.trim().isEmpty()) {
            addLog(context, payment.provider, sender, payment.customerName, "", payment.amount, "معلق", "لا يوجد رقم إرسال معروف", "");
            return;
        }

        CardItem card = AppStore.takeAvailableCard(context, payment.amount, receiver);
        if (card == null) {
            String noStock = "تم استلام مبلغ " + payment.amount + " ريال، لكن كروت فئة " + payment.amount + " غير متوفرة حاليًا.";
            trySendSms(receiver, noStock);
            addLog(context, payment.provider, sender, payment.customerName, receiver, payment.amount, "معلق", "لا توجد كروت متاحة من نفس الفئة", "");
            return;
        }

        String reply = "تم استلام " + payment.amount + "ريال\n"
                + "رقم الكرت: " + card.code + "\n"
                + "لشبكة: فور يو نت\n"
                + "فئة " + payment.amount + "ريال";

        boolean sent = trySendSms(receiver, reply);

        if (sent) {
            NotifyHelper.notifyCardSold(context, payment.amount, card.code, receiver);
        } else {
            NotifyHelper.notifySendFailed(context, payment.amount, card.code, receiver);
        }

        addLog(context, payment.provider, sender, payment.customerName, receiver, payment.amount,
                sent ? "تم الإرسال" : "فشل الإرسال",
                sent ? messageNote : "تم حجز الكرت لكن فشل إرسال SMS",
                card.code);
    }

    private static boolean trySendSms(String phone, String text) {
        try {
            if (phone == null || phone.trim().isEmpty()) return false;
            SmsManager sms = SmsManager.getDefault();
            if (text.length() > 160) sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null);
            else sms.sendTextMessage(phone, null, text, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void addLog(Context context, String provider, String sender, String name, String phone, int amount, String status, String message, String cardCode) {
        AppStore.addLog(context, new OperationLog(
                UUID.randomUUID().toString(),
                provider == null ? "" : provider,
                sender == null ? "" : sender,
                name == null ? "" : name,
                phone == null ? "" : phone,
                amount,
                status,
                message == null ? "" : message,
                cardCode == null ? "" : cardCode,
                AppStore.now()
        ));
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
}
