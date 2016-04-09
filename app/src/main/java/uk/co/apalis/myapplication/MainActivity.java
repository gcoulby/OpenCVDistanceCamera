package uk.co.apalis.myapplication;

import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import static org.opencv.imgproc.Imgproc.Canny;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private JavaCameraView mOpenCvCameraView;
    private Mat edge;
    Sensor accelerometer;
    SensorManager sm;
    TextView distanceOutput, heightOutput, distanceAngleOutput, heightAngleOutput;
    double phoneHeight = 1.22;
    double x, y, z, distance, height, distanceAngle, heightAngle;
    Button distanceButton, heightButton, resetButton;
    SeekBar thresholdBar;
    int threshold;
    boolean checkDistance = true;
    boolean checkHeight = false;
    TextView tv;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.MainActivityCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        distanceOutput = (TextView) findViewById(R.id.distanceOutput);
        distanceButton = (Button) findViewById(R.id.distanceButton);
        heightOutput = (TextView) findViewById(R.id.heightOutput);
        heightButton = (Button) findViewById(R.id.heightButton);
        resetButton = (Button) findViewById(R.id.resetButton);
        thresholdBar = (SeekBar) findViewById(R.id.thresholdBar);
        heightAngleOutput = (TextView) findViewById(R.id.heightAngle);
        distanceAngleOutput = (TextView) findViewById(R.id.distanceAngle);
        tv = (TextView) findViewById(R.id.textView4);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                setXYZ(event.values[0], event.values[1], event.values[2]);

                int rotation = (int) Math.round(Math.toDegrees(Math.atan2(x, y)));

                if(checkDistance)
                {
                    distance = getDistance();
                    distanceOutput.setText(distance + "m");
                    distanceAngleOutput.setText("Angle: " + distanceAngle);
                }
                if(checkHeight)
                {
                    height = getHeight();
                    heightOutput.setText(height + "m");
                    heightAngleOutput.setText("Angle: " + heightAngle);

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {

            }
        }, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        distanceButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkDistance = false;
                checkHeight = true;
                heightOutput.setTypeface(null, Typeface.NORMAL);
                heightButton.setVisibility(View.VISIBLE);
            }
        });

        heightButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkHeight = false;
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkDistance = true;
                checkHeight = false;
                heightOutput.setTypeface(null, Typeface.ITALIC);
                heightOutput.setText("Set Distance");
                heightButton.setVisibility(View.INVISIBLE);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        edge = new Mat();
    }

    @Override
    public void onCameraViewStopped()
    {
        edge.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        threshold = thresholdBar.getProgress();
        edge = inputFrame.rgba();
        if(((Switch) findViewById(R.id.edgeSwitch)).isChecked())
            Canny(edge, edge,threshold , threshold*3);
        return edge;
    }

    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
    }

    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView != null)
        {
            mOpenCvCameraView.disableView();
        }
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        public static final String TAG = "";

        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    private void setXYZ(double x, double y, double z)
    {
        double norm = Math.sqrt(x * x + y * y + z * z);

        this.x = x / norm;
        this.y = y / norm;
        this.z = z / norm;
    }

    private double getDistance()
    {
        double inclination = Math.toDegrees(Math.acos(z));
        double angle;

        if(inclination > 90)
            angle = 90;
        else if(inclination < 0 || y < 0)
            angle = 0;
        else
            angle = Math.round((inclination) * 10.0) / 10.0;

        distanceAngle = angle;
        double tanAngle = Math.tan(Math.toRadians(angle));

        return Math.round(((phoneHeight * tanAngle) - 0.1) * 100.0) / 100.0;
    }

    private double getHeight()
    {

        double angle = Math.toDegrees(Math.acos(z));
//        double angle;

//        angle = Math.round((inclination) * 10.0) / 10.0;
        double a, c, A, B, C;

        a = Math.sqrt((Math.pow(phoneHeight, 2) + Math.pow(distance,2)));

        C = ((angle - distanceAngle) > 0) ? round((angle - distanceAngle), 2) : 0;
        B = 180 - 90 - distanceAngle;
        A = 180 - B - C;

        c = (a * (Math.sin(Math.toRadians(C)))) / Math.sin(Math.toRadians(A));
        tv.setText("a: " + round(a, 2) + "\nA: " + round(A,2) + "\nB: " + round(B,2) + "\nC: " + round(C,2) + "\nc: " + round(c,2));

        heightAngle = C;

        return round(c,2);
    }

    private double round(double d, int decimals)
    {
        double power = Math.pow(10, decimals);
        if(decimals == 0)
        {
            return Math.round(d);
        }
        else
        {
            return Math.round(d * power) / power;
        }
    }

}
