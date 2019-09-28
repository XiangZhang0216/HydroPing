package com.enel400.xiang.hydroping;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;

public class Reach100PercentDialog extends AppCompatDialogFragment {
    @Override
    public Dialog onCreateDialog (Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Target Reached!")
                .setMessage("You have reached your hydration goal. Click OK to reset hydration counter.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WaterVolume.total = 0;
                        BluetoothActivity.gaugeCenterText.setText("0%");
                        BluetoothActivity.gaugeCenterText.setTextColor(getResources().getColor(R.color.botstage0));
                        BluetoothActivity.manTextView.setTextColor(getResources().getColor(R.color.manstage0));
                        BluetoothActivity.manTextView.setText("H20:"+"\n"+"0%");
                        BluetoothActivity.manImageView.setImageResource(R.drawable.man_stage0);
                        BluetoothActivity.messages = new StringBuilder();
                        BluetoothActivity.receivedTextDisplay.setText("");
                        BluetoothActivity.gaugeTextView.setText("Volume in bottle: "+WaterVolume.v+"ml"+"\n"+"Volume drank: "+WaterVolume.total+"ml"+"\n" +"Target: "+WaterVolume.target+"ml"+"\n" +"Hydration: "+"0%");
                    }
                });
        return builder.create();
    }
}
