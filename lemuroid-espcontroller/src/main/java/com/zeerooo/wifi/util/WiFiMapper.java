package com.zeerooo.wifi.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.widget.Toast;

import com.swordfish.libretrodroid.GLRetroView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.WIFI_SERVICE;

public class WiFiMapper {

    private byte inputByte, currentButton = 0;
    private byte[] buffer;
    private boolean dpadRelease = false, buttonRelease = false;
    private Disposable disposable = null;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream = null;
    private SharedPreferences sharedPreferences;
    private String espIp;

    public WiFiMapper(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void sendData(String message, String destinationIp) {
        System.out.println("Sending: " + message + " to IP: " + destinationIp);

        disposable = Single.just(true)
                .subscribeOn(Schedulers.io())
                .doFinally(() -> {
                    if (socket != null && !socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (dataOutputStream != null)
                        dataOutputStream.close();
                })
                .doOnError(throwable -> dispose())
                .subscribe(aBoolean -> {
                    socket = new Socket(destinationIp, 9998);
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeBytes(message);
                    dataOutputStream.flush();
                });
    }

    public void getEspIp(Context context) {
        String currentIpAddress = Formatter.formatIpAddress((((WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE)).getConnectionInfo().getIpAddress()));

        System.out.println("-> Getting ESP ip address. This device address is " + currentIpAddress);

        final String[] currentIpAddressArray = currentIpAddress.split(Pattern.quote("."));

        disposable = Observable
                .create((ObservableEmitter<String> emitter) -> {
                    InetAddress espAddress;

                    for (short i = 0; i < 255; i++) {
                        try {
                            espAddress = InetAddress.getByName(currentIpAddressArray[0] + '.' + currentIpAddressArray[1] + '.' + currentIpAddressArray[2] + '.' + i);

                            if (espAddress.isReachable(50)) {
                                socket = new Socket(espAddress, 9998);
                                socket.setSoTimeout(50);

                                dataInputStream = new DataInputStream(socket.getInputStream());
                                dataInputStream.read();

                                if (dataInputStream.available() != 0) {
                                    System.out.println("ESP found at: " + socket.getInetAddress().getHostName());

                                    sharedPreferences.edit().putString("esp_ip", socket.getInetAddress().getHostName()).apply();

                                    emitter.onNext("ESP8266 found at: " + socket.getInetAddress().getHostName());

                                    break;
                                }
                            } else if (i == 254) {
                                emitter.onNext("ESP8266 not found. Make sure key 'A' is pressed while the device is starting");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnDispose(() -> {
                    if (socket != null && !socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .doOnError(throwable -> dispose())
                .doOnNext(string -> {
                    Toast.makeText(context.getApplicationContext(), string, Toast.LENGTH_LONG).show();
                    dispose();
                })
                .subscribe();
    }

    public void setupWiFiPad(GLRetroView retroGameView) {
        espIp = sharedPreferences.getString("esp_ip", "0.0.0.0");

        disposable = Observable
                .create((ObservableEmitter<byte[]> emitter) -> {
                    socket = new Socket(espIp, 9998);

                    while (disposable != null) {
                        try {
                            dataInputStream = new DataInputStream(socket.getInputStream());
                            buffer = new byte[dataInputStream.available()];
                            if (buffer.length > 0) {
                                dataInputStream.readFully(buffer);
                                emitter.onNext(buffer);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnDispose(() -> {
                    if (socket != null && !socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .subscribe(inputArray -> {
                    if (inputArray[0] == 99) { // Release
                        if (dpadRelease) {
                            retroGameView.sendMotionEvent(0, 0, 0, 0);
                            dpadRelease = false;
                            buttonRelease = true;
                        }
                        if (buttonRelease) {
                            retroGameView.sendKeyEvent(1, currentButton, 0);
                            buttonRelease = false;
                        }
                    } else
                        for (byte bytePosition = 0; bytePosition < inputArray.length; bytePosition++) {
                            inputByte = inputArray[bytePosition];

                            if (inputByte == 0) { // Button pressed
                                switch (bytePosition) {
                                    case 4: // Up
                                        retroGameView.sendMotionEvent(0, 0, -1, 0);
                                        dpadRelease = true;
                                        break;
                                    case 5: // Down
                                        retroGameView.sendMotionEvent(0, 0, 1, 0);
                                        dpadRelease = true;
                                        break;
                                    case 6: // Left
                                        retroGameView.sendMotionEvent(0, -1, 0, 0);
                                        dpadRelease = true;
                                        break;
                                    case 7: // Right
                                        retroGameView.sendMotionEvent(0, 1, 0, 0);
                                        dpadRelease = true;
                                        break;

                                    case 0: //
                                        currentButton = KeyEvent.KEYCODE_BUTTON_Y;
                                        retroGameView.sendKeyEvent(0, currentButton, 0);
                                        buttonRelease = true;
                                        break;
                                    case 1: //
                                        currentButton = KeyEvent.KEYCODE_BUTTON_X;
                                        retroGameView.sendKeyEvent(0, currentButton, 0);
                                        buttonRelease = true;
                                        break;
                                    case 2: // Select
                                        currentButton = KeyEvent.KEYCODE_BUTTON_SELECT;
                                        retroGameView.sendKeyEvent(0, currentButton, 0);
                                        buttonRelease = true;
                                        break;
                                    case 3: // Start
                                        currentButton = KeyEvent.KEYCODE_BUTTON_START;
                                        retroGameView.sendKeyEvent(0, currentButton, 0);
                                        buttonRelease = true;
                                        break;
                                    case 8: //
                                        currentButton = KeyEvent.KEYCODE_BUTTON_A;
                                        retroGameView.sendKeyEvent(0, currentButton, 0);
                                        buttonRelease = true;
                                        break;
                                    case 9: //
                                        currentButton = KeyEvent.KEYCODE_BUTTON_B;
                                        retroGameView.sendKeyEvent(0, currentButton, 0);
                                        buttonRelease = true;
                                        break;
                                }

                            }
                        }
                }, throwable -> {
                    throwable.printStackTrace();
                    dispose();
                });
    }

    public void dispose() {
        System.out.println("--> Disposing");
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
        }
    }
}
