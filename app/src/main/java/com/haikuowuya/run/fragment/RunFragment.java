package com.haikuowuya.run.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.haikuowuya.run.R;
import com.haikuowuya.run.activity.RunActivity;
import com.haikuowuya.run.activity.RunRecordActivity;
import com.haikuowuya.run.model.bean.RunRecord;
import com.haikuowuya.run.utils.GeneralUtil;

import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.listener.FindListener;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.

 * create an instance of this fragment.
 */
public class RunFragment extends Fragment implements View.OnClickListener {
    private static final int request_code_from_friend = 0x11;
    private Button startBtn;
    private TextView ditanceText;
    private TextView timeText;
    private TextView scoreNumberText;

    private double totalDistance = 0.0; //总距离

    private int totalTime = 0; //总时间

    private int totalCount = 0; //次数


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RunFragment.
     */

    public static RunFragment newInstance() {
        RunFragment fragment = new RunFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_run, container, false);
        initView(view);
        queryData();
        return view;
    }

    /**
     * 初始化组件
     *
     * @param view
     */
    private void initView(View view) {
        startBtn = (Button) view.findViewById(R.id.main_run_start_btn);
        ditanceText = (TextView) view.findViewById(R.id.main_run_data_distance);
        timeText = (TextView) view.findViewById(R.id.main_run_data_time);
        scoreNumberText = (TextView) view.findViewById(R.id.main_run_data_score);
        scoreNumberText.setOnClickListener(this);
        startBtn.setOnClickListener(this);
    }


    private void setView() {
        ditanceText.setText(GeneralUtil.doubleToString(totalDistance));
        timeText.setText(GeneralUtil.secondsToHourString(totalTime));
        scoreNumberText.setText(String.valueOf(totalCount));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_run_start_btn:
                Intent runIntent = new Intent(getActivity(), RunActivity.class);
                startActivityForResult(runIntent, request_code_from_friend);
                break;
            case R.id.main_run_data_score:
                Intent recordIntent = new Intent(getActivity(), RunRecordActivity.class);
                startActivityForResult(recordIntent, request_code_from_friend);
                break;
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        queryData();
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     * 查询运动数据
     */
    private void queryData() {
        BmobQuery<RunRecord> query = new BmobQuery<>();
        query.setLimit(50);
        query.findObjects(getActivity(), new FindListener<RunRecord>() {
            @Override
            public void onSuccess(List<RunRecord> list) {
                if (list.size() > 0) {
                    totalCount = 0;
                    totalDistance = 0.0;
                    totalTime = 0;
                    for (RunRecord runRecord : list) {
                        totalTime += runRecord.getTime();
                        totalDistance += runRecord.getDistance();
                    }
                    totalCount = list.size();
                }
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        setView();
                    }
                });
            }
            @Override
            public void onError(int i, String s) {

            }
        });
    }
}
