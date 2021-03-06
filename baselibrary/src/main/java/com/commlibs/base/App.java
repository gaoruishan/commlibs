/*
 * Copyright (c) 2015, 张涛.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commlibs.base;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.commlibs.base.common.FileUtils;
import com.commlibs.base.common.LogUtil;
import com.commlibs.base.common.SystemTool;
import com.commlibs.base.helper.CockroachCrash;

/**
 * Created by ZhangTao on 10/12/16.
 */
public class App {

	public static  Application INSTANCE;


	public static Uri ENV_URI;
	public static final int ENV_RELEASE = 0;
	public static final int ENV_BETA = 1;
	public static final int ENV_ALPHA = 2;

	static {
		Application app = null;
		try {
			app = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null);
			if (app == null)
				throw new IllegalStateException("Static initialization of Applications must be on main thread.");
		} catch (final Exception e) {
			LogUtil.e("Failed to get current application from AppGlobals." + e.getMessage());
			try {
				app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
			} catch (final Exception ex) {
				LogUtil.e("Failed to get current application from ActivityThread." + e.getMessage());
			}
		} finally {
			INSTANCE = app;
		}
	}

	/**
	 * 获取当前Application
	 * @return Application
	 */
	public static Application get() {
		return INSTANCE;
	}

	/**
	 * 添加Crash日志
	 * @param isDebug 开启/关闭
	 */
	public static void setCrashDebug(boolean isDebug) {
		if (isDebug) {
			CockroachCrash.install(INSTANCE);
		} else {
			CockroachCrash.uninstall();
		}
	}

	/**
	 * 判断是否是debug
	 * @return true/false;
	 */
	public static boolean isDebug() {
		boolean debug = INSTANCE.getApplicationInfo() != null
				&& (INSTANCE.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		LogUtil.e(debug);
		return debug;
	}

	public static int getEnvCode() {
		Cursor cursor = null;
		int env;
		try {
			ENV_URI = Uri.parse(String.format("content://%s/app_info", SystemTool.getMetaData("appmanager")));
			cursor = App.INSTANCE.getContentResolver().query(ENV_URI,
					new String[]{"env_code"}, "package_name=?",
					new String[]{App.INSTANCE.getPackageName()}, null);
			if (cursor == null || !cursor.moveToFirst()) {
				return ENV_RELEASE;
			}

			env = cursor.getInt(0);
		} catch (Exception e) {
			e.printStackTrace();
			return ENV_RELEASE;
		} finally {
			FileUtils.closeIO(cursor);
		}
		return env;
	}


	private static Activity findActivityFrom(final Context context) {
		if (context instanceof Activity)
			return (Activity) context;
		if (context instanceof Application || context instanceof Service)
			return null;
		if (!(context instanceof ContextWrapper))
			return null;
		final Context base_context = ((ContextWrapper) context).getBaseContext();
		if (base_context == context)
			return null;
		return findActivityFrom(base_context);
	}

	public static void startActivity(final Context context, final Intent intent) {
		final Activity activity = findActivityFrom(context);
		if (activity != null) activity.startActivity(intent);
		else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	public static void toast(String msg) {
		Toast.makeText(INSTANCE, msg, Toast.LENGTH_SHORT).show();
	}

	public static void toast(int msgId) {
		Toast.makeText(INSTANCE, msgId, Toast.LENGTH_SHORT).show();
	}

	public static void longToast(String msg) {
		Toast.makeText(INSTANCE, msg, Toast.LENGTH_LONG).show();
	}
}
