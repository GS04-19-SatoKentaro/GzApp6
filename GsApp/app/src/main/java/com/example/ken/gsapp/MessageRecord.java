//1つのセルにあるデータを保存するためのデータクラスです。
package com.example.ken.gsapp;

public class MessageRecord {
    //保存するデータ全てを変数で定義します。
    //privateにするとあとで内部だけの変更で済む。
    //typesciriptにもこのような機能が導入されている
    private String imageUrl;
    private String comment;
    private String id;
    private int goodCount;

    //データを１つ作成する関数です。項目が増えたら増やしましょう。
    //コンストラクター、クラス名と同じファンクション
    public MessageRecord(String id, String imageUrl, String comment, int goodCount) {
        this.imageUrl = imageUrl;
        this.comment = comment;
        this.id = id;
        this.goodCount = goodCount;
    }
    //それぞれの項目を返す関数です。項目が増えたら増やしましょう。
    //thisが省略されている
    public String getComment() {
        return comment;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public String getId() {
        return id;
    }
    public int getGoodCount() {return goodCount;}
    public void setGoodCount(int goodCount){this.goodCount = goodCount;}
}
