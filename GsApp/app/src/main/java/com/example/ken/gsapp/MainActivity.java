//起動時に実行されるアクティビティーです。１つの画面に１つのアクティビティーが必要です。
//どのアクティビティーが起動時に実行されるのかはAndroidManifestに記述されています。
package com.example.ken.gsapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiQueryCallBack;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//MainActivityはContextの子供

public class MainActivity extends ActionBarActivity {

    //アダプタークラスです。
    private MessageRecordsAdapter mAdapter;

    //起動時にOSから実行される関数です。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Userで追加ここから
        //KiiCloudでのログイン状態を取得します。nullの時はログインしていない。
        KiiUser user = KiiUser.getCurrentUser();
        //自動ログインのため保存されているaccess tokenを読み出す。tokenがあればログインできる
        SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
        String token = pref.getString(getString(R.string.save_token), "");//保存されていない時は""
        //ログインしていない時はログインのactivityに遷移.SharedPreferencesが空の時もチェックしないとLogOutできない。
        //「 || token == ""」は不要？
        if(user == null || token == "") {
            // Intent のインスタンスを取得する。getApplicationContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
            Intent intent = new Intent(getApplicationContext(), UserActivity.class);
            // 遷移先の画面を呼び出す
            startActivity(intent);
            //戻れないようにActivityを終了します。
            finish();
        }
        //Userで追加ここまで

