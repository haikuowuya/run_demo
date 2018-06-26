package com.haikuowuya.run.activity;

import android.os.Bundle;

import com.haikuowuya.run.R;
import com.haikuowuya.run.fragment.RunFragment;

import cn.bmob.newim.listener.ObseverListener;


public class MainActivity extends BaseLocationActivity implements ObseverListener {
    private RunFragment runFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (runFragment == null) {
            runFragment = RunFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.frame_container, runFragment).commit();
        }
    }

    @Override
    public boolean isAutoLocation() {
        return true;
    }
}
