package com.haikuowuya.run.listener;

import com.baidu.location.BDLocation;

/**
 * 说明：定位发生变化时的回调
 * 文件名称：OnLocationChangeListener
 * 创建者: leo
 * 邮箱: leo.li@qingbao.cn
 * 时间: 2018/6/25 17:32
 * 版本：V1.0.1
 */
public interface OnLocationChangeListener {
    public void onLocationChange(BDLocation bdLocation);
}
