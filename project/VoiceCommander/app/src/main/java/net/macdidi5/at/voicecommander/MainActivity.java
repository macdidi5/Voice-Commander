package net.macdidi5.at.voicecommander;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.google.android.things.contrib.driver.pwmservo.Servo;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int SPEECH_REQUEST_CODE = 0;

    private final String PIN_NAME = "BCM24";
    private Gpio buttonGpio, ledGpio01, ledGpio02, ledGpio03, ledGpio04;
    private boolean lightOn = false;

    private Servo servo;
    private boolean doorOpen = false;

    private TextToSpeech ttsEngine;
    private Handler ttsHandler = new Handler();

    private Screen screen;

    private final String UTTERANCE_ID = "THINGS_COMMANDER";

    private boolean isListen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onStart");

        PeripheralManagerService service = new PeripheralManagerService();

        try {

            ledGpio01 = service.openGpio("BCM17");
            ledGpio02 = service.openGpio("BCM22");
            ledGpio03 = service.openGpio("BCM23");
            ledGpio04 = service.openGpio("BCM27");

            ledGpio01.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpio02.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpio03.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpio04.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            buttonGpio = service.openGpio("BCM16");
            buttonGpio.setDirection(Gpio.DIRECTION_IN);
            buttonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);

            buttonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    if (isListen) {
                        return true;
                    }

                    try {
                        Log.i(TAG, "Button: " + gpio.getValue());

                        if (gpio.getValue()) {
                            startSpeechToTextActivity();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }

                    return true;
                }
            });
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        configServo();
        configScreen();

        ttsEngine = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    ttsEngine.setLanguage(Locale.TRADITIONAL_CHINESE);
                    ttsEngine.setPitch(1f);
                    ttsEngine.setSpeechRate(1f);
                    Log.i(TAG, "TextToSpeech onInit ok!");
                    ttsHandler.post(new TtsRunnable(R_WELCOME));
                } else {
                    Log.w(TAG, "Could not open TTS Engine (onInit status=" + status + ")");
                    ttsEngine = null;
                }
            }
        });

        ttsEngine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
                Log.i("============", "UtteranceProgressListener:onStart");
            }

            @Override
            public void onDone(String id) {
                Log.i("============", "UtteranceProgressListener:onDone");
                screen.showAction(MainActivity.this, R.drawable.ic_android);
            }

            @Override
            public void onError(String id) {
                Log.i("============", "UtteranceProgressListener:onError");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE) {
            isListen = false;
            screen.clear();

            if (resultCode == RESULT_OK && data != null) {
                List<String> result =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String spokenText = result.get(0);
                takeCommand(spokenText);
            }
        }
    }

    private int errorCount = 0;

    private final String KEY_WORD = "你好";

    private final String[][] R_I_DONT_KNOW = {
            {"我不知道你在說什麼", "請你說清楚一點"},
            {"不要再亂說了", "我不想理你"},
            {"我的老天鵝啊,你不要再說了"},
            {"你想跟我這樣玩下去嗎"},
            {"我警告你,馬上停止這樣的行為"}};
    private final String R_OK = "好的";
    private final String R_DOOR_IS_OPENED = "門已經開了";
    private final String R_DOOR_IS_CLOSED = "門已經關了";
    private final String R_LIGHT_IS_TURNED_ON = "燈已經開了";
    private final String R_LIGHT_IS_TURNED_OFF = "燈已經關了";
    private final String R_YOU_ARE_JOKE = "哈哈哈,你就是一個笑話";
    private final String R_WELCOME = "歡迎光臨";

    private final String C_WHAT_TIME_IS_IT = "現在幾點";
    private final String C_TURN_ON_THE_LIGHT = "開燈";
    private final String C_TURN_OFF_THE_LIGHT = "關燈";
    private final String C_OPEN_THE_DOOR = "開門";
    private final String C_CLOSE_THE_DOOR = "關門";
    private final String C_WAKE_UP = "起床";
    private final String C_GO_TO_SLEEP = "睡覺";
    private final String C_JOKE = "講笑話";

    private final SimpleDateFormat sdf = new SimpleDateFormat ("H:mm");

    private void takeCommand(String command) {
        Log.i("============", "takeCommand: " + command);
        boolean isCommand = true;

        switch (command) {
            case C_WHAT_TIME_IS_IT:
                screen.showAction(this, R.drawable.ic_clock);
                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"));
                String[] hm = sdf.format(now.getTime()).split(":");
                String s = hm[0] + "點" + hm[1] + "分";
                ttsHandler.post(new TtsRunnable(s));
                Log.i("============", s);
                break;
            case C_TURN_ON_THE_LIGHT:
                screen.showAction(this, R.drawable.ic_lightbulb);

                if (lightOn) {
                    ttsHandler.post(new TtsRunnable(R_LIGHT_IS_TURNED_ON));
                    return;
                }

                ttsHandler.post(new TtsRunnable(R_OK));
                controlLight(true);
                break;
            case C_TURN_OFF_THE_LIGHT:
                if (!lightOn) {
                    ttsHandler.post(new TtsRunnable(R_LIGHT_IS_TURNED_OFF));
                    return;
                }

                screen.showAction(this, R.drawable.ic_lightbulb);
                ttsHandler.post(new TtsRunnable(R_OK));
                controlLight(false);
                break;
            case C_OPEN_THE_DOOR:
                screen.showAction(this, R.drawable.ic_lock);

                if (doorOpen) {
                    ttsHandler.post(new TtsRunnable(R_DOOR_IS_OPENED));
                    return;
                }

                ttsHandler.post(new TtsRunnable(R_OK));
                controlServo(true);
                break;
            case C_CLOSE_THE_DOOR:
                if (!doorOpen) {
                    ttsHandler.post(new TtsRunnable(R_DOOR_IS_CLOSED));
                    return;
                }

                screen.showAction(this, R.drawable.ic_lock);
                ttsHandler.post(new TtsRunnable(R_OK));
                controlServo(false);
                break;
            case C_WAKE_UP:
                break;
            case C_GO_TO_SLEEP:
                screen.showAction(this, R.drawable.ic_sleep);
                break;
            case C_JOKE:
                screen.showAction(this, R.drawable.ic_smile);
                ttsHandler.post(new TtsRunnable(R_YOU_ARE_JOKE));
                break;
            default:
                isCommand = false;
                screen.showAction(this, R.drawable.ic_bad);
                String sayIt = R_I_DONT_KNOW[errorCount]
                        [(int) (Math.random() * R_I_DONT_KNOW[errorCount].length)];
                ttsHandler.post(new TtsRunnable(sayIt));
                errorCount++;

                if (errorCount >= R_I_DONT_KNOW.length) {
                    errorCount = R_I_DONT_KNOW.length - 1;
                }

                break;
        }

        if (isCommand) {
            errorCount = 0;
        }

    }

    private void controlLight(boolean isOn) {
        lightOn = isOn;

        try {
            ledGpio01.setValue(isOn);
            ledGpio02.setValue(isOn);
            ledGpio03.setValue(isOn);
            ledGpio04.setValue(isOn);
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void controlServo(boolean isOpen) {
        doorOpen = isOpen;
        int angle = isOpen ? 180 : 0;

        try {
            servo.setAngle(angle);
            Log.d(TAG, "Servo setAngle(" + angle + ")");
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private class TtsRunnable implements Runnable {

        private String message;

        TtsRunnable(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            ttsEngine.speak(message, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }

    }

    private void startSpeechToTextActivity() {
        Log.i(TAG, "startSpeechToTextActivity()");

        isListen = true;
        ttsHandler.post(new TtsRunnable(KEY_WORD));

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW");

        try {
            screen.showAction(this, R.drawable.ic_voice);
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Log.e(TAG, "Your device does not support Speech to Text");
        }
    }

    private void configServo() {
        Log.d(TAG, "configServo() start...");

        try {
            servo = new Servo("PWM0");
            servo.setAngleRange(0f, 180f);
            servo.setEnabled(true);
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        Log.d(TAG, "configServo() done...");
    }

    private void configScreen() {
        Log.d(TAG, "configScreen() start...");

        try {
            screen = new Screen(this, "I2C1");
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        screen.showAction(this, R.drawable.ic_android);

        Log.d(TAG, "configScreen() done...");
    }


}
