 package com.example.pets;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.pets.data.PetContract.PetEntry;
import com.example.pets.data.PetDbHelper;

 public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {


     Uri mCurrentPetUri;

     /** Identifier for the pet data loader */
     private static final int EXISTING_PET_LOADER = 0;

    /** EditText field to enter the pet's name */
    private EditText mNameEditText;

    /** EditText field to enter the pet's breed */
    private EditText mBreedEditText;

    /** EditText field to enter the pet's weight */
    private EditText mWeightEditText;

    /** EditText field to enter the pet's gender */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN ;

     /** Boolean flag that keeps track of whether the pet has been edited (true) or not (false) */
     private boolean mPetHasChanged = false;

     /**
      * OnTouchListener that listens for any user touches on a View, implying that they are modifying
      * the view, and we change the mPetHasChanged boolean to true.
      */

     private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
         @Override
         public boolean onTouch(View v, MotionEvent event) {
             mPetHasChanged= true;
             return false;
         }
     };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();

       mCurrentPetUri = intent.getData();

        if (mCurrentPetUri==null){
            setTitle(getString(R.string.editor_activity_title_new_pet));
            invalidateOptionsMenu();
        }else {
            setTitle(getString(R.string.editor_activity_title_edit_pet));

            // Initialize a loader to read the pet data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PET_LOADER,null,this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();
    }

    private void setupSpinner(){
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout

        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,R.array.array_gender_options, android.R.layout.simple_dropdown_item_1line);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);
        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN; // Unknown
            }
        });
    }

    private void savePet(){
       String nameString = mNameEditText.getText().toString().trim();
       String breedString= mBreedEditText.getText().toString().trim();
       String weightString= mWeightEditText.getText().toString().trim();
      // int weight= Integer.parseInt(weightString);

        // Check if this is supposed to be a new pet
        // and check if all the fields in the editor are blank
        if (mCurrentPetUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(breedString) &&
                TextUtils.isEmpty(weightString) && mGender == PetEntry.GENDER_UNKNOWN) {
            // Since no fields were modified, we can return early without creating a new pet.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }



        ContentValues values= new ContentValues();

        values.put(PetEntry.COLUMN_PET_NAME,nameString);
        values.put(PetEntry.COLUMN_PET_BREED,breedString);
        values.put(PetEntry.COLUMN_PET_GENDER,mGender);
       // values.put(PetEntry.COLUMN_PET_WEIGHT,weight);

        // If the weight is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int weight = 0;
        if (!TextUtils.isEmpty(weightString)) {
            weight = Integer.parseInt(weightString);
        }
        values.put(PetEntry.COLUMN_PET_WEIGHT, weight);




        // Show a toast message depending on whether or not the insertion was successful
        if (mCurrentPetUri == null) {
            // This is a NEW pet, so insert a new pet into the provider,
            // returning the content URI for the new pet.
           Uri newUri =  getContentResolver().insert(PetEntry.CONTENT_URI,values);

           if (newUri==null){
               Toast.makeText(this,getString(R.string.editor_insert_pet_failed),Toast.LENGTH_SHORT).show();
           } else {
               Toast.makeText(this,getString(R.string.editor_insert_pet_successful),Toast.LENGTH_SHORT).show();
           }

        } else {
            // Otherwise this is an EXISTING pet, so update the pet with content URI: mCurrentPetUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentPetUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentPetUri,values,null,null);

            if (rowsAffected==0){
                Toast.makeText(this,getString(R.string.editor_update_pet_failed),Toast.LENGTH_SHORT).show();
            } else{
              Toast.makeText(this,getString(R.string.editor_update_pet_Successful),Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);
         // If this is a new pet, hide the "Delete" menu item.
         if (mCurrentPetUri==null){
             MenuItem menuItem = menu.findItem(R.id.action_delete);
             menuItem.setVisible(false);
         }
         return true;
     }

     @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // save pet to database
                savePet();
                //Exit activity
                finish();
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Do nothing for now
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}
                if (!mPetHasChanged){
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.

                DialogInterface.OnClickListener discardButtonClickListener =new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked "Discard" button, navigate to parent activity.
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };


                //show the dialog to user that have some unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

     @Override
     public void onBackPressed() {

        if (!mPetHasChanged){
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked "Discard" button, close the current activity.
                finish();
            }
        };
         // Show dialog that there are unsaved changes
         showUnsavedChangesDialog(discardButtonClickListener);
     }

     /**
      * This method is called when the back button is pressed.
      */


     @Override
     public Loader<Cursor> onCreateLoader(int id, Bundle args) {
         // Since the editor shows all pet attributes, define a projection that contains
         // all columns from the pet table
         String[] projection = {
                 PetEntry._ID,
                 PetEntry.COLUMN_PET_NAME,
                 PetEntry.COLUMN_PET_BREED,
                 PetEntry.COLUMN_PET_GENDER,
                 PetEntry.COLUMN_PET_WEIGHT };

         // This loader will execute the ContentProvider's query method on a background thread

       return new CursorLoader(this,
               mCurrentPetUri,
               projection,
               null,
               null,
               null
               );
     }

     @Override
     public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
         // Bail early if the cursor is null or there is less than 1 row in the cursor
         if (cursor == null || cursor.getCount() < 1) {
             return;
         }

         // Proceed with moving to the first row of the cursor and reading data from it
         // (This should be the only row in the cursor)
         if (cursor.moveToFirst()){
             int nameColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
             int breedColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED);
             int genderColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
             int weightColumnIndex=  cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);


             String name= cursor.getString(nameColumnIndex);
             String breed = cursor.getString(breedColumnIndex);
             int gender = cursor.getInt(genderColumnIndex);
             int weight = cursor.getInt(weightColumnIndex);

             mNameEditText.setText(name);
             mBreedEditText.setText(breed);
             mWeightEditText.setText(Integer.toString(weight));


             switch (gender){
                 case PetEntry.GENDER_MALE:
                     mGenderSpinner.setSelection(1);
                     break;
                 case PetEntry.GENDER_FEMALE:
                     mGenderSpinner.setSelection(2);
                     break;
                 default:
                     mGenderSpinner.setSelection(0);
                     break;
             }
         }

     }

     @Override
     public void onLoaderReset(Loader<Cursor> loader) {
         // If the loader is invalidated, clear out all the data from the input fields.
         mNameEditText.setText("");
         mBreedEditText.setText("");
         mWeightEditText.setText("");
         mGenderSpinner.setSelection(0); // Select "Unknown" gender
     }

     private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener){
         // Create an AlertDialog.Builder and set the message, and click listeners
         // for the Positive and negative buttons on the dialog.
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setMessage(R.string.unsaved_changes_dialog_msg);
         builder.setPositiveButton(R.string.discard,discardButtonClickListener);
         builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 // User clicked the "Keep editing" button, so dismiss the dialog
                 // and continue editing the pet.

                 if (dialog!=null){
                      dialog.dismiss();
                 }
             }
         });
         AlertDialog alertDialog = builder.create();
         alertDialog.show();
     }

     private void showDeleteConfirmationDialog() {
         // Create an AlertDialog.Builder and set the message, and click listeners
         // for the postivie and negative buttons on the dialog.
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setMessage(R.string.delete_dialog_msg);
         builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 // User clicked the "Delete" button, so delete the pet.
                 deletePet();
             }
         });
         builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 // User clicked the "Cancel" button, so dismiss the dialog
                 // and continue editing the pet.
                 if (dialog != null) {
                     dialog.dismiss();
                 }
             }
         });

         // Create and show the AlertDialog
         AlertDialog alertDialog = builder.create();
         alertDialog.show();
     }

     /**
      * Perform the deletion of the pet in the database.
      */
     private void deletePet()
     {
         // Only perform the delete if this is an existing pet.
         if (mCurrentPetUri != null) {
             // Call the ContentResolver to delete the pet at the given content URI.
             // Pass in null for the selection and selection args because the mCurrentPetUri
             // content URI already identifies the pet that we want.
             int rowsDeleted = getContentResolver().delete(mCurrentPetUri,null,null);

             if (rowsDeleted==0){
                 Toast.makeText(this,getString(R.string.editor_delete_pet_failed),Toast.LENGTH_SHORT).show();
             }else{
                 Toast.makeText(this,getString(R.string.editor_delete_pet_successful),Toast.LENGTH_SHORT).show();
             }
         }

         finish();
     }

 }