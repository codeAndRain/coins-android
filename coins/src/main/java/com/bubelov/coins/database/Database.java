package com.bubelov.coins.database;

import android.net.Uri;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:12
 */

public interface Database {
    String AUTHORITY = "com.bubelov.coins";

    interface Merchants extends BaseColumns {
        String TABLE_NAME = "merchants";
        Uri CONTENT_URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, TABLE_NAME));

        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String NAME = "name";
        String DESCRIPTION = "description";
        String PHONE = "phone";
        String WEBSITE = "website";
        String AMENITY = "amenity";
    }

    interface Currencies extends BaseColumns {
        String TABLE_NAME = "currencies";
        Uri CONTENT_URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, TABLE_NAME));

        String NAME = "name";
        String CODE = "code";
        String SHOW_ON_MAP = "show_on_map";
        String PRICE = "price";
        String PRICE_LAST_CHECK = "price_last_check";
    }

    interface CurrenciesMerchants extends BaseColumns {
        String TABLE_NAME = "currencies_merchants";

        String CURRENCY_ID = "currency_id";
        String MERCHANT_ID = "merchant_id";
    }
}