        //メイン画面のレイアウトをセットしています。ListView
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);
        // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ボタンがクリックされた時に呼び出されます
                finish();
            }
        });

        //アダプターを作成します。newでクラスをインスタンス化しています。
        //MainActivityはContextの子供
        mAdapter = new MessageRecordsAdapter(this);

        //ListViewのViewを取得
        ListView listView = (ListView) findViewById(R.id.mylist);
        //ListViewにアダプターをセット。
        listView.setAdapter(mAdapter);
        //一覧のデータを作成して表示します。
        fetch();

    }

    //ListView2で追加ここから
    //KiiCLoud対応のfetchです。
    //自分で作った関数です。一覧のデータを作成して表示します。
    private void fetch() {
        //KiiCloudの検索条件を作成。検索条件は未設定。なので全件。
        //queryは条件という意味
        KiiQuery query = new KiiQuery();
        //ソート条件を設定。日付の降順
        //Descは降順
        //_modifiedは修正
        //_ownerはUser
        query.sortByDesc("_created");
        //バケットmessagesを検索する。最大200件
        //new KiiQueryCallBack<KiiObject>〜はコールバック
        Kii.bucket("messages")
                .query(new KiiQueryCallBack<KiiObject>() {
                    //検索が完了した時
                    //resultは結果
                    //exceptionはエラー
                    @Override
                    public void onQueryCompleted(int token, KiiQueryResult<KiiObject> result, Exception exception) {
                        if (exception != null) {
                            //エラー処理を書く
                            //エラーメッセ時の表示灯
                            return;
                        }
                        //空のMessageRecordデータの配列を作成
                        //new ArrayList<MessageRecord>(2)とかすると2つ作る
                        //あとで追加するためにからのやつ作成
                        ArrayList<MessageRecord> records = new ArrayList<MessageRecord>();
                        //検索結果をListで得る
                        //KiiObjectはKiiCloudのオブジェクト
                        List<KiiObject> objLists = result.getResult();
                        //得られたListをMessageRecordに設定する
                        // 「:」で右のリスト？の中の一つをobjに代入
                        //_id, _created, _modified, _owner, _version, _bodyType, imageUrl, comment,
                        for (KiiObject obj : objLists) {
                            //_id(KiiCloudのキー)を得る。空の時は""が得られる。
                            //nullかどうかを意識する。
                            String id = obj.getString("_id", "");
                            String title = obj.getString("comment", "");
                            String url = obj.getString("imageUrl", "");
                            int goodCount = obj.getInt("goodCount", 0);//Goodで修正
                            //MessageRecordを新しく作ります。
                            MessageRecord record = new MessageRecord(id, url, title, goodCount);//Goodで修正
                            //MessageRecordの配列に追加します。
                            records.add(record);
                        }
                        //データをアダプターにセットしています。これで表示されます。
                        mAdapter.setMessageRecords(records);
                    }
                }, query);

    }
    //Postから戻ってくるときに画面を更新したいのでfetchを実行しています。
    @Override
    protected void onStart() {
        super.onStart();
        //一覧のデータを作成して表示します。
        fetch();
        //GrowthHackで追加ここから
        //GAのスクリーン名はアクティビティーの名前を送信します。
        Tracker t = ((VolleyApplication)getApplication()).getTracker(VolleyApplication.TrackerName.APP_TRACKER);
        //画面名前としてクラス名を取得（this.getClass().getSimpleName()）
        t.setScreenName(this.getClass().getSimpleName());
        //GAに送る
        t.send(new HitBuilders.AppViewBuilder().build());
        //GrowthHackで追加ここまで
    }
    //ListView2で追加ここまで


    /*ListView2で削除ここから

    //自分で作った関数です。一覧のデータを作成して表示します。
    private void fetch() {
        //jsonデータをサーバーから取得する通信機能です。Volleyの機能です。通信クラスのインスタンスを作成しているだけです。通信はまだしていません。
        JsonObjectRequest request = new JsonObjectRequest(
                "http://schrift.sakura.ne.jp/gsapp/json.txt" ,//jsonデータが有るサーバーのURLを指定します。引数1
                null,//引数2
                //サーバー通信した結果、成功した時の処理をするクラスを作成しています。//引数3
                //通信したときのハンドリングの仕方が違う
                //一回こっきりのクラス
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        //try catchでエラーを処理します。tryが必要かどうかはtryに記述している関数次第です。
                        try {
                            //jsonデータを下記で定義したparse関数を使いデータクラスにセットしています。
                            List<MessageRecord> messageRecords = parse(jsonObject);
                            //データをアダプターにセットしています。
                            mAdapter.setMessageRecords(messageRecords);
                        }
                        catch(JSONException e) {
                            //トーストを表示、テキストメッセージを出す
                            Toast.makeText(getApplicationContext(), "Unable to parse data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                //通信結果、エラーの時の処理クラスを作成。
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //トーストを表示
                        Toast.makeText(getApplicationContext(), "Unable to fetch data: " + volleyError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        //作成した通信クラスをキュー、待ち行列にいれて適当なタイミングで通信します。
        //VolleyApplicationはnewしていません。これはAndroidManifestで記載しているので起動時に自動的にnewされています。
        VolleyApplication.getInstance().getRequestQueue().add(request);
    }
    //サーバにあるjsonデータをMessageRecordに変換します。
    //throws JSONExceptionは文法的に必要
    private List<MessageRecord> parse(JSONObject json) throws JSONException {
        //空のMessageRecordデータの配列を作成
        ArrayList<MessageRecord> records = new ArrayList<MessageRecord>();
        //jsonデータのmessagesにあるJson配列を取得します。
        JSONArray jsonMessages = json.getJSONArray("messages");
        //配列の数だけ繰り返します。
        for(int i =0; i < jsonMessages.length(); i++) {
            //１つだけ取り出します。
            JSONObject jsonMessage = jsonMessages.getJSONObject(i);
            //jsonの値を取得します。
            String title = jsonMessage.getString("comment");
            String url = jsonMessage.getString("imageUrl");
            //jsonMessageを新しく作ります。
            MessageRecord record = new MessageRecord(url, title);
            //MessageRecordの配列に追加します。
            records.add(record);
        }

        return records;
    }

    ListView2で削除ここまで*/


    //デフォルトで作成されたメニューの関数です。未使用。

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        //Userで追加ここから
        //ログアウト処理.KiiCloudにはログアウト機能はないのでAccesTokenを削除して対応。
        if (id == R.id.log_out) {
            //自動ログインのため保存されているaccess tokenを消す。
            //clear()でフォルダごと削除
            SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
            pref.edit().clear().apply();
            //ログイン画面に遷移
            // Intent のインスタンスを取得する。getApplicationContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
            Intent intent = new Intent(getApplicationContext(), UserActivity.class);
            // 遷移先の画面を呼び出す
            startActivity(intent);
            //戻れないようにActivityを終了します。
            finish();
            return true;
        }
        //Userで追加ここまで

        //Postで追加ここから
        //投稿処理
        if (id == R.id.post) {
            //投稿画面に遷移
            // Intent のインスタンスを取得する。getApplicationContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
            Intent intent = new Intent(getApplicationContext(), PostActivity.class);
            // 遷移先の画面を呼び出す
            startActivity(intent);
            return true;
        }
        //Postで追加ここまで

        return super.onOptionsItemSelected(item);
    }


}
