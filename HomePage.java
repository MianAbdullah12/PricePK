package com.example.pricepk;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout categoryList;
    private RecyclerView productRecyclerView;
    private ProductAdapter productAdapter;
    private List<Category> categories = new ArrayList<>();
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton floatingActionButton;

    // QR Code Scanner Result Launcher
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Cancelled QR scan", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            searchProductInDatabase(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        categoryList = findViewById(R.id.category_list);
        productRecyclerView = findViewById(R.id.product_recycler_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        floatingActionButton = findViewById(R.id.floatingActionButton);

        // Setup RecyclerView
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, new ArrayList<>(), false, null);
        productRecyclerView.setAdapter(productAdapter);

        // Setup Navigation Drawer
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
                return true;
            } else if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_profile) {
                showProfileDialog();
                return true;
            } else if (id == R.id.nav_messages) {
                showMessagesFragment();
                return true;
            } else if (id == R.id.nav_share) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });

        // Setup Bottom Navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Fragment fragment = null;
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    loadFragment(null);
                    return true;
                } else if (id == R.id.nav_report) {
                    fragment = new ReportFragment();
                } else if (id == R.id.nav_offers) {
                    fragment = new OffersFragment();
                } else if (id == R.id.nav_help) {
                    fragment = new HelpFragment();
                }
                if (fragment != null) {
                    loadFragment(fragment);
                    return true;
                }
                return false;
            }
        });

        // Setup QR Code Scanner
        floatingActionButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            } else {
                startQrCodeScanner();
            }
        });

        // Set click listener for search icon
        ImageView searchIcon = findViewById(R.id.search_icon);
        searchIcon.setOnClickListener(v -> showSearchDialog());

        // Fetch categories from Firebase
        fetchCategories();

        // Set default fragment to Home
        loadFragment(null);
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fragment != null) {
            fragmentTransaction.replace(R.id.content_frame, fragment);
            findViewById(R.id.main_content).setVisibility(View.GONE);
        } else {
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.content_frame);
            if (currentFragment != null) {
                fragmentTransaction.remove(currentFragment);
            }
            findViewById(R.id.main_content).setVisibility(View.VISIBLE);
        }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void fetchCategories() {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("categories");
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                categoryList.removeAllViews();
                categories.clear();

                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    Category category = new Category();
                    category.setId(categorySnapshot.getKey());
                    category.setName(categorySnapshot.child("name").getValue(String.class));

                    List<Product> products = new ArrayList<>();
                    DataSnapshot productsSnapshot = categorySnapshot.child("products");
                    for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                        Product product = productSnapshot.getValue(Product.class);
                        if (product != null) {
                            product.setId(productSnapshot.getKey());
                            products.add(product);
                        }
                    }
                    category.setProducts(products);
                    categories.add(category);

                    View categoryView = LayoutInflater.from(HomePage.this).inflate(R.layout.category_item, categoryList, false);
                    TextView categoryNameText = categoryView.findViewById(R.id.category_name);
                    ImageView editCategory = categoryView.findViewById(R.id.edit_category);
                    ImageView deleteCategory = categoryView.findViewById(R.id.delete_category);

                    editCategory.setVisibility(View.GONE);
                    deleteCategory.setVisibility(View.GONE);

                    categoryNameText.setText(category.getName());

                    categoryView.setOnClickListener(v -> productAdapter.updateProducts(category.getProducts()));

                    categoryList.addView(categoryView);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomePage.this, "Error fetching categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // QR Code Scanner
    private void startQrCodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR code");
        options.setBeepEnabled(true);
        options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity.class);
        qrCodeLauncher.launch(options);
    }

    // Search functionality
    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Product");

        final EditText searchInput = new EditText(this);
        searchInput.setHint("Enter product name");
        searchInput.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(searchInput);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String searchQuery = searchInput.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                searchProductInDatabase(searchQuery);
            } else {
                Toast.makeText(this, "Please enter a product name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void searchProductInDatabase(String searchQuery) {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("categories");
        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> productDetailsList = new ArrayList<>();
                boolean productFound = false;

                // Loop through each category
                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    String categoryName = categorySnapshot.child("name").getValue(String.class);
                    DataSnapshot productsSnapshot = categorySnapshot.child("products");

                    // Loop through each product in the category
                    for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                        String productName = productSnapshot.child("name").getValue(String.class);
                        if (productName != null && productName.toLowerCase().contains(searchQuery.toLowerCase())) {
                            productFound = true;
                            StringBuilder productDetails = new StringBuilder();
                            productDetails.append("Product: ").append(productName).append("\n");
                            productDetails.append("Category: ").append(categoryName != null ? categoryName : "Unknown").append("\n");

                            // Fetch pricePerKg as Double
                            Double pricePerKg = productSnapshot.child("pricePerKg").getValue(Double.class);
                            if (pricePerKg != null) {
                                productDetails.append("Price per Kg: ").append(pricePerKg).append("\n");
                            } else {
                                productDetails.append("Price per Kg: Not available\n");
                            }

                            // Fetch discountRate as Double
                            Double discountRate = productSnapshot.child("discountRate").getValue(Double.class);
                            if (discountRate != null) {
                                productDetails.append("Discount Rate: ").append(discountRate).append("%\n");
                            } else {
                                productDetails.append("Discount Rate: Not available\n");
                            }

                            String description = productSnapshot.child("description").getValue(String.class);
                            if (description != null) {
                                productDetails.append("Description: ").append(description).append("\n");
                            }

                            productDetailsList.add(productDetails.toString());
                        }
                    }
                }

                if (productFound) {
                    showProductDetailsDialog(productDetailsList);
                } else {
                    // Check if the search query matches the username
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String username = dataSnapshot.child("username").getValue(String.class);
                                if (username != null && username.toLowerCase().contains(searchQuery.toLowerCase())) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(HomePage.this);
                                    builder.setTitle("User Details");
                                    builder.setMessage("Username: " + username + "\nEmail: " + dataSnapshot.child("email").getValue(String.class));
                                    builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                                    builder.show();
                                } else {
                                    Toast.makeText(HomePage.this, "No product or username found", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(HomePage.this, "Error searching username", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(HomePage.this, "No product found", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomePage.this, "Error searching product: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProductDetailsDialog(List<String> productDetailsList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product Details");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Add each product's details to the dialog
        for (String details : productDetailsList) {
            TextView textView = new TextView(this);
            textView.setText(details);
            textView.setPadding(0, 0, 0, 16);
            layout.addView(textView);
        }

        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Show messages fragment for user
    private void showMessagesFragment() {
        loadFragment(new UserMessagesFragment());
    }

    // Logout functionality
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(HomePage.this, loginpageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Show profile dialog
    private void showProfileDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String email = dataSnapshot.child("email").getValue(String.class) != null ? dataSnapshot.child("email").getValue(String.class) : "";
                    String phone = dataSnapshot.child("phone").getValue(String.class) != null ? dataSnapshot.child("phone").getValue(String.class) : "";
                    String address = dataSnapshot.child("address").getValue(String.class) != null ? dataSnapshot.child("address").getValue(String.class) : "";
                    String city = dataSnapshot.child("city").getValue(String.class) != null ? dataSnapshot.child("city").getValue(String.class) : "";
                    String dob = dataSnapshot.child("dob").getValue(String.class) != null ? dataSnapshot.child("dob").getValue(String.class) : "";
                    String province = dataSnapshot.child("province").getValue(String.class) != null ? dataSnapshot.child("province").getValue(String.class) : "";

                    AlertDialog.Builder builder = new AlertDialog.Builder(HomePage.this);
                    builder.setTitle("Edit Profile");

                    LinearLayout layout = new LinearLayout(HomePage.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(16, 16, 16, 16);

                    final EditText emailInput = new EditText(HomePage.this);
                    emailInput.setHint("Email");
                    emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    emailInput.setText(email);
                    emailInput.setEnabled(false);
                    layout.addView(emailInput);

                    final EditText phoneInput = new EditText(HomePage.this);
                    phoneInput.setHint("Phone");
                    phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
                    phoneInput.setText(phone);
                    layout.addView(phoneInput);

                    final EditText addressInput = new EditText(HomePage.this);
                    addressInput.setHint("Address");
                    addressInput.setText(address);
                    layout.addView(addressInput);

                    final EditText cityInput = new EditText(HomePage.this);
                    cityInput.setHint("City");
                    cityInput.setText(city);
                    layout.addView(cityInput);

                    final EditText dobInput = new EditText(HomePage.this);
                    dobInput.setHint("Date of Birth");
                    dobInput.setText(dob);
                    layout.addView(dobInput);

                    final EditText provinceInput = new EditText(HomePage.this);
                    provinceInput.setHint("Province");
                    provinceInput.setText(province);
                    layout.addView(provinceInput);

                    builder.setView(layout);

                    builder.setPositiveButton("Update", (dialog, which) -> {
                        String newPhone = phoneInput.getText().toString().trim();
                        String newAddress = addressInput.getText().toString().trim();
                        String newCity = cityInput.getText().toString().trim();
                        String newDob = dobInput.getText().toString().trim();
                        String newProvince = provinceInput.getText().toString().trim();

                        if (!newPhone.isEmpty() && !newAddress.isEmpty() && !newCity.isEmpty() && !newDob.isEmpty() && !newProvince.isEmpty()) {
                            userRef.child("phone").setValue(newPhone);
                            userRef.child("address").setValue(newAddress);
                            userRef.child("city").setValue(newCity);
                            userRef.child("dob").setValue(newDob);
                            userRef.child("province").setValue(newProvince);
                            Toast.makeText(HomePage.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                    builder.show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(HomePage.this, "Error fetching profile data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Method to toggle the drawer
    public void toggleDrawer(View view) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}