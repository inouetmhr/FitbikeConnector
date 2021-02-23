# FitbikeConnector

- フィットネスバイクのペダル信号を拾ってRPMおよび換算速度を表示するとともに、走行データをFirebase (firestore) に投げるAndroidアプリ
- Firebase からそれを読んで画面に反映させる別のアプリとセットで使うことを想定している
- 例：フィットネスバイクで進んだ分だけGoogle Street Viewの中を移動する

先人の ie4 さんのアプリの改善版 https://qiita.com/ie4/items/130308793444bd98179f

## 仕組み
- 安価なフィットネスバイクのペダル信号は単純な ON/OFF のパルス
- Arduinoでその信号を読み取ってシリアルコンソールに書き出す
  - ペダル信号たいていはステレオミニプラグでフィットネスバイクの表示用コンソールにつながっているので、 そのプラグをコンソールの代わりにArduino に繋げば良い（Arduino上にミニプラグジャックの実装が必要）
- ArduinoとUSBで接続されたAndroidアプリ（このアプリ）がUSBシリアルからペダル信号を読み出し、画面表示するとともにFirebaseにアップロードする

## 必要なもの
- 安価なフィットネスバイク
- Arduino (Aruduino Nano あるいはその互換品が小さいのでおすすめ） 
  - arduino/ 配下の sketch の書き込み
  - PIN3 と GND にバイクからの信号取り出し用のミニプラグジャック等を実装
- Andorid (OS version 8以上）
  - Arduino接続用USBケーブル (USB-C to USB mini-B等）
- Firebase (firestore) 接続用の設定ファイル ( app/google-service.json に配置) 

## 開発環境
- Andorid Studio 4.1.2 で確認
