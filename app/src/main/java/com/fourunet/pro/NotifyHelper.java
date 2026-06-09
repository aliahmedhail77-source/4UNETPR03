package com.fourunet.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

class NotifyHelper {
    private static final String CHANNEL_ID = "card_sales";
    private static final String CHANNEL_NAME = "إشعارات بيع الكروت";

    static void notifyCardSold(Context context, int amount, String code, String phone) {
        String title = "تم بيع كرت فئة " + amount + " ريال";
        String body = "رقم الكرت: " + code + "\nالعميل: " + (phone == null || phone.isEmpty() ? "-" : phone) + "\nالشبكة: فور يو نت";
        show(context, title, body);
    }

    static void notifySendFailed(Context context, int amount, String code, String phone) {
        String title = "فشل إرسال كرت فئة " + amount + " ريال";
        String body = "تم حجز الكرت: " + code + "\nالعميل: " + (phone == null || phone.isEmpty() ? "-" : phone) + "\nراجع السجل لإعادة المعالجة يدويًا.";
        show(context, title, body);
    }

    private static void show(Context context, String title, String body) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }

            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(context, 0, open, flags);

            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL_ID)
                    : new Notification.Builder(context);
            builder.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body.replace("\n", "  "))
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setPriority(Notification.PRIORITY_HIGH);

            nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        } catch (Exception ignored) {}
    }
}
