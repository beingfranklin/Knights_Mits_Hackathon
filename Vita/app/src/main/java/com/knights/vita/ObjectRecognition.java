package com.knights.vita;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ObjectRecognition extends AppCompatActivity {


    private Uri uriPhoto;
    Uri uploadUri;
    private Bitmap bitmap;

    String str,item;

    Integer userId = 1;
    Integer flag = 0;

    String[] xyzing = {"beef liver","sweet potato","bananna","carrot"};



    SharedPreferences sharedPreferences;



    private int STORAGE_PERMISSION_CODE = 23;
    private int CAMERA_PERMISSION_CODE = 24;

    private int LOAD_IMAGE = 1;
    private int TAKE_CAMERA = 2;
    private int CHECK_IMAGE = 3;


//    private StorageReference mStorageRef;
//    private DatabaseReference mDatabaseRef;
//    private StorageMetadata metadata;


    Button camerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_recognition);


        camerButton = (Button) findViewById(R.id.cameraButton);

        sharedPreferences = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId",0);



        StrictMode.VmPolicy.Builder builderStrict = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builderStrict.build());




        camerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (isCameraAllowed()){
                        openCamera();
                        return;
                    }
                    requestCamera();
                }else {
                    openCamera();
                }
            }
        });


    }


    private void openCamera() {

        Log.d("inside","openCamera");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),  "Pic.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        uriPhoto = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_CAMERA);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if(requestCode == TAKE_CAMERA && resultCode == RESULT_OK) {
            Uri selectedImage = uriPhoto;
            uploadUri = uriPhoto;
            getContentResolver().notifyChange(selectedImage, null);
            ContentResolver cr = getContentResolver();


            //byte[] encodeValue = Base64.encode(testValue.getBytes(), Base64.DEFAULT);


            byte[] byteArrayImage;
            String encodedImage = "";

            try {
                bitmap = android.provider.MediaStore.Images.Media
                        .getBitmap(cr, selectedImage);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                byteArrayImage = baos.toByteArray();

                encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
                Log.d("encodedImage",encodedImage);

                //img.setImageBitmap(bitmap);
                //not showing image as already shown in camera view
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                Log.e("Camera", e.toString());
            }

            uploadImage(encodedImage);
        }

    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


//        if(requestCode == STORAGE_PERMISSION_CODE){
//
//            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this,"Permission granted.Click Selfie Again.",Toast.LENGTH_LONG).show();
//            }else{
//                Toast.makeText(this,"Oops you just denied the permission",Toast.LENGTH_LONG).show();
//            }
//        }



        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission granted.Click Selfie Again.",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,"Oops you just denied the permission",Toast.LENGTH_LONG).show();
            }
        }
    }



    private boolean isCameraAllowed() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);

        if (result == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }



    //Requesting permission
    private void requestCamera(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)){

        }
        ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION_CODE);
    }



    public void uploadImage(String encodedImage){


        String url = "https://vision.googleapis.com/v1/images:annotate?key=AIzaSyCtFDOWbGdYmjJrdKs4oUlwhoNR5w79Kew";



        final JsonArray requests = new JsonArray();
        JsonArray features = new JsonArray();
        JsonObject image = new JsonObject();
//        JsonObject source = new JsonObject();
        JsonObject list = new JsonObject();
        JsonObject forFeatures = new JsonObject();

        JsonObject finalObject = new JsonObject();


        JsonObject imageRoot = new JsonObject();
        JsonObject featuresRoot = new JsonObject();


        JsonObject finalOutput = new JsonObject();


        final JsonObject response = new JsonObject();
        


        try{
            image.addProperty("content",encodedImage);
            forFeatures.addProperty("type","LABEL_DETECTION");
            forFeatures.addProperty("maxResults",4);
            features.add(forFeatures);


            Map<String,JsonObject> mapPass = new HashMap<>();


            imageRoot.add("image", image);
            imageRoot.add("features",features);


            requests.add(imageRoot);


            finalOutput.add("requests",requests);

            finalObject = finalOutput.getAsJsonObject();



        }catch (Exception e){

        }



        final SweetAlertDialog comingDialog = new SweetAlertDialog(this,SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("Hurray")
                .setContentText("You can have carrots!")
                .hideConfirmButton();






        Log.d("url",url);


        Ion.with(getApplicationContext())
                .load(url)
                .setJsonObjectBody(finalObject)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {


                        str = result.toString();
                        Log.d("resp",str);
                        String resp2 = " {\"responses\":[{\"labelAnnotations\":[{\"mid\":\"/m/0fj52s\",\"description\":\"carrot\",\"score\":0.9807567,\"topicality\":0.9807567},{\"mid\":\"/m/0f4s2w\",\"description\":\"vegetable\",\"score\":0.9564303,\"topicality\":0.9564303},{\"mid\":\"/m/02wbm\",\"description\":\"food\",\"score\":0.7437834,\"topicality\":0.7437834}]}]}";
                        Log.d("resp",resp2);


                        Log.d("before next","......");

                        nextStep();

                    }
                });

        flag = 0;
        Log.d("length xyz",Integer.toString(xyzing.length));


    }

    public void nextStep(){



        Log.d("inside nextStep",".........");

        final SweetAlertDialog successDialog = new SweetAlertDialog(this,SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Hurray!")
                .setContentText("You can have the item.")
                .hideConfirmButton();

        final SweetAlertDialog errorDialog = new SweetAlertDialog(this,SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Nope")
                .setContentText("Please refrain from having this food!")
                .hideConfirmButton();


        Log.d("str",str);


        for(int i=0;i<xyzing.length;i++) {
            if (str.contains(xyzing[i])) {
                flag = 1;
                item = xyzing[i];
                break;

            }
        }

            if(flag == 1){


                Log.d("elementdfound","........");

                UrlClass urlClass = new UrlClass();
                String rootUrl = urlClass.getUrl();
                final String JSON_URL =rootUrl + "/recs/object/" + item + "?id=" + Integer.toString(userId);
                Log.d("completeUrl",JSON_URL);


                StringRequest stringRequest = new StringRequest(Request.Method.GET, JSON_URL,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                                try {

                                    JSONArray arr = new JSONArray(response);

                                    Log.d("test","success");
                                    Log.d("sample",arr.toString());

                                    JSONObject obj1 = new JSONObject(arr.get(0).toString());
                                    Log.d("obj1 length",Integer.toString(obj1.length()));


                                    JSONObject obj2 = new JSONObject(obj1.get("rec").toString());
                                    Log.d("obj2 length",Integer.toString(obj2.length()));

                                    String status = obj2.get("status").toString();
                                    Log.d("Status",status);
                                    String food = obj2.get("food").toString();
                                    String foodLower = food.toLowerCase();
                                    Log.d("food",food);

                                    if(foodLower.contains(item) && status.equals("true")){

                                        successDialog.show();

                                    }else{
                                        Log.d("iniside...","else");
                                        errorDialog.show();
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("here",".....");
                                Log.d("here",error.toString());
                                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });


                RequestQueue requestQueue = Volley.newRequestQueue(this);
                requestQueue.add(stringRequest);


        }else{
                Log.d("iniside...","else");
                errorDialog.show();
            }

            }

    }



