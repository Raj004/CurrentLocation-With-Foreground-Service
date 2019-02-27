package com.example.forgraundservice_locationupdate;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
  private static final String TAG=MainActivity.class.getSimpleName();
  private static final int REQUEST_PERMISSION_REQUESTCODE=40;
  private MyReciver myReceiver;
  private LocationUpdatesService mService = null;
  private boolean mBound=false;
  Button start_btn,stop_btn;
  TextView tv_location;
  private final ServiceConnection mServiceConnection= new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      LocationUpdatesService.LocalBinder binder=(LocationUpdatesService.LocalBinder)service;
      mService=binder.getService();
      mBound=true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
     mService=null;
     mBound=false;
    }
  };



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    myReceiver = new MyReciver();

    setContentView(R.layout.activity_main);

    tv_location =(TextView)findViewById(R.id.text1);
    //check permission
    if(Utils.requestingLocationUpdates(this)){
      if(!checkPermission()){
        requestPermissions();


      }
    }
  }
  @Override
  protected void onStart() {
    super.onStart();
    PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this);

    start_btn = (Button) findViewById(R.id.btn);
    stop_btn = (Button) findViewById(R.id.btn2);

    start_btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!checkPermission()) {
          requestPermissions();
        } else {
          mService.requestLocationUpdates();
        }
      }
    });

    stop_btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mService.removeLocationUpdates();
      }
    });

    // Restore the state of the buttons when the activity (re)launches.
    setButtonsState(Utils.requestingLocationUpdates(this));

    // Bind to the service. If the service is in foreground mode, this signals to the service
    // that since this activity is in the foreground, the service can exit foreground mode.
    bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
            Context.BIND_AUTO_CREATE);
  }


  @Override
  protected void onResume() {
    super.onResume();
    LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
            new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
  }
  @Override
  protected void onPause(){
    LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
    super.onPause();
  }
  @Override
  protected void onStop(){
    if(mBound){
      unbindService(mServiceConnection);
      mBound=false;
    }
    PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this);
    super.onStop();
  }

  private void requestPermissions() {
    boolean shouldProvideRationale=
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
    if(shouldProvideRationale){
      Log.i(TAG,"Displaying permission rationale to provide additional context");

    }else {
      Log.i(TAG,"Requesting permission");
      ActivityCompat.requestPermissions(MainActivity.this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              REQUEST_PERMISSION_REQUESTCODE);
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    Log.i(TAG, "onRequestPermissionResult");
    if (requestCode == REQUEST_PERMISSION_REQUESTCODE) {
      if (grantResults.length <= 0) {
        // If user interaction was interrupted, the permission request is cancelled and you
        // receive empty arrays.
        Log.i(TAG, "User interaction was cancelled.");
      } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permission was granted.
        mService.requestLocationUpdates();
      } else {
        // Permission denied.
        setButtonsState(false);

      }
    }
  }


  private boolean checkPermission() {
    return PackageManager.PERMISSION_GRANTED== ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION);

  }


  //BroadcastReceiver
  private class MyReciver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Location location=intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
    if(location!=null){
      Toast.makeText(MainActivity.this,Utils.getLocationText(location),
              Toast.LENGTH_SHORT).show();
      tv_location.setText(Utils.getLocationText(location));

    }
  }
}
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String s){
    if(s.equals(Utils.KEY_REQUESTING_LOCATION_UPDATES)){
      setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES,false));
    }

  }
  private void setButtonsState(boolean requestingLocalUpdate) {
    if(requestingLocalUpdate){
      start_btn.setEnabled(false);
      stop_btn.setEnabled(true);
    }else {
      start_btn.setEnabled(true);
      stop_btn.setEnabled(false);
      tv_location.setText("");
    }
  }
}
