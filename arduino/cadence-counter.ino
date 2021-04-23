#include <avr/sleep.h>

int PIN = 3;
int rpm_max = 150;
int wait = 60 * 1000.0 / rpm_max  ;

void setup()
{
  Serial.begin(9600);
  pinMode(PIN, INPUT_PULLUP); // default High
  //Serial.print(wait); 
  set_sleep_mode(SLEEP_MODE_PWR_DOWN); //スリープモードの設定
  //attachInterrupt(1, wakeUpNow, FALLING); // use interrupt 1 (pin 3) and run function
  //attachInterrupt(1, wakeUpNow, LOW); // use interrupt 1 (pin 3) and run function
}

void loop()
{
  //Serial.print("loop\n"); 
  //https://playground.arduino.cc/Learning/ArduinoSleepCode/
  attachInterrupt(1, wakeUpNow, FALLING); // use interrupt 1 (pin 3) and run function
  sleep_mode();            // here the device is actually put to sleep!!
  // THE PROGRAM CONTINUES FROM HERE AFTER WAKING UP
  //sleep_disable();         // first thing after waking from sleep:
  detachInterrupt(1);
  Serial.print(1); 
  Serial.print("\n");
  delay(wait);  
}

void wakeUpNow(){
  // awake only. do nothing.
  // back to main loop()
}
