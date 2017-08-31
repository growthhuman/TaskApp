//タスク管理アプリにCategory（カテゴリ）を追加して、ListViewの画面でカテゴリによるTaskの絞り込みをさせるようにしてください。
//
//        下記の要件を満たしてください。
//
//        ●本レッスンで制作した TaskApp プロジェクトを基に制作してください
//        ●TaskクラスにcategoryというStringプロパティを追加してください
//        ●タスク作成画面でcategoryを入力できるようにしてください
//        ●一覧画面に文字列検索用の入力欄を設置し、categoryと合致するTaskのみ絞込み表示させてください
//        要件を満たすものであれば、どのようなものでも構いません。
//        例えば、保存ボタンやキャンセルボタンを作ったりしてみてください。
//
//        ■ヒント
//        以下のRealmのドキュメントを確認しましょう。
//        検索条件を指定する | Realm
//        注意
//        categoryプロパティを追加したあとは、エミュレータのタスク管理アプリを削除してください（以前のデータである *.realm ファイルが残っているため）
//
//        ■発展課題
//        以下は、チャレンジできる方はしてみましょう。
//
//        レッスン内の機能を全て満たしてください（AlarmManager機能などあるかも確認してください）
//        上記のString型のcategoryを、クラスのCategoryへ変更してください
//        追加で、タスク作成画面から遷移する画面を1つ作成してください
//        その画面ではCategory（idとカテゴリ名を持つ）のクラスを作成できるようにしてください
//        タスク作成画面でTaskを作成するときにCategoryを選択できるようにしてください
//        一覧画面でCategoryを選択すると、Categoryに属しているタスクのみ表示されるようにしてください


package jp.techacademy.kenta.imabayashi.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.kenta.imabayashi.taskapp.TASK";


    private Realm mRealm;
    //
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;

    private EditText text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,InputActivity.class);
                startActivity(intent);
            }
        });

        // Realmの設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK,task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する
                final Task task = (Task) parent.getAdapter().getItem(position);

                //　ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id",task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(),TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent  = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alaramManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alaramManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });

                builder.setNegativeButton("CANCEL",null);
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        //検索ボタンの処理を追加する。
        text = (EditText)findViewById(R.id.editText1);


        Button button = (Button)findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                    reloadListViewByCategory(text.getText());
            }

        });


        // アプリ起動時に表示テスト用のタスクを作成する
//        addTaskForTest();

        reloadListView();
    }

    private void reloadListViewByCategory(Editable text) {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        RealmResults<Task> taskRealmResults = mRealm.where(Task.class).equalTo("category", String.valueOf(text)).findAll();
        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
        Log.d("Test", String.valueOf(text));
    }

    private void reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        RealmResults<Task> taskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
    }

    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }

    private void addTaskForTest() {
        Task task = new Task();
        task.setTitle("作業");
        task.setContents("プログラムを書いてPUSHする");
        task.setDate(new Date());
        task.setId(0);
        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(task);
        mRealm.commitTransaction();
    }

}