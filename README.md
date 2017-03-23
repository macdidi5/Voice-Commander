# Voice Commander

Android Things TTS（Text-To-Speech、文字轉語音）與 STT （Speech-To-Text、語音轉文字）


## 示範影片

[https://youtu.be/-Rv8ZAUHTvU](https://youtu.be/-Rv8ZAUHTvU)

## 需要的設備與零件

* [Raspberry Pi 3 - Model B - ARMv8 with 1G RAM](https://www.adafruit.com/products/3055) (with power supply and MicroSD)
    * Android Things preview 0.2
* [伺服馬達, Tower Pro SG90](http://www.towerpro.com.tw/product/sg90-7/)
* LED
* [麵包板連接線](https://www.adafruit.com/products/153)
* [公母杜邦線](https://www.adafruit.com/products/1954)
* [單色顯示器、0.96吋、128x64](https://www.adafruit.com/products/326)
* 麥克風、喇叭、USB外接耳機與麥克風音效卡

    

* [樂高海灘小屋](http://shop.lego.com/en-US/Beach-Hut-31035)
* 個人電腦，PC或Mac
    * Android Studio
    * Android SDK version 24
    * Build tool version 25.0.2

## 系統設定

**Android Things developer preview 2目前沒有支援語音轉文字，這裡使用的是比較簡易的作法**

1. 在行動電話安裝 [Voice Search App](https://play.google.com/store/apps/details?id=com.google.android.voicesearch)。
2. 連接行動電話到個人電腦，啟動adb。
3. 執行下列的指令，下載Voice Search App到個人電腦：

        adb shell pm path com.google.android.voicesearch
        adb pull /data/app/com.google.android.voicesearch-1/base.apk

4. 為Raspberry Pi連接USB滑鼠與螢幕。
5. 啟動與連接安裝Android Things的Raspberry Pi，執行下列的指令，安裝Voice Search App到Android Things：

        adb install base.apk

6. 執行應用程式。
7. 第一次啟動應用程式，在Raspberry Pi的輸出畫面選擇同意語音辨識授權。**這是目前的作法，未來Android Things支援語音轉文字以後，就不用這樣作。後續執行應用程式不需要再連接USB滑鼠與螢幕。**