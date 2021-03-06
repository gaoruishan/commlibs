package com.commlibs.base.list;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.commlibs.base.BaseActivity;
import com.commlibs.base.adapter.RecyclerAdapter;
import com.commlibs.base.list.footer.DefaultFooterTool;
import com.commlibs.base.list.footer.IFooterTool;
import com.commlibs.base.list.refresh.IRefreshTool;
import com.commlibs.base.list.refresh.SwipeRefreshTool;

import java.util.List;

/**
 * Created by on 2016/5/17.
 */
public abstract class SupportRecyclerActivity<T> extends BaseActivity implements IBaseList<T> {

    private ListCompat<T> mListCompat;


    @Override
    public void onCreateActivity(@Nullable Bundle savedInstanceState) {
        mListCompat = ListCompat.with(this, getWindow().getDecorView());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecyclerView.ItemAnimator generateItemAnimator() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecyclerView.ItemDecoration generateItemDecoration() {
        return new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinearLayoutManager generateLayoutManager() {
        return new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public IRefreshTool generateRefreshTool() {
        return new SwipeRefreshTool();
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public IFooterTool generateFooterTool() {
        return new DefaultFooterTool();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RecyclerView getRecyclerView() {
        return mListCompat.mRecyclerView;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public final IRefreshTool getRefreshTool() {
        return mListCompat.mRefreshTool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final IFooterTool getFooterTool() {
        return mListCompat.mFooterTool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final LinearLayoutManager getLayoutManager() {
        return mListCompat.mLayoutManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RecyclerAdapter<T> getAdapter() {
        return mListCompat.mAdapter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newData() {
        mListCompat.newData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getData() {
        mListCompat.getData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSuccess(int pageCount, List<T> data) {
        mListCompat.onSuccess(pageCount, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailed(String msg) {
        mListCompat.onFailed(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reportIsCache(boolean isCache) {
        mListCompat.reportIsCache(isCache);
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public int startPage() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getCurrentPage() {
        return mListCompat.getCurrentPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setCurrentPage(int page) {
        mListCompat.setCurrentPage(page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showFooter() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean refreshEnable() {
        return true;
    }
}
