package com.example.donorschoose;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class CharityProfile extends AppCompatActivity {

    private FirebaseFirestore db;
    private int buttonFlag = 0;
    DocumentSnapshot charityDocument;

    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;
    ImageView backgroundImage;

    public void loadUser(View view) { startActivity(new Intent(this, UserProfile.class)); }
    public void goHome(View view) { startActivity(new Intent(this, Home.class)); }

    public void logout(View view)
    {
        FirebaseAuth.getInstance().signOut();
        startActivity((new Intent(this, Login.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charity_profile);

        db = FirebaseFirestore.getInstance();

        Bundle extras = getIntent().getExtras();
        retrieveCharityData(extras.getString("Charity ID"));
        if (extras.getString("Access Level").equals("Edit")) makeEditable();
    }

    public void retrieveCharityData(String charityID)
    {
        db.collection("charities").document(charityID).addSnapshotListener(CharityProfile.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                displayProfile(documentSnapshot);
            }
        });
    }

    public void displayProfile(DocumentSnapshot document)
    {
        charityDocument = document;

        TextView nameTextView = (TextView) findViewById(R.id.nameTextView);
        TextView descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);
        ImageView imageView = (ImageView) findViewById(R.id.backgroundImage);

        nameTextView.setText(charityDocument.getString("name"));
        descriptionTextView.setText(charityDocument.getString("description"));

        String downloadUrl = charityDocument.getString("background");
        if (!downloadUrl.isEmpty())
        {
            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl);
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) { Picasso.with(getApplicationContext()).load(uri.toString()).into(imageView); }});
        }
    }

    private void makeEditable()
    {
        ((ImageButton) findViewById(R.id.homeButton)).setVisibility(View.GONE);
        ((ImageButton) findViewById(R.id.profileButton)).setVisibility(View.GONE);
        ((Button) findViewById(R.id.donateButton)).setText("Edit");
        buttonFlag = 1;
        backgroundImage = (ImageView) findViewById(R.id.backgroundImage);
    }

    public void buttonHandler(View view)
    {
        if (buttonFlag == 0)  donate();
        else if (buttonFlag == 1) editProfile();
        else if (buttonFlag == 2) saveProfile();
    }

    private void editProfile()
    {
        ((Button) findViewById(R.id.donateButton)).setText("Save");
        buttonFlag = 2;

        TextView name = ((TextView) findViewById(R.id.nameTextView));
        TextView description = ((TextView) findViewById(R.id.descriptionTextView));

        name.setEnabled(true);
        description.setEnabled(true);

        name.setBackgroundResource(android.R.drawable.editbox_background_normal);
        description.setBackgroundResource(android.R.drawable.editbox_background_normal);

        backgroundImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageChange();
            }
        });
    }

    private void imageChange()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null )
        {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                backgroundImage.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void saveProfile()
    {
        TextView name = ((TextView) findViewById(R.id.nameTextView));
        TextView description = ((TextView) findViewById(R.id.descriptionTextView));

        name.setEnabled(false);
        description.setEnabled(false);

        name.setBackgroundResource(android.R.color.white);
        description.setBackgroundResource(android.R.color.white);

        backgroundImage.setOnClickListener(null);
        updatePicture();

        charityDocument.getReference().update("name", name.getText().toString());
        charityDocument.getReference().update("description", description.getText().toString());

        makeEditable();
    }

    private void updatePicture()
    {
        if (filePath != null)
        {
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("images/" + userID);
            ref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    charityDocument.getReference().update("background", ref.toString());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(CharityProfile.this, "Failed to save image.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void donate()
    {
        Intent donatePage = new Intent(this, Donate.class);
        donatePage.putExtra("Charity ID", charityDocument.getId());
        donatePage.putExtra("Charity Name", charityDocument.get("name").toString());
        startActivity(donatePage);
    }

}