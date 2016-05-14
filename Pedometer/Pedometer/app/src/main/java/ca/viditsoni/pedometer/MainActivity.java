package ca.viditsoni.pedometer;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.viditsoni.pedometer.LineGraphView;
import java.util.Arrays;

public class MainActivity extends Activity {
    private SensorManager sensorManager;

    //Declaring TextViews to display the sensor data
    TextView accelerometerSensorInfo;
    TextView maxAccelerometerSensorInfo;
    TextView steps;
    LineGraphView graph;

    float max_accelerometer_x_value = 0;
    float max_accelerometer_y_value = 0;
    float max_accelerometer_z_value = 0;
    int stepsAmount = 0;

    float last_x_value = 0;
    float last_y_value = 0;
    float last_z_value = 0;

    float max_x_change = 0;
    float max_y_change = 0;
    float max_z_change = 0;

    //Represents the max steps taken
    float max_step = 0.25f;

    boolean steps_taken = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setting up the text views so that they can display data later on in the code
        accelerometerSensorInfo = (TextView) findViewById(R.id.accelerometer);
        maxAccelerometerSensorInfo = (TextView) findViewById(R.id.max_accelerometer);
        steps = (TextView) findViewById(R.id.steps);


        //Setting up the sensor manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        SensorEventListener l = new LightSensorEventListener(accelerometerSensorInfo);

        //Registering all the different sensors to be used
        sensorManager.registerListener(l, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        LinearLayout layout = (LinearLayout) findViewById(R.id.info_graph);

        //Creating a new graph
        graph = new LineGraphView(getApplicationContext(),
                100,
                Arrays.asList("x", "y", "z"));

        //Displaying the new created graph
        layout.addView(graph,0);
        graph.setVisibility(View.VISIBLE);
    }

    //Applies a lowpass filter to the sensor data
    float[] lowpass(float[] in) {
        float[] out = new float[in.length];
        final float alpha = 0.8f;
        out[0] = 0;
        for(int i = 1; i < in.length; i++) {
            out[i] = alpha  * in[i] + (1- alpha) * out[i-1];
        }
        return out;
    }

    //Applies a highpass filter to the sensor data
    float[] highpass(float[] in) {
        float[] out = new float[in.length];
        float alpha = 0.8f;
        out[0] = 0;
        for(int i = 1; i < in.length; i++) {
            out[i] = alpha * out[i-1] + alpha * (in[i] - in[i-1]);
        }
        return out;
    }

    class LightSensorEventListener implements SensorEventListener {
        TextView output;

        public LightSensorEventListener(TextView outputView) {
            output = outputView;
        }

        public void onAccuracyChanged(Sensor s, int i) {
        }

        public void onSensorChanged(SensorEvent se) {
            if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                graph.addPoint(se.values);

                //Isolating the values of gravity with a low pass filter
                float gravity[] = lowpass(se.values.clone());
                //Removing the values of gravity to prevent unneccesary x,y,z values
                float x_value = se.values[0] - gravity[0];
                float y_value = se.values[1] - gravity[1];
                float z_value = se.values[2] - gravity[2];

                float change_in_x = Math.abs(last_x_value - x_value);
                float change_in_y = Math.abs(last_y_value - y_value);
                float change_in_z = Math.abs(last_z_value - z_value);


                if (change_in_x > max_x_change) {
                    max_x_change = change_in_x;
                }
                if (change_in_y > max_y_change) {
                    max_y_change = change_in_y;
                }

                if (change_in_z > max_z_change) {
                    max_z_change = change_in_z;
                }

                last_x_value = x_value;
                last_y_value = y_value;
                last_z_value = z_value;

                if (change_in_z > max_step && steps_taken == false){
                    stepsAmount += 1;
                    steps_taken = true;
                }

                if (change_in_z < max_step && steps_taken == true) {
                    steps_taken = false;
                }

                //Updating max accelerometer sensor values if the current values are the highest
                if (x_value > max_accelerometer_x_value) {
                    max_accelerometer_x_value = se.values[0];
                }
                else if (y_value > max_accelerometer_y_value){
                    max_accelerometer_y_value = se.values[1];
                }
                else if (z_value > max_accelerometer_z_value) {
                    max_accelerometer_z_value = se.values[2];
                }

                String steps_string = Integer.toString(stepsAmount);

                steps.setText(steps_string + " Steps");


                //Converting the accelerometer values to string because setText wont display floats...
                String x=Float.toString(x_value);
                String y=Float.toString(y_value);
                String z=Float.toString(z_value);

                String max_x=Float.toString(max_accelerometer_x_value);
                String max_y=Float.toString(max_accelerometer_y_value);
                String max_z=Float.toString(max_accelerometer_z_value);


                //Displaying accelerometer values within the text views
                maxAccelerometerSensorInfo.setText("x: " + x +"\ny:" + y + "\nz:" + z);
                accelerometerSensorInfo.setText("max_x: " + max_x + "\nmax-y: " + max_y + "\nmax-z: " + max_z);
            }
        }
    }
}