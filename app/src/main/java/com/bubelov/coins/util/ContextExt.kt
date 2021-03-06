/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import android.view.View
import android.view.inputmethod.InputMethodManager
import timber.log.Timber

fun Context.openUrl(url: String): Boolean {
    val urlBuilder = StringBuilder()

    if (url.startsWith("www.") || !url.contains("http")) {
        urlBuilder.append("http://")
    }

    urlBuilder.append(url)
    val intentBuilder = CustomTabsIntent.Builder()
    intentBuilder.setStartAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
    intentBuilder.setExitAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
    val customTabsIntent = intentBuilder.build()

    return try {
        customTabsIntent.launchUrl(this, Uri.parse(urlBuilder.toString()))
        true
    } catch (e : Exception) {
        Timber.e(e)
        false
    }
}

fun Context.showKeyboard(view: View) {
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(view, 0)
}

fun Context.hideKeyboard(view: View) {
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}