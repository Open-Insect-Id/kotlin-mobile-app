package org.openinsectid.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URI
import androidx.core.net.toUri

/**
 * And from https://github.com/Elnix90/Dragon-Launcher
 * (https://github.com/Elnix90/Dragon-Launcher/blob/main/app/src/main/java/org/elnix/dragonlauncher/utils/Extentions.kt)
 */


/**
 * Show a toast message with flexible input types
 * @param message Can be a String, StringRes Int, or null
 * @param duration Toast duration (LENGTH_SHORT or LENGTH_LONG)
 */
fun Context.showToast(
    message: Any?,
    duration: Int = Toast.LENGTH_SHORT
) {
    when (message) {
        is String -> {
            if (message.isNotBlank()) {
                Toast.makeText(this, message, duration).show()
            }
        }
        is Int -> {
            try {
                Toast.makeText(this, getString(message), duration).show()
            } catch (_: Exception) {
                // Invalid resource ID, ignore
            }
        }
        else -> {
            // Null or unsupported type, do nothing
        }
    }
}



fun Context.hasInternet(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager

    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false

    return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}


fun webSearch(context: Context, query: String) {
    val encodedQuery = Uri.encode(query)
    val searchUrl = "https://www.duckduckgo.com/search?q=$encodedQuery"

    val intent = Intent(Intent.ACTION_VIEW, searchUrl.toUri()).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

//
//fun Context.copyToClipboard(text: String) {
//    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//    val clipData = ClipData.newPlainText(getString(R.string.app_name), text)
//    clipboardManager.setPrimaryClip(clipData)
//    showToast("")
//}


//
//fun Context.openUrl(url: String) {
//    if (url.isEmpty()) return
//    val intent = Intent(Intent.ACTION_VIEW)
//    intent.data = url.toUri()
//    startActivity(intent)
//}


//
//fun hasAllFilesAccess(context: Context): Boolean {
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        // Android 11+
//        Environment.isExternalStorageManager()
//    } else {
//        // Android 10 and below (uses old READ/WRITE)
//        ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//}
//
//// Request function (requires an Activity or the activity result launcher equivalent)
//fun requestAllFilesAccess(activity: Activity) {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        // Intent to redirect the user to the "All files access" setting
//        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//        val uri = Uri.fromParts("package", activity.packageName, null)
//        intent.data = uri
//        activity.startActivity(intent)
//    }
//    // For older APIs, the standard permission request dialog is used.
//}



//fun Context.hasUriPermission(uri: Uri): Boolean {
//    val perms = contentResolver.persistedUriPermissions
//    return perms.any { it.uri == uri && it.isReadPermission }
//}
//
//
//
//fun openSearch(context: Context) {
//    val intent = Intent(Intent.ACTION_WEB_SEARCH)
//    intent.putExtra(SearchManager.QUERY, "")
//    context.startActivity(intent)
//}

////@SuppressLint("WrongConstant")
//fun expandQuickActionsDrawer(context: Context) {
//    try {
//        //  (Android 12+)
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
////            val statusBarManager = context.getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
////            statusBarManager.expandNotificationsPanel()
////            return
////        }
//
//        // Fall back -> reflection for older versions
//        val statusBarService = context.getSystemService("statusbar")
//        val statusBarManager = Class.forName("android.app.StatusBarManager")
//        val method = statusBarManager.getMethod("expandNotificationsPanel")
//        method.invoke(statusBarService)
//    } catch (_: Exception) {
//        // If all else fails, try to use the notification intent
//        try {
//            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
//            context.startActivity(intent)
//        } catch (e2: Exception) {
//            e2.printStackTrace()
//        }
//    }
//}

//fun openAlarmApp(context: Context) {
//    try {
//        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
//        context.startActivity(intent)
//    } catch (e: Exception) {
//        Log.d("TAG", e.toString())
//    }
//}
//
//fun openCalendar(context: Context) {
//    try {
//        val calendarUri = CalendarContract.CONTENT_URI
//            .buildUpon()
//            .appendPath("time")
//            .build()
//        context.startActivity(Intent(Intent.ACTION_VIEW, calendarUri))
//    } catch (e: Exception) {
//        e.printStackTrace()
//        try {
//            val intent = Intent(Intent.ACTION_MAIN).setClassName(
//                context,
//                "app.cclauncher.helper.FakeHomeActivity"
//            )
//            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
//            context.startActivity(intent)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//}


//
//fun getVersionCode(ctx: Context): Int =
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//        ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode.toInt()
//    } else {
//        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
//    }
