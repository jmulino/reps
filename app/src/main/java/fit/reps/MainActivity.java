package fit.reps;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.view.View;

import bolts.Task;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;

import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Settings.BatteryState;
import bolts.Continuation;




public class MainActivity extends AppCompatActivity implements ServiceConnection {


    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board;
    private Accelerometer accelerometer;



    ////////////////////
    ////////////////Buttons
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("reps", "start");
                accelerometer.acceleration().start();
                accelerometer.start();
            }
        });
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("reps", "stop");
                accelerometer.stop();
                accelerometer.acceleration().stop();
            }
        });
        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.tearDown();
            }
        });
    }
    @Override

    public void onDestroy(){

        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        // Typecast the binder to the service's LocalBinder class

        serviceBinder = (BtleService.LocalBinder) service;



        Log.i("reps", "BT Service Connected");

        //mac address here
        retrieveBoard("CA:56:C4:FF:F6:98");

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }


        //connects via direct mac-address

    private void retrieveBoard(final String macAddr) {
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice =
                btManager.getAdapter().getRemoteDevice(macAddr);

        // Create a MetaWear board object for the Bluetooth Device
        board = serviceBinder.getMetaWearBoard(remoteDevice);
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override

            public Task<Route> then(Task<Void> task) throws Exception {
                Log.i("reps", "Connected to " + macAddr);





                accelerometer = board.getModule(Accelerometer.class );
                accelerometer.configure()
                        .odr(50f)       // Set sampling frequency to 25Hz, or closest valid ODR
                        .range(4f)
                        .commit();



                return accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override

                    //accelerometer.routeData().fromZAxis();

                    public void configure(RouteComponent source) {
                        source.map(Function1.RSS).lowpass((byte) 8).filter(ThresholdOutput.BINARY, .9f)
                                .multicast()
                                    .to().filter(Comparison.EQ, -1).stream(new Subscriber(){
                                        @Override
                                        public void apply(Data data, Object... env){
                                            Log.i("reps", "downwards");
                                        }
                                    })
                                    .to().filter(Comparison.EQ, 1). stream(new Subscriber() {
                                        @Override
                                        public void apply(Data data, Object... env){
                                            Log.i("reps", "upwards");
                                        }
                                    })
                                .end();

                                //////////raw sensor date below


///////////                        source.stream(new Subscriber() {
///////////                             @Override
///////////                             public void apply(Data data, Object... env) {
///////////                                 Log.i("reps", data.value(Acceleration.class).toString());
///////////                             }
///////////                         });
                    }
                });

                accelerometer.routeData()
                        .fromZAxis()
                        .
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                if (task.isFaulted()) {
                    Log.w("reps", "failed to configure app", task.getError());
                } else {
                    Log.i("reps", "App Configured");
                }

                return null;
            }
        });
////////////////////////// //////////////////////////////////////
///
//test battery read

//            board.readBatteryLevelAsync()
//                    .continueWith(new Continuation<Byte, Void>() {
//                @Override
//                public Void then(Task<Byte> task) throws Exception {
//                    Log.i("reps", "Battery Level: " + task.getResult());
//                    return null;
//                }

//           });

            /////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////
    }


}



