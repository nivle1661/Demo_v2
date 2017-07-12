package com.example.ggu.demo_v1;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;
import static com.example.ggu.demo_v1.R.id.imageView;
import static org.opencv.calib3d.Calib3d.CALIB_CB_ADAPTIVE_THRESH;
import static org.opencv.calib3d.Calib3d.CALIB_CB_FAST_CHECK;
import static org.opencv.calib3d.Calib3d.CALIB_CB_NORMALIZE_IMAGE;
import static org.opencv.calib3d.Calib3d.drawChessboardCorners;
import static org.opencv.core.Core.inRange;
import static org.opencv.core.Core.perspectiveTransform;
import static org.opencv.core.Core.sumElems;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.RETR_LIST;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.fillPoly;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.warpPerspective;

public class MainActivity extends AppCompatActivity {
    /**
     * Loads OpenCV prior to starting the app
     */
    private static final String TAG = "GUESS WHAT?";
    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }
    static{ System.loadLibrary("opencv_java3"); }
    private int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1661;

    private int width;
    private int height;
    private float scale;
    private boolean found = false;

    private int[] color;
    private Mat flipper;
    private Mat imgMAT;
    private Mat imgFilt = new Mat();
    private Mat K_m;
    private Mat H;
    private MatOfPoint3f coord = new MatOfPoint3f();
    private MatOfDouble distCoeff_m = new MatOfDouble();
    private MatOfDouble nope = new MatOfDouble();
    private ArrayList<Mat> objects = new ArrayList<>();
    private ArrayList<String> paths = new ArrayList<>();

    private Mat R_t = new Mat();
    private Mat T = new Mat();

    Bitmap thumbnail;
    Bitmap hello;
    Uri pathName;

    ImageView capture;
    ImageView preview;
    ImageButton delete;
    ImageButton removeBack;
    ImageButton takePic;
    ImageButton rectifier;
    ImageButton next;
    TextView pallete;
    TextView hi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        /**
         *
         */
        capture = (ImageView) findViewById(imageView);
        preview = (ImageView) findViewById(R.id.imageView2);
        pallete = (TextView) findViewById(R.id.textView2);
        delete = (ImageButton) findViewById(R.id.imageButton);
        removeBack = (ImageButton) findViewById(R.id.imageButton2);
        takePic = (ImageButton) findViewById(R.id.imageButton4);
        rectifier = (ImageButton) findViewById(R.id.imageButton3);
        next = (ImageButton) findViewById(R.id.imageButton5);
        hi = (TextView) findViewById(R.id.textView);


        double[][] K = {{3069.094942863979200, 0, 1511.5}, {0, 3068.940961932011300, 2015.5}, {0, 0, 1}};
        double[] distCoeff = {0.082087956386913, -0.393987049774921, -0.000887697003344, -0.000496522586645, 0.000000000000000};
        double[] zeroed = {0, 0, 0, 0, 0};
        double[][] noscope = {{-1.0,  0.0,  0.0}, {0.0, -1.0,  0.0}, {0.0,  0.0,  1.0}};

        K_m = new Mat(3, 3, CvType.CV_32FC1);
        flipper = new Mat(3, 3, CvType.CV_32FC1);
        H = new Mat(3, 3, CvType.CV_32FC1);
        for(int row = 0; row <3; row++) {
            for (int col = 0; col < 3; col++) {
                K_m.put(row, col, K[row][col]);
                flipper.put(row, col, noscope[row][col]);
            }
        }

        java.util.List<Point3> pointsList = new ArrayList<>();
        for(int row = 1; row < 8; row++)
            for(int col = 1; col < 8; col++) {
                //original 150
                pointsList.add(new Point3((double) (200+row*150), (double) (200+(8-col)*150), 0.0));
            }
        coord.fromList(pointsList);

        distCoeff_m.fromArray(distCoeff);
        nope.fromArray(zeroed);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (thumbnail != null) {
                    capture.setImageResource(0);
                    thumbnail.recycle();
                    imgMAT = new Mat();
                }
            }
        });

        takePic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectImage();
            }
        });

        rectifier.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("myTag", "Part 7: Saving fragments");
                if(found) new correctPose().execute("leggo");
            }
        });

        capture.setOnTouchListener(new ImageView.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                ImageView imageView = ((ImageView)v);

                //hi.setText("Touch coordinates : " +
                //        String.valueOf(event.getX()) + "x" + String.valueOf(event.getY()));

                if(imageView.getDrawable() != null) {
                    Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    double xscale = 1120/300, yscale = 800/230;
                    int pixel = bitmap.getPixel((int) ((event.getX() )*xscale),
                                                (int) ((event.getY() )*yscale));
                    color = new int[]{Color.red(pixel), Color.green(pixel), Color.blue(pixel)};

                    pallete.setBackgroundColor(pixel);
                    hi.setText("Color : " +
                            color[0] + " " + color[1] + " " + color[2]);

                    return true;    }
                return false;
            }
        });

        removeBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(color != null) new RemoveBackground().execute("leggo");
            }
        });

        next.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, view_gallery.class);

                intent.putExtra("objects", paths);
                startActivity(intent);
            }
        });

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite uniqueBundle extras = getIntent().getExtras();
            return;
        }
    }

    String mCurrentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                String userChoosenTask;
                if (items[item].equals("Take Photo")) {
                    userChoosenTask="Take Photo";

                    dispatchTakePictureIntent();
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask="Choose from Library";

                    galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    private void dispatchTakePictureIntent() {
        System.gc();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider2",
                    photoFile);
            pathName = photoURI;
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    static final int PICK_IMAGE = 2;
    private void galleryIntent() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }


    public static final int THUMBNAIL_HEIGHT = 300;
    public static final int THUMBNAIL_WIDTH = 230;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            InputStream streamIn = null;
            InputStream streamInT = null;

            if (requestCode == REQUEST_TAKE_PHOTO) {
                try {
                    //Unsafe but it's fine!
                    streamIn = new FileInputStream(mCurrentPhotoPath);
                    streamInT = new FileInputStream(mCurrentPhotoPath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                Uri selectedImage = data.getData();
                try {
                    streamIn = getContentResolver().openInputStream(selectedImage);
                    streamInT = getContentResolver().openInputStream(selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            hello = BitmapFactory.decodeStream(streamIn);

            float widthScale = (float) bitmapOptions.outWidth/THUMBNAIL_WIDTH;
            float heightScale = (float) bitmapOptions.outHeight/THUMBNAIL_HEIGHT;

            scale = Math.min(widthScale, heightScale);
            bitmapOptions.inSampleSize = (int) Math.floor(scale);
            bitmapOptions.inJustDecodeBounds = false;

            thumbnail = BitmapFactory.decodeStream(streamInT, null, bitmapOptions);
            imgMAT = new Mat(hello.getHeight(), hello.getWidth(), CV_8UC3);
            Mat temp = new Mat(imgMAT.size(), CV_8UC4);
            Utils.bitmapToMat(hello, temp);
            Imgproc.cvtColor(temp, imgMAT, Imgproc.COLOR_RGBA2RGB);

            capture.setImageBitmap(thumbnail);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private class RemoveBackground extends AsyncTask<String, Void, String> {
        int count;
        Mat edges;

        @Override
        protected String doInBackground(String... urls) {
            //Resize image beforehand plox
            Log.d("myTag", "Part 1: Color thresholding");

            width = (int) imgMAT.size().width;
            height = (int) imgMAT.size().height;

            Mat tempM;
            edges = new Mat(imgMAT.size(), CV_8UC1, new Scalar(0));
            Scalar min = new Scalar(Math.max(color[0] - 30, 0), Math.max(color[1] - 30, 0), Math.max(color[2] - 30, 0));
            Scalar max = new Scalar(Math.min(color[0] + 30, 255), Math.min(color[1] + 30, 255), Math.min(color[2] + 30, 255));
            inRange(imgMAT, min, max, edges);

            Log.d("myTag", "Part 2: Finding Contours " + sumElems(edges).val[0]/255/width/height);
            Mat hierarchy = new Mat();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            List<MatOfPoint> actualContours = new ArrayList<>();
            findContours(edges, contours, hierarchy, RETR_LIST, CHAIN_APPROX_NONE);

            Iterator<MatOfPoint> each = contours.iterator();
            double minArea = width*height*0.005;
            //Mat mask = new Mat(imgMAT.size(), CV_8UC1, new Scalar(0));
            //fillPoly(mask, contours, new Scalar(255));
            //imgMAT.copyTo(imgMAT, mask);

            Log.d("myTag", "Part 3: Contour thresholding " + contours.size());
            while (each.hasNext()) {
                MatOfPoint wrapper = each.next();

                if (Imgproc.contourArea(wrapper) >= minArea && Imgproc.contourArea(wrapper) < .95*width*height) {
                    actualContours.add(wrapper);
                }
            }

            Log.d("myTag", "Part 4: Background removal " + actualContours.size());
            Mat mask = new Mat(imgMAT.size(), CV_8UC1, new Scalar(0));
            fillPoly(mask, actualContours, new Scalar(255));
            imgMAT.copyTo(imgFilt, mask);
            drawContours(imgFilt, actualContours, -1, new Scalar(0, 0, 0), 9);
            if(!isChessboard(imgFilt)) return "donezoed";

            Log.d("myTag", "Part 5: Fragment extraction");
            each = actualContours.iterator();
            MatOfPoint2f corners = new MatOfPoint2f();

            count = 0;
            while(each.hasNext()) {
                count++;
                MatOfPoint wrapper = each.next();

                Rect rect = boundingRect(wrapper);
                int downX = Math.max(rect.x - 10, 0), upX = downX + rect.width + Math.min(width - rect.x - rect.width, 10);
                int downY = Math.max(rect.y - 10, 0), upY = downY + rect.height + Math.min(height - rect.y - rect.height, 10);
                Log.d("myTag", downX + " " + downY + " " + upX + " " + upY);

                List<Point> c = new ArrayList<>();
                c.add(new Point(downX, downY));
                c.add(new Point(downX, upY));
                c.add(new Point(upX, downY));
                c.add(new Point(upX, upY));

                List<Point> rekt = new ArrayList<>();
                Mat c_new = new Mat();
                perspectiveTransform(Converters.vector_Point2f_to_Mat(c), c_new, H);
                Converters.Mat_to_vector_Point(c_new, rekt);
                downX = (int) Math.min(Math.min(Math.min(rekt.get(0).x, rekt.get(1).x), rekt.get(2).x), rekt.get(3).x);
                downY = (int) Math.min(Math.min(Math.min(rekt.get(0).y, rekt.get(1).y), rekt.get(2).y), rekt.get(3).y);
                upX = (int) Math.max(Math.max(Math.max(rekt.get(0).x, rekt.get(1).x), rekt.get(2).x), rekt.get(3).x);
                upY = (int) Math.max(Math.max(Math.max(rekt.get(0).y, rekt.get(1).y), rekt.get(2).y), rekt.get(3).y);

                Rect rektified = new Rect(new Point(Math.max(downX, 0), Math.min(downY, width-1)), new Point(Math.max(upX, 0), Math.min(upY, height-1)));
                tempM = imgFilt.submat(rektified);

                Log.d("myTag", "Part 5: Fragment extraction " + count + "a");
                if (found || !Calib3d.findChessboardCorners(tempM, new Size(7, 7), corners,
                     CALIB_CB_ADAPTIVE_THRESH + CALIB_CB_NORMALIZE_IMAGE + CALIB_CB_FAST_CHECK))
                    objects.add(tempM);
                else found = true;
            }

            //multiply(H, flipper, H);
            Log.d("myTag", "Part 6: Unwarping");
            imgMAT = new Mat();
            warpPerspective(imgFilt, imgMAT, H, imgFilt.size());
            return "done";
        }

        private boolean isChessboard(Mat contour) {
            Mat temp = new Mat();
            cvtColor(contour, temp, COLOR_RGB2GRAY);

            Log.d("myTag", "Part 5: Fragment extraction " + count + "b");
            MatOfPoint2f corners = new MatOfPoint2f();
            boolean result = Calib3d.findChessboardCorners(temp, new Size(7, 7), corners,
                    CALIB_CB_ADAPTIVE_THRESH + CALIB_CB_NORMALIZE_IMAGE + CALIB_CB_FAST_CHECK);

            Log.d("myTag", "Part 5: Fragment extraction " + count + "b end");
            if(result) {
                Log.d("myTag", "Part 5: Fragment extraction " + count + "c");
                drawChessboardCorners(imgFilt, new Size(7, 7), corners, true);

                TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
                Imgproc.cornerSubPix(temp, corners, new Size(11, 11), new Size(-1, -1), term);

                /**Mat R_t_t = new Mat();
                 * Mat tempH = new Mat(3, 3, CvType.CV_32FC1);
                 * Calib3d.solvePnPRansac(coord, corners, K_m, nope, R_t_t, T);
                 * Rodrigues(R_t_t, R_t);

                 * List<Mat> RandT = new ArrayList<>();
                 * RandT.add(R_t.submat(0, 3, 0, 2));
                 * RandT.add(T);
                 * hconcat(RandT, tempH);
                 * multiply(K_m, tempH, H, 1.0, CvType.CV_32FC1); */

                Point[] convenient = corners.toArray();
                List<Point> src = new ArrayList<>();
                Point c1 = convenient[0], c2 = convenient[6], c3 = convenient[42], c4 = convenient[48];
                src.add(c1); src.add(c2); src.add(c3); src.add(c4);

                double hlength = (Math.sqrt((c4.y - c1.y)*(c4.y - c1.y) + (c4.x - c1.x)*(c4.x - c1.x)) +
                                  Math.sqrt((c3.y - c2.y)*(c3.y - c2.y) + (c3.x - c2.x)*(c3.x - c2.x)))/4/Math.sqrt(2);
                Point center = new Point((c1.x + c2.x + c3.x + c4.x)/4, (c1.y + c2.y + c3.y + c4.y)/4);
                List<Point> dst = new ArrayList<>();
                for(int i = 0; i < 4; i++) {
                    Point c_new;
                    Point c = src.get(i);
                    if(c.x > center.x && c.y > center.y) c_new = new Point(center.x + hlength, center.y + hlength);
                    else if(c.x < center.x && c.y > center.y) c_new = new Point(center.x - hlength, center.y + hlength);
                    else if(c.x > center.x && c.y < center.y) c_new = new Point(center.x + hlength, center.y - hlength);
                    else c_new = new Point(center.x - hlength, center.y - hlength);
                    Log.d("myTag", c_new.toString());

                    dst.add(c_new);
                }
                H = getPerspectiveTransform(Converters.vector_Point2f_to_Mat(src), Converters.vector_Point2f_to_Mat(dst));
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if(found) {
                hi.setText("Success \n" + H.get(0, 0)[0] + " " + H.get(0, 1)[0] + " " + H.get(0, 2)[0] +
                                   "\n" + H.get(1, 0)[0] + " " + H.get(1, 1)[0] + " " + H.get(1, 2)[0] +
                                   "\n" + H.get(2, 0)[0] + " " + H.get(2, 1)[0] + " " + H.get(2, 2)[0]);
            }
            else hi.setText("Pose can't be computed. \nChessboard not found\n");

            Bitmap scale = createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(imgMAT, scale);
            preview.setImageBitmap(scale);
        }
    }

    private class correctPose extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            transformSegments();
            return "done";
        }

        @Override
        protected void onPostExecute(String result) {
            hi.setText("Pieces have been rectified");
        }

        private void transformSegments() {
            Mat temp;
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOutputStream = null;

            for(int i = 0; i < objects.size(); i++) {
                temp = objects.get(i);
                //warpPerspective(objects.get(i), temp, H.inv(), objects.get(i).size());

                Bitmap tempL = createBitmap(temp.width(), temp.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(objects.get(i), tempL);

                File file = new File(path, "object" + (i+1) + ".jpg");
                try {
                    fOutputStream = new FileOutputStream(file);
                    tempL.compress(Bitmap.CompressFormat.JPEG, 100, fOutputStream);

                    fOutputStream.flush();
                    fOutputStream.close();

                    MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
                    paths.add(file.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
