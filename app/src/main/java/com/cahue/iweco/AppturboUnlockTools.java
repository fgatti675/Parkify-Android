package com.cahue.iweco;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.List;


public class AppturboUnlockTools {

    public static boolean isAppturboUnlockable(Context context) {
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
        for (PackageInfo pi : packages) {
            if (pi.packageName.equalsIgnoreCase("com.appturbo.appturboCA2015")
                    || pi.packageName.equalsIgnoreCase("com.appturbo.appoftheday2015")
                    || pi.packageName.equalsIgnoreCase("com.appturbo.appofthenight")) {
                return true;
            }
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("appturbo://check"));
        List list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
