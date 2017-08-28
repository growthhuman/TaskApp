package jp.techacademy.kenta.imabayashi.taskapp;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by kenta on 2017/08/27.
 */

public class TaskApp extends Application {

    public void onCreate(){
        super.onCreate();
        Realm.init(this);
    }
}
