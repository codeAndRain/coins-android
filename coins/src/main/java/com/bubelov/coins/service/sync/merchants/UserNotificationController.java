package com.bubelov.coins.service.sync.merchants;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.ui.activity.MapActivity;
import com.bubelov.coins.util.DistanceUtils;

import java.util.UUID;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 14:46
 */

public class UserNotificationController {
    private Context context;

    public UserNotificationController(Context context) {
        this.context = context;
    }

    public boolean shouldNotifyUser(Merchant merchant) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!preferences.getBoolean(context.getString(R.string.pref_show_new_merchants_key), true)) {
            return false;
        }

        NotificationArea notificationArea = new NotificationAreaProvider(context).get();

        if (notificationArea == null) {
            return false;
        }

        if (DistanceUtils.getDistance(notificationArea.getCenter(), merchant.getPosition()) > notificationArea.getRadiusMeters()) {
            return false;
        }

        App app = (App) context.getApplicationContext();
        SQLiteDatabase db = app.getDatabaseHelper().getReadableDatabase();

        Cursor cursor = db.query(Database.Merchants.TABLE_NAME,
                new String[] { Database.Merchants._ID },
                "_id = ?",
                new String[] { String.valueOf(merchant.getId()) },
                null,
                null,
                null);

        boolean alreadyExists = cursor.getCount() > 0;
        cursor.close();

        return !alreadyExists;

    }

    public void notifyUser(long merchantId, String merchantName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_merchant))
                .setContentText(!TextUtils.isEmpty(merchantName) ? merchantName : context.getString(R.string.notification_new_merchant_no_name))
                .setAutoCancel(true);

        Intent intent = MapActivity.newShowMerchantIntent(context, merchantId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build());
    }
}