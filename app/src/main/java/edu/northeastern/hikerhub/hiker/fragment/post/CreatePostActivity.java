package edu.northeastern.hikerhub.hiker.fragment.post;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import edu.northeastern.hikerhub.R;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CreatePostActivity extends AppCompatActivity {

    private TextInputLayout tilPostTitle;
    private TextInputEditText etPostTitle;
    private TextInputLayout tilPostContent;
    private TextInputEditText etPostContent;
    private MaterialButton btnSubmitPost;
    private Spinner spinnerCategory;
    private CheckBox checkBoxRecommend;
    DatabaseReference postsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        tilPostTitle = findViewById(R.id.til_post_title);
        etPostTitle = findViewById(R.id.et_post_title);
        tilPostContent = findViewById(R.id.til_post_content);
        etPostContent = findViewById(R.id.et_post_content);
        btnSubmitPost = findViewById(R.id.btn_submit_post);
        spinnerCategory = findViewById(R.id.spinner_category);
        checkBoxRecommend = findViewById(R.id.checkbox_recommend);

        // Set up the Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnSubmitPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etPostTitle.getText().toString().trim();
                String content = etPostContent.getText().toString().trim();
                String category = spinnerCategory.getSelectedItem().toString().trim();
                boolean isRecommended = checkBoxRecommend.isChecked();
                String currentDate = LocalDate.now().toString();


                if (validateInput(title, content, category)) {
                    // Save the new post to your backend here
                    saveNewPost(title, content, category, isRecommended, currentDate);
                }
            }
        });
    }

    private boolean validateInput(String title, String content, String category) {
        boolean isValid = true;

        if (title.isEmpty()) {
            tilPostTitle.setError(getString(R.string.error_title_empty));
            isValid = false;
        } else {
            tilPostTitle.setError(null);
        }

        if (content.isEmpty()) {
            tilPostContent.setError(getString(R.string.error_content_empty));
            isValid = false;
        } else {
            tilPostContent.setError(null);
        }

        if (category.isEmpty()) {
            tilPostContent.setError(getString(R.string.error_category_empty));
            isValid = false;
        }

        return isValid;
    }

    private void saveNewPost(String title, String content, String category, boolean isRecommended, String postDate) {
        // Backend logic
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        postsRef = database.getReference("posts");

        // Create a new post object with the input data
        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("content", content);
        post.put("category", category);
        post.put("recommend", isRecommended);
        post.put("postDate", postDate);

        // Generate a new unique key for the post
        String key = postsRef.push().getKey();

        // Add the post object to the "posts" reference in the Realtime Database
        postsRef.child(key).setValue(post)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(CreatePostActivity.this, getString(R.string.post_created), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreatePostActivity.this, getString(R.string.post_creation_failed), Toast.LENGTH_SHORT).show();
                    }
                });

        // After saving, return to MainActivity and update the list of posts
        Toast.makeText(CreatePostActivity.this, getString(R.string.post_created), Toast.LENGTH_SHORT).show();
        finish();
    }
}

