package com.moyu.apptimes;

import cn.bmob.v3.BmobObject;

public class AppTimes extends BmobObject {
    private String time;
    private String count;
    private String others;

    public void setTime(String time) {
        this.time = time;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    public String getTime() {
        return time;
    }

    public String getCount() {
        return count;
    }

    public String getOthers() {
        return others;
    }
}
