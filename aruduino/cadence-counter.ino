int PIN = 3;
int HighLow = LOW;
//int mtr = 5.000; // 1回転で進む距離(m)
unsigned long int now = 0;
unsigned long int pre = 0;
int rpm_max = 150;
int wait = 60 * 1000.0 / rpm_max  ;

void setup()
{
  Serial.begin(9600);
  pinMode(PIN, INPUT_PULLUP); // default High
  //Serial.print(wait); 
}

void loop()
{
  now = millis();
  //Serial.print(now);  Serial.print("\n");
  HighLow = digitalRead(PIN);
  if (HighLow == LOW) {
    if (pre > 0) {
      float d = now - pre; // 経過時間 [ms]
      //float km = mtr / 1000.000; // 進んだ距離 [km]
      //float th = d / 3600000.000; // 1000 * 60 * 60; 経過時間 [hour]
      //int spd = km / th; // 速度の計算
      float rpm = 60 * 1000.0 / d;  // RPM
      if ( 0 < rpm and rpm <= rpm_max-1 ) { // 正常な範囲のみ
        //Serial.print(spd); // シリアル出力、これをUSB経由でスマホが読み取る
        Serial.print(rpm); 
        Serial.print("\n");
        delay(wait);
      }
    }
    pre = now;
  }
}
