package com.enel400.xiang.hydroping;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;


import pl.pawelkleczkowski.customgauge.CustomGauge;


public class BluetoothActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
    private static String lastNotificationTime;
    private static final String TAG = "BluetoothActivity";
    public String messageBuffer;
    public boolean notificationStateFlag;
    public int updatingArduinoReply;
    private CustomGauge circleGauge;
    BluetoothAdapter myBluetoothAdapter;
    public ArrayList<BluetoothDevice> myBTDevices = new ArrayList<>();
    public DeviceListAdapter myDeviceListAdapter;
    BluetoothConnectionService myBluetoothConnectionService;
    public static TextView receivedTextDisplay;
    Switch bluetoothToggleSwitch;
    Button rebootButton;
    Button btnDiscover;
    public static TextView gaugeTextView;
    TextView bottleTextView;
    public static TextView manTextView;
    ImageView bottleImageView;
    public static ImageView manImageView;
    public static TextView gaugeCenterText;
    ProgressDialog calibrationDialog;
    public static StringBuilder messages;
    NotificationCompat.Builder testNotification;
    private static final int notificationID = 97216;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice myBTDevice;
    ListView lvNewDevices;

    //Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(myBluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, myBluetoothAdapter.ERROR);
                switch(state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG,"mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG,"mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /*
    * Broadcast Receiver for changes made to bluetooth states such as:
    * Discoverability mode on/off or expire
    * */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED))
            {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,BluetoothAdapter.ERROR);
                switch(mode)
                {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG,"mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG,"mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG,"mBroadcastReceiver2: Discoverability Disabled. Not enable to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG,"mBroadcastReceiver2: Connecting......");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG,"mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    /*
    * Broadcast Receiver for listing devices that are not yet paired
    * - Executed by btnDiscover() method
    * */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            Log.d(TAG,"onReceive: ACTION FOUND");
            if(action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName().equals("DSD TECH HC-05")&& device.getAddress().equals("00:14:03:06:8F:65")) {
                    myBTDevices.add(device);
                    Log.i(TAG,"Bottle Found");

                    myBluetoothAdapter.cancelDiscovery();
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
                    {
                        device.createBond();
                        myBluetoothConnectionService = new BluetoothConnectionService(BluetoothActivity.this);
                    }

                    startBTConnection(device,MY_UUID_INSECURE);
                }
                if(myBTDevices.isEmpty()){
                    Log.i(TAG,"myBTDevices is empty.");
                    myBluetoothAdapter.cancelDiscovery();
                    openDialog();
                }

            }
        }
    };

    /*
    * Broadcast Receiver that detects bond state changes (Pairing status changes)
    *
    * */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG,"BroadcastReceiver: BOND_BONDED");
                    myBTDevice = mDevice;
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG,"BroadcastReceiver: BOND_BONDING");
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG,"BroadcastReceiver: BOND_NONE");
                }
            }
        }
    };

    @Override
    protected void onDestroy(){
        Log.d(TAG,"onDestroy: called");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Grabbing the last time a notification message was received.
        SharedPreferences lastNotificationPreference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        lastNotificationTime = lastNotificationPreference.getString("lastNotificationTimestamp","");
        messageBuffer = "";
        notificationStateFlag = false;
        updatingArduinoReply = 1;
        setContentView(R.layout.activity_bluetooth);
        testNotification = new NotificationCompat.Builder(this);
        testNotification.setAutoCancel(true);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        receivedTextDisplay = (TextView)findViewById(R.id.incomingMessage);
        bluetoothToggleSwitch = (Switch)findViewById(R.id.BTSwitch);
        btnDiscover = (Button)findViewById(R.id.btnFindUplairedDevices);
        lvNewDevices = (ListView)findViewById(R.id.lvNewDevices);
        gaugeTextView = (TextView)findViewById(R.id.gaugeTextView);
        manTextView = (TextView)findViewById(R.id.manTextView);
        bottleTextView = (TextView)findViewById(R.id.bottleTextView);
        manImageView = (ImageView)findViewById(R.id.manImageView);
        bottleImageView = (ImageView)findViewById(R.id.bottleImageView);
        gaugeCenterText = (TextView)findViewById(R.id.gaugeCenterText);
        rebootButton = (Button)findViewById(R.id.btnReboot);
        myBTDevices = new ArrayList<>();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4,filter);
        messages = new StringBuilder();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));
        lvNewDevices.setOnItemClickListener(BluetoothActivity.this);
        circleGauge = (CustomGauge)findViewById(R.id.circleGauge);
        calibrationDialog = new ProgressDialog(BluetoothActivity.this);
        circleGauge.setValue(0);
        WaterVolume.total = 0;
        WaterVolume.last = 0; //For demostration with bottle starting as empty.
        WaterVolume.drank = 0;

        bluetoothToggleSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean check = bluetoothToggleSwitch.isChecked();
                Log.d(TAG,"onClick: enabling/disabling bluetooth");
                if(check)
                {
                    if(myBluetoothAdapter == null)
                    {
                        Log.d(TAG,"bluetoothToggleSwitch: Does not have BT capabilities.");
                    }
                    if(!myBluetoothAdapter.isEnabled())
                    {
                        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableBTIntent);
                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(mBroadcastReceiver1, BTIntent);
                    }
                    else
                    {
                        Log.d(TAG,"bluetoothToggleSwitch: Already Enabled.");
                    }
                }
                else
                {
                    if(myBluetoothAdapter == null)
                    {
                        Log.d(TAG,"bluetoothToggleSwitch: Does not have BT capabilities.");
                    }
                    if(myBluetoothAdapter.isEnabled())
                    {
                        myBluetoothAdapter.disable();
                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(mBroadcastReceiver1, BTIntent);
                    }
                    else
                    {
                        Log.d(TAG,"bluetoothToggleSwitch: Already Disabled.");
                    }
                }
            }
        });

        rebootButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                Toast.makeText(getApplicationContext(),"REBOOTING...",Toast.LENGTH_SHORT).show();
                String rebootArduino = "R";
                byte[] bytes = rebootArduino.getBytes(Charset.defaultCharset());
                myBluetoothConnectionService.write(bytes);
                WaterVolume.total = 0;
                WaterVolume.drank = 0;
                WaterVolume.v = 0;
                WaterVolume.last = 0;
                WaterVolume.manStage = 10;
                WaterVolume.bottleStage = 10;
                modifyGraphicDisplay();
            }
        });

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            if(text.contains(("Z")))
            {
                /////////The end of the message, could be in notification state or normal state. Normal state does not trigger notification.
                int footer = text.indexOf("Z");
                String finalPiece = text.substring(0,footer);
                messageBuffer +=finalPiece;
                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                String formattedDate = dateFormat.format(date);
                if(notificationStateFlag == true)
                {
                    openNotification("Don't forget to drink some water!");
                    notificationStateFlag = false;
                    messageBuffer = "";
                }
                else if (messageBuffer.contains("V"))
                {
                    messageBuffer = messageBuffer.replace("\n","");
                    messageBuffer = messageBuffer.substring(messageBuffer.indexOf("V")+1);
                    WaterVolume.v = Integer.valueOf(messageBuffer);//Converting completed messageBuffer to an integer

                    if(WaterVolume.v > WaterVolume.max) //Measurement correction
                    {
                       WaterVolume.v = WaterVolume.max;
                    }
                    if (WaterVolume.v < 0)
                    {
                        WaterVolume.v  = 0;
                    }

                    if(WaterVolume.v > WaterVolume.last) //More water added
                    {
                       WaterVolume.last = WaterVolume.v;
                       WaterVolume.drank = 0;
                    }

                    else if (WaterVolume.v <= WaterVolume.last)//Newly added, testing this else if statement
                    {
                        WaterVolume.drank = WaterVolume.last - WaterVolume.v;
                        WaterVolume.last = WaterVolume.v;
                        WaterVolume.total += WaterVolume.drank;
                    }
                    modifyGraphicDisplay();

                    messages.insert(0,formattedDate + "-->   "+ "Drank: " + WaterVolume.drank+ "ml Total: "+WaterVolume.total+"ml "+"In Bottle: "+WaterVolume.v+"\n"+"\n");
                    receivedTextDisplay.setText(messages);
                    messageBuffer = "";

                }

                else //Receive broken/corrupt/DEBUG message
                {

                }
            }
            else if (messageBuffer.contains("C")&&calibrationDialog.isShowing() == false)
            {
                    calibrationDialog.setMessage("Bottle Calibrating...");
                    calibrationDialog.setCancelable(false);
                    calibrationDialog.show();
                    messageBuffer = "";
                    messages = new StringBuilder();
                    receivedTextDisplay.setText("");
            }
            else if (messageBuffer.contains("S")&&calibrationDialog.isShowing()) //to make calibrationDialog disappear
            {
                calibrationDialog.dismiss();
                messageBuffer = "";
                messages = new StringBuilder();
                receivedTextDisplay.setText("");
            }
            else //Incomplete messages,not yet assempled.
            {
                messageBuffer += text;

                if (notificationStateFlag == false && messageBuffer.contains("N"))//Enter "NOTIFICATION STATE"
                {
                    messageBuffer = messageBuffer.replace("N", "");
                    //Refreshed notification Time
                    Date date = Calendar.getInstance().getTime();
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    String newNotificationTime = dateFormat.format(date);
                    SharedPreferences lastNotificationAdd = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    //Forgot how to save data.
                    SharedPreferences.Editor editor = lastNotificationAdd.edit();
                    editor.putString("lastNotificationTimestamp", newNotificationTime);
                    editor.commit();
                    notificationStateFlag = true;
                }
            }
        }
    };
    public void startConnection(){
        startBTConnection(myBTDevice,MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG,"startBTConnection: Initializing RFCOM Bluetooth Connection.");
        myBluetoothConnectionService.startClient(device,uuid);
    }

    public void btnEnableDisable_Discoverable(View view) {
        //Toast.makeText(BluetoothActivity.this,"Device discoverable for 300 seconds.",Toast.LENGTH_SHORT).show();
        Log.d(TAG,"btnEnableDisable_Discoverable: Making device discoverable for 300 seconds");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivity(discoverableIntent);
        IntentFilter intentFilter = new IntentFilter(myBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2,intentFilter);
    }

    public void btnDiscover(View view) {
        Toast.makeText(BluetoothActivity.this,"Discovering devices...",Toast.LENGTH_SHORT).show();
        //calibrationFlag = true;
        Log.d(TAG,"btnDiscover: Looking for unpaired devices.");
        if(myBluetoothAdapter.isDiscovering())
        {
            myBluetoothAdapter.cancelDiscovery();
            //Check BT permissions in manifest
            checkBTPermissions();
            myBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3,discoverDevicesIntent);
        }
        if(!myBluetoothAdapter.isDiscovering())
        {
            checkBTPermissions();
            myBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3,discoverDevicesIntent);
        }
    }

    private void checkBTPermissions()
    {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0)
            {
                this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},1001);
            }
            else
            {
                Log.d(TAG,"checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP");
            }

        }
    }

    public void openDialog(){
        BottleNotFoundDialog dialog = new BottleNotFoundDialog();
        dialog.show(getSupportFragmentManager(),"Bottle Not Found Dialog");
    }

    public void hundredPercentDialog()
    {
        Reach100PercentDialog dialog = new Reach100PercentDialog();
        dialog.show(getSupportFragmentManager(),"Reach 100 Percent Dialog");
    }

    public void openNotification(String text){
        testNotification.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_large));
        testNotification.setSmallIcon(R.drawable.ic_small);
        testNotification.setTicker("Time to drink some H20!");
        testNotification.setWhen(SystemClock.currentThreadTimeMillis());
        testNotification.setContentTitle("Hydration Reminder");
        testNotification.setContentText(text);
        testNotification.setDefaults(Notification.DEFAULT_ALL);
        Intent notificationIntent = new Intent(getApplicationContext(), BluetoothActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(getApplicationContext(),0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        testNotification.setContentIntent(notificationPendingIntent);
        //testNotification.setVibrate(new long[]{Notification.DEFAULT_VIBRATE});
        testNotification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        testNotification.setPriority(NotificationManager.IMPORTANCE_HIGH);
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(notificationID,testNotification.build());
    }

    public void modifyGraphicDisplay()
    {
        float h20percentfloat;
        int h20percent;
        int manStage;
        float manStageFloat;
        int bottleStage;
        float bottleStageFloat;

        if(WaterVolume.total==0) //Haven't drank any water yet
        {
            h20percent = 0;
        }
        else
        {
            h20percentfloat = 100*WaterVolume.total/WaterVolume.target;//For manImageView and manTextView
            h20percent = (int)h20percentfloat;
            if (h20percent > 100)
            {
                h20percent = 100;
            }
        }

        if(WaterVolume.v == WaterVolume.max) //Bottle is full
        {
            bottleStage = 0;
        }
        else
        {
            bottleStageFloat = (WaterVolume.max - WaterVolume.v)/35;
            bottleStage = (int)bottleStageFloat;
        }
        if(h20percent == 0)
        {
            manStage = 0;
        }
        else
        {
            manStageFloat = h20percent/10;
            manStage = (int)manStageFloat;
        }

            switch(bottleStage)
            {
                case 0:
                    bottleImageView.setImageResource(R.drawable.bottle_stage0);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage0));
                    break;
                case 1:
                    bottleImageView.setImageResource(R.drawable.bottle_stage1);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage1));
                    break;
                case 2:
                    bottleImageView.setImageResource(R.drawable.bottle_stage2);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage2));
                    break;
                case 3:
                    bottleImageView.setImageResource(R.drawable.bottle_stage3);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage3));
                    break;
                case 4:
                    bottleImageView.setImageResource(R.drawable.bottle_stage4);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage4));
                    break;
                case 5:
                    bottleImageView.setImageResource(R.drawable.bottle_stage5);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage5));
                    break;
                case 6:
                    bottleImageView.setImageResource(R.drawable.bottle_stage6);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage6));
                    break;
                case 7:
                    bottleImageView.setImageResource(R.drawable.bottle_stage7);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage7));
                    break;
                case 8:
                    bottleImageView.setImageResource(R.drawable.bottle_stage8);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage8));
                    break;
                case 9:
                    bottleImageView.setImageResource(R.drawable.bottle_stage9);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage9));
                    break;
                case 10:
                    bottleImageView.setImageResource(R.drawable.bottle_stage10);
                    bottleTextView.setTextColor(getResources().getColor(R.color.botstage10));
                    break;
                 default:
                     bottleImageView.setImageResource(R.drawable.bottle_stage0);
                     bottleTextView.setTextColor(getResources().getColor(R.color.botstage0));
                     break;
            }
            WaterVolume.bottleStage = bottleStage;

            switch(manStage)
            {
                case 0:
                    manImageView.setImageResource(R.drawable.man_stage0);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage0));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage0));
                    break;

                case 1:
                    manImageView.setImageResource(R.drawable.man_stage1);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage1));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage1));
                    break;
                case 2:
                    manImageView.setImageResource(R.drawable.man_stage2);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage2));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage2));
                    break;
                case 3:
                    manImageView.setImageResource(R.drawable.man_stage3);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage3));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage3));
                    break;
                case 4:
                    manImageView.setImageResource(R.drawable.man_stage4);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage4));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage4));
                    break;
                case 5:
                    manImageView.setImageResource(R.drawable.man_stage5);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage5));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage5));
                    break;
                case 6:
                    manImageView.setImageResource(R.drawable.man_stage6);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage6));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage6));
                    break;
                case 7:
                    manImageView.setImageResource(R.drawable.man_stage7);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage7));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage7));
                    break;
                case 8:
                    manImageView.setImageResource(R.drawable.man_stage8);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage8));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage8));
                    break;
                case 9:
                    manImageView.setImageResource(R.drawable.man_stage9);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage9));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage9));
                    break;
                case 10:
                    manImageView.setImageResource(R.drawable.man_stage10);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage10));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage10));
                    hundredPercentDialog();
                    break;
                default:
                    manImageView.setImageResource(R.drawable.man_stage0);
                    manTextView.setTextColor(getResources().getColor(R.color.manstage0));
                    gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage0));
                    break;
            }
            WaterVolume.manStage = manStage;

        circleGauge.setValue(h20percent);
        manTextView.setText("H20:"+"\n"+h20percent+"%");
        bottleTextView.setText("Vol: "+WaterVolume.v + "ml");
        gaugeCenterText.setText(h20percent+"%");
        gaugeTextView.setText("Volume in bottle: "+WaterVolume.v+"ml"+"\n"+"Volume drank: "+WaterVolume.total+"ml"+"\n" +"Target: "+WaterVolume.target+"ml"+"\n" +"Hydration: "+h20percent+"%");
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l){
        myBluetoothAdapter.cancelDiscovery();
        Log.d(TAG,"onItemClick: You clicked on a device");
        String deviceName = myBTDevices.get(i).getName();
        String deviceAddress = myBTDevices.get(i).getAddress();
        Log.d(TAG,"onItemClick: deviceName = "+ deviceName);
        Log.d(TAG,"onItemClick: deviceAddress = "+ deviceAddress);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            Log.d(TAG,"Trying to pair with "+ deviceName);
            myBTDevices.get(i).createBond();
            myBTDevice = myBTDevices.get(i);
            myBluetoothConnectionService = new BluetoothConnectionService(BluetoothActivity.this);
        }
    }
}
