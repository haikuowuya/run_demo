package com.haikuowuya.run.activity;

import android.Manifest;
import android.os.Bundle;
import android.view.Window;

import com.haikuowuya.run.R;
import com.haikuowuya.run.fragment.RunFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

import cn.bmob.newim.listener.ObseverListener;


public class MainActivity extends BaseLocationActivity implements ObseverListener {

    private RunFragment runFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (runFragment == null) {
            runFragment = RunFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.frame_container, runFragment).commit();
        }
    }
}
