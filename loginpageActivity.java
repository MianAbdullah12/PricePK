package com.example.pricepk;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MotionEvent;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class loginpageActivity extends AppCompatActivity {

    EditText LoginUsername, LoginPassword;
    Button btnLogin;
    TextView tvSignUp, tvForgetPassword;
    CheckBox rememberMeCheckBox;
    FirebaseAuth mAuth;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private static final String PREF_NAME = "loginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;

    // 🔵 Define Admin email
    private static final String ADMIN_EMAIL = "amian1886@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginpage);

        LoginUsername = findViewById(R.id.LoginUsername);
        LoginPassword = findViewById(R.id.LoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgetPassword = findViewById(R.id.tvForgetPassword);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);

        mAuth = FirebaseAuth.getInstance();

        SignInButton btnGoogle = findViewById(R.id.btnGoogle);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        if (sharedPreferences.getBoolean(KEY_REMEMBER, false)) {
            LoginUsername.setText(sharedPreferences.getString(KEY_EMAIL, ""));
            LoginPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
            rememberMeCheckBox.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> loginUser());

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(loginpageActivity.this, SignupPageActivity.class);
            startActivity(intent);
        });

        LoginPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (LoginPassword.getRight() - LoginPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (LoginPassword.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                        LoginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        LoginPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password, 0, R.drawable.visibility_off, 0);
                    } else {
                        LoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        LoginPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password, 0, R.drawable.visibility, 0);
                    }
                    return true;
                }
            }
            return false;
        });

        tvForgetPassword.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Reset Password");

            final EditText input = new EditText(this);
            input.setHint("Enter your registered email");
            input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            builder.setView(input);

            builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
                String email = input.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(loginpageActivity.this, "Email required", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(unused -> Toast.makeText(loginpageActivity.this, "Reset link sent to your email", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(loginpageActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    private void loginUser() {
        String email = LoginUsername.getText().toString().trim();
        String password = LoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.getEmail().equals(ADMIN_EMAIL)) {
                            DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("admins").child(user.getUid());
                            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Toast.makeText(loginpageActivity.this, "Admin Login Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(loginpageActivity.this, AdminPage.class));
                                    } else {
                                        Toast.makeText(loginpageActivity.this, "User Login Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(loginpageActivity.this, HomePage.class));
                                    }
                                    finish();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(loginpageActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(loginpageActivity.this, "User Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(loginpageActivity.this, HomePage.class));
                            finish();
                        }

                        if (rememberMeCheckBox.isChecked()) {
                            editor.putString(KEY_EMAIL, email);
                            editor.putString(KEY_PASSWORD, password);
                            editor.putBoolean(KEY_REMEMBER, true);
                            editor.apply();
                        } else {
                            editor.clear();
                            editor.apply();
                        }
                    } else {
                        Toast.makeText(loginpageActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Toast.makeText(this, "Account: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                }
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Google Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomePage.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Firebase Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}