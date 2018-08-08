package com.gaoruishan.commlibs;

import android.os.Bundle;

import com.commlibs.base.BaseActivity;
import com.commlibs.base.common.LogUtil;


public class MainActivity extends BaseActivity {

	@Override
	public int getContentViewId() {
		return R.layout.activity_main;
	}

	@Override
	public void onCreateActivity(Bundle savedInstanceState) {
		LogUtil.e(this.TAG);
	}

}
