//ListViewに１つのセルの情報(message_item.xmlとMessageRecord)を結びつけるためのクラス
package com.example.ken.gsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.callback.KiiObjectCallBack;

import java.util.List;

//<MessageRecord>はデータクラスMessageRecordのArrayAdapterであることを示している。このアダプターで管理したいデータクラスを記述されば良い。
//ArrayAdapterは元からあるクラス？ extendsは継承
//https://developer.android.com/reference/android/widget/ArrayAdapter.html
//<MessageRecord>はデータクラスの指定、クラスによって違う
public class MessageRecordsAdapter extends ArrayAdapter<MessageRecord> {
    //ImageLoaderがvolleyの型、クラス自体が型、型Javaではという言い方はしない、
    private ImageLoader mImageLoader;

    //アダプターを作成する関数。コンストラクター。クラス名と同じです。
    //newするときに必ずよび出される
    //ContextはUIにかんする
    public MessageRecordsAdapter(Context context) {
        //レイアウトのidmessage_itemのViewを親クラスに設定している
        //R -> resouce、super?
        super(context, R.layout.message_item);
        //キャッシュメモリを確保して画像を取得するクラスを作成。これを使って画像をダウンロードする。Volleyの機能
        mImageLoader = new ImageLoader(VolleyApplication.getInstance().getRequestQueue(), new BitmapLruCache());
    }
    //表示するViewを返します。これがListVewの１つのセルとして表示されます。表示されるたびに実行されます。
    //ViewGroupはあまり使わない、
    //positionは非同期、finalで固定する?書きあたりをgetViewの外に書くような書き方もある
    //                MessageRecord messageRecord = getItem(position);
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //convertViewをチェックし、Viewがないときは新しくViewを作成します。convertViewがセットされている時は未使用なのでそのまま再利用します。メモリーに優しい。
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_item, parent, false);
        }

        //レイアウトにある画像と文字のViewを所得します。
        //findViewById
        //(NetworkImageView) は型変換
        NetworkImageView imageView = (NetworkImageView) convertView.findViewById(R.id.image1);
        TextView textView = (TextView) convertView.findViewById(R.id.text1);

        //webリンクを制御するプログラムはここから
        // TextView に LinkMovementMethod を登録します
        //TextViewをタップした時のイベントリスナー（タップの状況を監視するクラス）を登録します。onTouchにタップした時の処理を記述します。buttonやほかのViewも同じように記述できます。
        //OnTouchListenerを継承した新しいクラスを作る
        textView.setOnTouchListener(new ViewGroup.OnTouchListener() {
            //タップした時の処理
            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                //タップしたのはTextViewなのでキャスト（型の変換）する
                TextView textView = (TextView) view;
                //リンクをタップした時に処理するクラスを作成。AndroidSDKにあるLinkMovementMethodを拡張しています。
                MutableLinkMovementMethod m = new MutableLinkMovementMethod();
                //MutableLinkMovementMethodのイベントリスナーをさらにセットしています。
                m.setOnUrlClickListener(new MutableLinkMovementMethod.OnUrlClickListener() {
                    //リンクをクリックした時の処理
                    public void onUrlClick(TextView v,Uri uri) {
                        Log.d("myurl",uri.toString());//デバッグログを出力します。
                        // Intent のインスタンスを取得する。view.getContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
                        // Actibityにはintentあるがないので、new でIntent作成
                        // WebActivityのインテントをとる
                        // 画面遷移する時はクラスを〜
                        Intent intent = new Intent(view.getContext(), WebActivity.class);

                        // 渡したいデータとキーを指定する。urlという名前でリンクの文字列を渡しています。
                        // 受け取る側と同じキー？にする？
                        intent.putExtra("url", uri.toString());

                        // 遷移先の画面を呼び出す
                        view.getContext().startActivity(intent);

                    }
                });
                //ここからはMutableLinkMovementMethodを使うための処理なので毎回同じ感じ。
                //リンクのチェックを行うため一時的にsetする
                textView.setMovementMethod(m);
                boolean mt = m.onTouchEvent(textView, (Spannable) textView.getText(), event);
                //リンク以外をタッチした場合の処理を、mtを利用して作ることもできる。
                //チェックが終わったので解除する しないと親view(listview)に行けない
                textView.setMovementMethod(null);
                //setMovementMethodを呼ぶとフォーカスがtrueになるのでfalseにする
                textView.setFocusable(false);
                //戻り値がtrueの場合は今のviewで処理、falseの場合は親view(ListView)で処理
                return mt;
            }
        });
        //webリンクを制御するプログラムはここまで


        //表示するセルの位置からデータをMessageRecordのデータを取得します。
        //getItemはもともとArrayAdapterが持っているメソッド
        MessageRecord imageRecord = getItem(position);

        //mImageLoaderを使って画像をダウンロードし、Viewにセットします。
        //非同期なので、ダウンロードに時間が掛かる場合もある→ローディング表示機能の実装
        //setImageUrlはimageViewのメソッド
        imageView.setImageUrl(imageRecord.getImageUrl(), mImageLoader);
        //Viewに文字をセットします。
        textView.setText(imageRecord.getComment());

        //Goodで追加ここから　
        //いいねボタンを得る
        Button buttonView = (Button) convertView.findViewById(R.id.button1);
        //ボタンの文字にいいねの数を追加します。
        buttonView.setText(getContext().getString(R.string.good)+":"+imageRecord.getGoodCount());

        //ボタンを押した時のクリックイベントを定義
        buttonView.setOnClickListener(new View.OnClickListener() {
            //クリックした時
            @Override
            public void onClick(View view) {
                //いいねボタンを得る
                Button buttonView = (Button) view;
                ////タグからどの位置のボタンかを得る
                //int position = (Integer)buttonView.getTag();
                //MessageRecordsAdapterの位置からMessageRecordのデータを得る
                //positionは表示した時の番号
                //getItemはArrayAdapter自体の命令
                //押された位置が知りたい
                MessageRecord messageRecord = getItem(position);
                //messagesのバケット名と_idの値からKiiObjectのuri(データの場所)を得る。参考：http://documentation.kii.com/ja/starts/cloudsdk/cloudoverview/idanduri/
                //更新するためのURI
                Uri objUri = Uri.parse("kiicloud://buckets/" + "messages" + "/objects/" + messageRecord.getId());
                //uriから空のデータを作成
                //通信する準備
                KiiObject object = KiiObject.createByUri(objUri);
                //いいねを＋１する。
                //ここではmessageRecordのgetGoodCountは更新しない、通信が成功するまで保留
                //データを更新、"goodCount"でカラムを指定、変更したいものだけ指定、このあと通信して
                object.set("goodCount", messageRecord.getGoodCount()+ 1);
                //既存の他のデータ(_id,comment,imageUrlなど)はそのままに、goodCountだけが更新される。参考：http://documentation.kii.com/ja/guides/android/managing-data/object-storages/updating/#full_update
                //通信してKiiCloudに保存
                //成功したらメモリー上のmessageRecordのgetGoodCountを更新しないとエラー時に整合性が取れない
                object.save(new KiiObjectCallBack() {
                    //KiiCloudの更新が完了した時
                    @Override
                    public void onSaveCompleted(int token, KiiObject object, Exception exception) {
                        if (exception != null) {
                            //エラーの時
                            return;
                        }
                        //通信が成功した時、以下を実行
                        //MessageRecordsAdapterの位置からMessageRecordのデータを得る
                        //ちょっと前にも同じコードがあるので、本当はグローバル変数に入れていてもいいかも？
                        MessageRecord messageRecord =  getItem(position);
                        //messageRecordのいいねの数を+1する。これでKiiCloudの値とListViewのデータが一致する。
                        messageRecord.setGoodCount(messageRecord.getGoodCount()+1);
                        //データの変更を通知します。
                        //ArrayAdapterのメソッド、変更があった部分をリドロー
                        notifyDataSetChanged();
                        //トーストを表示.Activityのコンテキストが必要なのでgetContext()してる。
                        //ダイアログだと押さないと消えない
                        //最近はポップアップ？
                        Toast.makeText(getContext(), getContext().getString(R.string.good_done), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        //Goodで追加ここまで


        //1つのセルのViewを返します。
        return convertView;
    }
    //データをセットしなおす関数
    //Listは配列
    public void setMessageRecords(List<MessageRecord> objects) {
        //ArrayAdapterを空にする。
        clear();
        //テータの数だけMessageRecordを追加します。
        for(MessageRecord object : objects) {
            add(object);
        }
        //データの変更を通知します。
        //画面に反映
        notifyDataSetChanged();
    }
}
