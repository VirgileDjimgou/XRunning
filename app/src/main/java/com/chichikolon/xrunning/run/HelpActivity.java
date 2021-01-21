package com.chichikolon.xrunning.run;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.chichikolon.xrunning.run.databinding.HelpBinding;
import com.chichikolon.xrunning.run.util.ViewUtils;

public class HelpActivity extends AbstractActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.makeClickableLinks(findViewById(android.R.id.content));
    }

    @Override
    protected View getRootView() {
        return HelpBinding.inflate(getLayoutInflater()).getRoot();
    }
}
