package com.tmtron.rxjava2udp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int PORT_NO = 8765;
    private TextView tvUdpData;

    private UdpWriter udpWriter;

    private final String TAG = MainActivity.class.getSimpleName();
    private Disposable disposableUdpData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        udpWriter = new UdpWriter(PORT_NO);

        tvUdpData = (TextView)findViewById(R.id.tvUdpData);
        Button btnStart = (Button) findViewById(R.id.btnStart);
        Button btnStop = (Button) findViewById(R.id.btnStop);

        // make the text-view scrollable
        // see http://stackoverflow.com/a/3256305/6287240
        tvUdpData.setMovementMethod(new ScrollingMovementMethod());

        btnStart.setOnClickListener(onStartClick());
        btnStop.setOnClickListener(onStopClick());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        final Observable<DatagramPacket> udpObservable = UdpObservable.create(PORT_NO, 512);

        disposableUdpData = udpObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<DatagramPacket>() {
                            @Override
                            public void accept(DatagramPacket datagramPacket) throws Exception {
                                Log.d(TAG, "received datagram");
                                appendText(new String(datagramPacket.getData()));
                            }
                        }
                        , new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e(TAG, "udp observable failed", throwable);
                                appendText("ERROR: " + throwable.getMessage());
                            }
                        }
                );
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        if (disposableUdpData != null) {
            disposableUdpData.dispose();
            disposableUdpData = null;
        }
    }

    private void appendText(CharSequence charSequence) {
        tvUdpData.append(charSequence+"\n");
    }

    @NonNull
    private View.OnClickListener onStopClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "stop");
                sendBroadcast("stop button clicked");
            }
        };
    }

    @NonNull
    private View.OnClickListener onStartClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "start");
                sendBroadcast("start button clicked");
            }
        };
    }

    private void sendBroadcast(String data) {
        try {
            udpWriter.sendBroadcast(data);
        } catch (IOException e) {
            Log.e(TAG, "failed to send broadcast", e);
        }
    }
}
