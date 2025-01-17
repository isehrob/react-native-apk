package be.skyzohlabs.rnapk;

import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Binder;
import android.os.Environment;
import android.app.Activity;
import android.support.v4.content.FileProvider;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.util.jar.JarFile;
import java.io.StringWriter;
import java.io.PrintWriter;

import javax.annotation.Nullable;

public class ReactNativeAPKModule extends ReactContextBaseJavaModule {

  private static final int INSTALL_APP_REQUEST = 54645;

  private final ReactApplicationContext reactContext;

  private Promise installAppPromise;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode == INSTALL_APP_REQUEST) {
        if (installAppPromise != null) {
          // if (resultCode == Activity.RESULT_CANCELED) {
          //   installAppPromise.reject("Installation canceled!!!");
          // } else if (resultCode == Activity.RESULT_OK) {
          //   installAppPromise.resolve("Ok");
          // }
          // Returning `RESULT_CANCELED` all the time
          installAppPromise.resolve(resultCode);
          installAppPromise = null;
        }
      }
    }
  };

  public ReactNativeAPKModule(ReactApplicationContext reactContext) {
    super(reactContext);
    
    this.reactContext = reactContext;
    this.reactContext.addActivityEventListener(mActivityEventListener);
  }

  @Override
  public String getName() {
    return "ReactNativeAPK";
  }

  @ReactMethod
  public void isAppInstalled(String packageName, Callback cb) {
    try {
      PackageInfo pInfo = this.reactContext.getPackageManager().getPackageInfo(packageName,
          PackageManager.GET_ACTIVITIES);

      cb.invoke(true);
    } catch (PackageManager.NameNotFoundException e) {
      cb.invoke(false);
    }
  }

  @ReactMethod
  public void installApp(String packagePath, final Promise promise) {
    
    Activity currentActivity = getCurrentActivity();

    File toInstall = new File(packagePath);
    PackageManager manager = this.reactContext.getPackageManager();
    installAppPromise = promise;

    try {
      if (Build.VERSION.SDK_INT >= 24) {
        String callingPackageName = manager.getNameForUid(Binder.getCallingUid());
        toInstall.setReadable(true, false);
        Uri apkUri = FileProvider.getUriForFile(this.reactContext, callingPackageName+".fileprovider", toInstall);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE, apkUri);
        intent.setDataAndType(apkUri, "application/vnd.android" + ".package-archive");
        // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        currentActivity.startActivityForResult(intent, INSTALL_APP_REQUEST);

      } else {
        // I'm gonna remove this part I guess
        Uri apkUri = Uri.fromFile(toInstall);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.reactContext.startActivity(intent);

      }

    } catch (Exception e) {

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      String sStackTrace = sw.toString();
      // cb.invoke(sStackTrace);
      installAppPromise.reject(sStackTrace);
      installAppPromise = null;
    }
  
  }

  @ReactMethod
  public void uninstallApp(String packageName, Callback cb) {
    Intent intent = new Intent(Intent.ACTION_DELETE);
    intent.setData(Uri.parse("package:" + packageName));
    this.reactContext.startActivity(intent);
    cb.invoke(true);
  }

  @ReactMethod
  public void getAppVersion(String packageName, Callback cb) {
    try {
      PackageInfo pInfo = this.reactContext.getPackageManager().getPackageInfo(packageName, 0);

      cb.invoke(pInfo.versionName);
    } catch (PackageManager.NameNotFoundException e) {
      cb.invoke(false);
    }
  }

  @ReactMethod
  public void getApps(Callback cb) {
    List<PackageInfo> packages = this.reactContext.getPackageManager().getInstalledPackages(0);

    WritableArray ret = new WritableNativeArray();
    for (final PackageInfo p : packages) {
      ret.pushString(p.packageName);
    }
    cb.invoke(ret);
  }

  @ReactMethod
  public void getNonSystemApps(Callback cb) {
    List<PackageInfo> packages = this.reactContext.getPackageManager().getInstalledPackages(0);

    WritableArray ret = new WritableNativeArray();
    for (final PackageInfo p : packages) {
      if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
        ret.pushString(p.packageName);
      }
    }
    cb.invoke(ret);
  }

  @ReactMethod
  public void runApp(String packageName) {
    // TODO: Allow to pass Extra's from react.
    Intent launchIntent = this.reactContext.getPackageManager().getLaunchIntentForPackage(packageName);
    //launchIntent.putExtra("test", "12331");
    this.reactContext.startActivity(launchIntent);
  }

  /*@Override
  public @Nullable Map<String, Object> getConstants() {
      Map<String, Object> constants = new HashMap<>();
  
      constants.put("getApps", getApps());
      constants.put("getNonSystemApps", getNonSystemApps());
      return constants;
  }*/
}
