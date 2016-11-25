package com.wearabletattoos.diana.tatty;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.files.BackendlessFile;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.exceptions.BackendlessFault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ScanTattooActivity extends ActionBarActivity {

    private AlertDialog mEnableNfc;
    private boolean mResume = true;
    private Intent mOldIntent = null;
    private TextView tagID;
    private String tagIDVal;
    private TextView tatUsername;
    private EditText tatName;
    private EditText tatMsg;
    private ImageButton tatImg;
    private Button saveBtn;

    private BackendlessUser user;
    private BackendlessFile image;

    private Tattoo tattoo;
    private Object[] userTattoos;
    private ArrayList<Object> userTattooList;

    private boolean isOwner;
    private boolean tagExists;

    /**
     * Check for NFC hardware, Mifare Classic support and for external storage.
     * If the directory structure and the std. keys files is not already there
     * it will be created. Also, at the first run of this App, a warning
     * notice and a donate message will be displayed.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_tattoo);

        user = Backendless.UserService.CurrentUser();
        if (user == null) {
            Toast.makeText(this, "Loading user information...", Toast.LENGTH_LONG).show();
            String whereClause = "email = " + getIntent().getStringExtra("email");
            BackendlessDataQuery query = new BackendlessDataQuery();
            query.setWhereClause(whereClause);
            BackendlessCollection<BackendlessUser> result =
                    Backendless.Persistence.of(BackendlessUser.class).find(query);
            user = result.getData().get(0);
        }

        //in application, keep track of universal username obj,
        //only allow modification if your username matches the one belonging to the tattoo

        //display found id on screen
        tagID = (TextView) findViewById(R.id.nfc_screen_id_text);

        //fetch username from backendless and replace
        tatUsername = (TextView) findViewById(R.id.username_text);

        //fetch tatname and replace
        tatName = (EditText) findViewById(R.id.tattoo_name_edit);

        //fetch tatmsg and replace
        tatMsg = (EditText) findViewById(R.id.tattoo_message_edit);

        //fetch tatimg, also change default img to neutral one
        tatImg = (ImageButton) findViewById(R.id.tattoo_image);
        tatImg.setEnabled(false);

        //give backendless command to take these data
        saveBtn = (Button) findViewById(R.id.save_btn);


        // Check if there is an NFC hardware component.
        Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
        if (Common.getNfcAdapter() == null) {
            Toast.makeText(this,"No NFC Adapter Available",Toast.LENGTH_LONG).show();
            mResume = false;
        }

        // Create a dialog that send user to NFC settings if NFC is off.
        // (Or let the user use the App in editor only mode / exit the App.)
        mEnableNfc = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_nfc_not_enabled_title)
                .setMessage(R.string.dialog_nfc_not_enabled)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_nfc,
                        new DialogInterface.OnClickListener() {
                            @Override
                            @SuppressLint("InlinedApi")
                            public void onClick(DialogInterface dialog, int which) {
                                // Goto NFC Settings.
                                if (Build.VERSION.SDK_INT >= 16) {
                                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                                } else {
                                    startActivity(new Intent(
                                            Settings.ACTION_WIRELESS_SETTINGS));
                                }
                            }
                        })
                .setNegativeButton(R.string.action_exit_app,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Exit the App.
                                finish();
                            }
                        }).create();

        // Check if there is Mifare Classic support.
        if (!Common.hasMifareClassicSupport()) {
            // Disable read/write tag options.
            CharSequence styledText = Html.fromHtml(
                    getString(R.string.dialog_no_mfc_support_device));
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_no_mfc_support_device_title)
                    .setMessage(styledText)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.action_exit_app,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .setNegativeButton(R.string.action_continue,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    mResume = true;
                                    checkNfc();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
            // Make links clickable.
            ((TextView)ad.findViewById(android.R.id.message)).setMovementMethod(
                    LinkMovementMethod.getInstance());
            mResume = false;
        }
    }

    public void chooseNewImage(View v) {
        if (isOwner) {
            //make this an option between taking photo or choosing photo from library
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle(R.string.change_image)
                    .setPositiveButton(R.string.new_photo,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent takePictureIntent = new Intent(
                                            MediaStore.ACTION_IMAGE_CAPTURE);
                                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                        startActivityForResult(takePictureIntent, 1);
                                    }
                                }
                            })
                    .setNegativeButton(R.string.existing_photo,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent getPictureIntent = new Intent();
                                    getPictureIntent.setType("image/*");
                                    getPictureIntent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent
                                            .createChooser(getPictureIntent, "Select Image"), 2);
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    public void saveTattoo(View v) {
        tattoo.setUID(tagIDVal);
        tattoo.setObjectID(tagIDVal);
        tattoo.setName(tatName.getText().toString());
        tattoo.setMessage(tatMsg.getText().toString());
        //tattoo.setImage(image);  //change to imageURL
        Toast.makeText(ScanTattooActivity.this, tattoo.toString(), Toast.LENGTH_LONG).show();
        if (!tagExists) {
            Backendless.Persistence.of(Tattoo.class).save(tattoo, new AsyncCallback<Tattoo>() {
                @Override
                public void handleResponse(Tattoo tat) {
                    Toast.makeText(ScanTattooActivity.this, tat.toString(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Toast.makeText(ScanTattooActivity.this, "There was an error saving", Toast.LENGTH_LONG).show();
                }
            });
        }
        else {

        }

        if (userTattooList != null) {
            user.setProperty("tattoos", userTattooList.toArray());
        }
        Backendless.UserService.update(user, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Toast.makeText(ScanTattooActivity.this, "Error updating user", Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * If resuming is allowed because all dependencies from
     * {@link #onCreate(Bundle)} are satisfied, call
     * {@link #checkNfc()}
     * @see #onCreate(Bundle)
     * @see #checkNfc()
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mResume) {
            checkNfc();
        }
    }

    /**
     * Check if NFC adapter is enabled. If not, show the user a dialog and let
     * him choose between "Goto NFC Setting", "Use Editor Only" and "Exit App".
     * Also enable NFC foreground dispatch system.
     * @see Common#enableNfcForegroundDispatch(Activity)
     */
    private void checkNfc() {
        // Check if the NFC hardware is enabled.
        if (Common.getNfcAdapter() != null
                && !Common.getNfcAdapter().isEnabled()) {
            // NFC is disabled. Show dialog.
            // Use as editor only?
            mEnableNfc.show();
        } else {
            // NFC is enabled. Hide dialog and enable NFC
            // foreground dispatch.
            if (mOldIntent != getIntent()) {
                String typeCheck = Common.treatAsNewTag(getIntent(), this);
                tagID.setText("UID: " + typeCheck);
                mOldIntent = getIntent();
            }
            Common.enableNfcForegroundDispatch(this);
            mEnableNfc.hide();
        }
    }


    /**
     * Disable NFC foreground dispatch system.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onPause() {
        super.onPause();

        Common.disableNfcForegroundDispatch(this);
    }

    /**
     * Handle new Intent as a new tag Intent and if the tag/device does not
     * support Mifare Classic, tell it you suck
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     */
    @Override
    public void onNewIntent(Intent intent) {
        // get UID
        tagIDVal = Common.treatAsNewTag(intent, this);
        tagID.setText("UID: " + tagIDVal);

        // determine ownership
        isOwner = false;
        tagExists = false;
        String whereClause = "uid = " + tagIDVal;
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause(whereClause);
        Backendless.Persistence.of(Tattoo.class).find(dataQuery,
                new AsyncCallback<BackendlessCollection<Tattoo>>() {
                    @Override
                    public void handleResponse(BackendlessCollection<Tattoo> tatCollection) {
                        if (tatCollection == null) {
                            isOwner = false;
                            tagExists = false;
                        }
                        else {
                            tattoo = tatCollection.getData().get(0);
                            tagExists = true;
                            Log.d("hey", "getting user email");
                            isOwner = (user.getEmail() == tattoo.getOwner());
                            Log.d("hey", "got user email");
                        }
                        // tatUsr is null so you can claim this tattoo
                        if (isOwner & !tagExists) {
                            Log.d("WARN", "i think i own this already");
                            tattoo.setUID(tagIDVal);
                            tattoo.setOwner(user.getEmail());
                            Log.d("hey", "getting user tats");
                            userTattoos = (Object[]) user.getProperty("tattoos");
                            userTattooList = new ArrayList<Object>(Arrays.asList(userTattoos));
                            if (!userTattooList.contains(tattoo)) {
                                userTattooList.add(tattoo);
                            }
                            Log.d("hey", "got user tats");

                            tatUsername.setText("Username: " + user.getEmail().toString());
                            tatName.setText("");
                            tatMsg.setText("");
                        }

                        else if (tagExists) {
                            Log.d("WARN", "this tag exists but belongs to someone else");
                            tatUsername.setText("Username: " + tattoo.getOwner());
                            tatName.setText(tattoo.getName());
                            tatMsg.setText(tattoo.getMessage());
                            tatImg.setImageBitmap(tattoo.getImage());
                        }

                        if (!isOwner) {
                            tatName.setEnabled(false);
                            tatName.setFocusable(false);
                            tatName.setBackgroundColor(0x0FFFFFFF);
                            tatMsg.setEnabled(false);
                            tatMsg.setFocusable(false);
                            tatMsg.setBackgroundColor(0x0FFFFFFF);
                            tatImg.setEnabled(false);
                            saveBtn.setEnabled(false);
                            saveBtn.setVisibility(View.GONE);

                        } else {
                            tatName.setEnabled(true);
                            tatName.setFocusable(true);
                            tatName.setBackgroundColor(0x0F0F0F0F);
                            tatMsg.setEnabled(true);
                            tatMsg.setFocusable(true);
                            tatMsg.setBackgroundColor(0x0F0F0F0F);
                            tatImg.setEnabled(true);
                            saveBtn.setEnabled(true);
                            saveBtn.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        isOwner = true;
                        tagExists = false;
                        tattoo = new Tattoo();
                        tattoo.setUID(tagIDVal);
                        tattoo.setOwner(user.getEmail());
                        Log.d("aah", user.getProperties().toString());
                        userTattoos = (Object[]) user.getProperty("tattoos");
                        userTattooList = new ArrayList<Object>(Arrays.asList(userTattoos));
                        if (!userTattooList.contains(tattoo)) {
                            userTattooList.add(tattoo);
                        }
                        Log.d("hey", "got user tats");

                        tatUsername.setText("Username: " + user.getEmail());
                        tatName.getText().clear();
                        tatMsg.getText().clear();

                        tatName.setEnabled(true);
                        tatName.setFocusable(true);
                        tatName.setBackgroundColor(0x0F0F0F0F);
                        tatMsg.setEnabled(true);
                        tatMsg.setFocusable(true);
                        tatMsg.setBackgroundColor(0x0F0F0F0F);
                        tatImg.setEnabled(true);
                        saveBtn.setEnabled(true);
                        saveBtn.setVisibility(View.VISIBLE);

                    }

        });
    }

    //crashes after selection, something wrong here
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imgBit = (Bitmap) extras.get("data");
            tatImg.setImageBitmap(imgBit);
            /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imgBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();*/

            String filePath = "/" + Common.applicationID + "/" + Common.appVersion +
                    "/images/";
            String fileName = tattoo.getName() + "_image";
            Backendless.Files.Android.upload(imgBit, Bitmap.CompressFormat.JPEG, 100, fileName,
                    filePath, new AsyncCallback<BackendlessFile>() {
                        @Override
                        public void handleResponse(final BackendlessFile file) {
                        }

                        @Override
                        public void handleFault(BackendlessFault fault) {
                            Toast.makeText(ScanTattooActivity.this, "Trouble saving image",
                                    Toast.LENGTH_LONG).show();
                        }

                    });

            /*if (byteArray != null) {
                image = new ParseFile(tatName.toString() + ".jpg", byteArray);
                try {
                    image.save();
                } catch (ParseException e) {
                    if (e == null) {
                        tattoo.put("image", image);
                        Toast.makeText(getApplicationContext(), "Saved picture.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Could not save picture.", Toast.LENGTH_LONG).show();
                    }
                }
            }*/
        }

        else if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            try{
                Bitmap imgBit = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                tatImg.setImageBitmap(imgBit);
                /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imgBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();*/

                String filePath = "/" + Common.applicationID + "/" + Common.appVersion +
                        "/images/";
                String fileName = tattoo.getName() + "_image";
                Backendless.Files.Android.upload(imgBit, Bitmap.CompressFormat.JPEG, 100, fileName,
                        filePath, new AsyncCallback<BackendlessFile>() {
                            @Override
                            public void handleResponse(final BackendlessFile file) {
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Toast.makeText(ScanTattooActivity.this, "Trouble saving image",
                                        Toast.LENGTH_LONG).show();
                            }

                        });

                /*if (byteArray != null) {
                    image = new ParseFile(tatName.toString() + ".jpg", byteArray);
                    try {
                        Toast.makeText(getApplicationContext(), "Saving Image", Toast.LENGTH_LONG).show();
                        image.save();
                    } catch (ParseException e) {
                        if (e == null) {
                            tattoo.put("image", image);
                            Toast.makeText(getApplicationContext(), "Saved picture.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Could not save picture.", Toast.LENGTH_LONG).show();
                        }
                    }
                }*/
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

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
}
