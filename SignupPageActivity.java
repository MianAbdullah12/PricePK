package com.example.pricepk;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;

import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.MotionEvent;

public class SignupPageActivity extends AppCompatActivity {

    EditText inputEmail, inputPhone, inputProvince, inputCity, inputAddress, inputDOB, inputPassword, inputConfirmPassword;
    Button btnSignUp;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_page);

        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputProvince = findViewById(R.id.inputProvince);
        inputCity = findViewById(R.id.inputCity);
        inputAddress = findViewById(R.id.inputAddress);
        inputDOB = findViewById(R.id.inputDOB);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        inputPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (inputPassword.getRight() - inputPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (inputPassword.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                        inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        inputPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password, 0, R.drawable.visibility_off, 0);
                    } else {
                        inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        inputPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password, 0, R.drawable.visibility, 0);
                    }
                    return true;
                }
            }
            return false;
        });

        inputConfirmPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (inputConfirmPassword.getRight() - inputConfirmPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (inputConfirmPassword.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                        inputConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        inputConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password, 0, R.drawable.visibility_off, 0);
                    } else {
                        inputConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        inputConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password, 0, R.drawable.visibility, 0);
                    }
                    return true;
                }
            }
            return false;
        });

        inputDOB.setOnClickListener(v -> showDatePicker());

        btnSignUp.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();
            String province = inputProvince.getText().toString().trim();
            String city = inputCity.getText().toString().trim();
            String address = inputAddress.getText().toString().trim();
            String dob = inputDOB.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String confirmPassword = inputConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || phone.isEmpty() || province.isEmpty() || city.isEmpty() ||
                    address.isEmpty() || dob.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                inputConfirmPassword.setError("Passwords do not match");
                return;
            }

            if (password.length() < 6) {
                inputPassword.setError("Password must be at least 6 characters");
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String userId = mAuth.getCurrentUser().getUid();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("phone", phone);
                        userMap.put("province", province);
                        userMap.put("city", city);
                        userMap.put("address", address);
                        userMap.put("dob", dob);

                        mDatabase.child(userId).setValue(userMap)
                                .addOnSuccessListener(unused -> {
                                    // ✅ Immediately logout user after signup
                                    mAuth.signOut();

                                    Toast.makeText(SignupPageActivity.this, "Signup Successful! Please login now.", Toast.LENGTH_SHORT).show();
                                    new android.os.Handler().postDelayed(() -> {
                                        Intent intent = new Intent(SignupPageActivity.this, loginpageActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }, 1500);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignupPageActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignupPageActivity.this, "Signup Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    inputDOB.setText(date);
                }, year, month, day);

        datePickerDialog.show();
    }
}
