package com.example.pricepk;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminPage extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout categoryList;
    private RecyclerView productRecyclerView;
    private ProductAdapter productAdapter;
    private FloatingActionButton addCategoryFab, addProductFab;
    private List<Category> categories = new ArrayList<>();
    private Category selectedCategory;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        categoryList = findViewById(R.id.category_list);
        productRecyclerView = findViewById(R.id.product_recycler_view);
        addCategoryFab = findViewById(R.id.add_category_fab);
        addProductFab = findViewById(R.id.add_product_fab);
        navigationView = findViewById(R.id.nav_view);

        // Setup RecyclerView
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, new ArrayList<>(), true, null);
        productRecyclerView.setAdapter(productAdapter);

        // Setup Navigation Drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
                return true;
            } else if (id == R.id.nav_profile) {
                showProfileDialog();
                return true;
            } else if (id == R.id.nav_home) {
                // Stay on the same page
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_messages) {
                showMessagesFragment();
                return true;
            }
            return false;
        });

        // Fetch categories from Firebase
        fetchCategories();

        // Add new category
        addCategoryFab.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Category");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Category Name");
            builder.setView(input);

            builder.setPositiveButton("Add", (dialog, which) -> {
                String categoryName = input.getText().toString().trim();
                if (!categoryName.isEmpty()) {
                    DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("categories").push();
                    Category category = new Category(categoryName, new ArrayList<>());
                    category.setId(categoriesRef.getKey());
                    categoriesRef.setValue(category);
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // Add new product
        addProductFab.setOnClickListener(v -> {
            if (selectedCategory != null) {
                showAddProductDialog();
            } else {
                Toast.makeText(this, "Select a category first", Toast.LENGTH_SHORT).show();
            }
        });
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

                    // Add category to ScrollView
                    View categoryView = LayoutInflater.from(AdminPage.this).inflate(R.layout.category_item, categoryList, false);
                    TextView categoryNameText = categoryView.findViewById(R.id.category_name);
                    ImageView editCategory = categoryView.findViewById(R.id.edit_category);
                    ImageView deleteCategory = categoryView.findViewById(R.id.delete_category);

                    // Make edit/delete buttons visible for admin
                    editCategory.setVisibility(View.VISIBLE);
                    deleteCategory.setVisibility(View.VISIBLE);

                    categoryNameText.setText(category.getName());

                    // On click, show products in RecyclerView
                    categoryView.setOnClickListener(v -> {
                        selectedCategory = category;
                        productAdapter = new ProductAdapter(AdminPage.this, category.getProducts(), true, category.getId());
                        productRecyclerView.setAdapter(productAdapter);
                    });

                    // Edit category
                    editCategory.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(AdminPage.this);
                        builder.setTitle("Edit Category");

                        final EditText input = new EditText(AdminPage.this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setText(category.getName());
                        builder.setView(input);

                        builder.setPositiveButton("Update", (dialog, which) -> {
                            String newName = input.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference("categories").child(category.getId());
                                categoryRef.child("name").setValue(newName);
                            }
                        });
                        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                        builder.show();
                    });

                    // Delete category
                    deleteCategory.setOnClickListener(v -> {
                        DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference("categories").child(category.getId());
                        categoryRef.removeValue();
                        if (selectedCategory != null && selectedCategory.getId().equals(category.getId())) {
                            productAdapter.updateProducts(new ArrayList<>());
                            selectedCategory = null;
                        }
                    });

                    categoryList.addView(categoryView);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Product");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Product Name");
        layout.addView(nameInput);

        final EditText priceInput = new EditText(this);
        priceInput.setHint("Price per kg");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(priceInput);

        final EditText discountInput = new EditText(this);
        discountInput.setHint("Discount Rate (%)");
        discountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(discountInput);

        final EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Description");
        layout.addView(descriptionInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String discountStr = discountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (!name.isEmpty() && !priceStr.isEmpty() && !discountStr.isEmpty() && !description.isEmpty()) {
                double price = Double.parseDouble(priceStr);
                double discount = Double.parseDouble(discountStr);
                Product product = new Product(name, price, discount, description);
                DatabaseReference productRef = FirebaseDatabase.getInstance().getReference("categories")
                        .child(selectedCategory.getId()).child("products").push();
                product.setId(productRef.getKey());
                productRef.setValue(product);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Logout functionality
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(AdminPage.this, loginpageActivity.class);
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

                    AlertDialog.Builder builder = new AlertDialog.Builder(AdminPage.this);
                    builder.setTitle("Edit Profile");

                    LinearLayout layout = new LinearLayout(AdminPage.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(16, 16, 16, 16);

                    final EditText emailInput = new EditText(AdminPage.this);
                    emailInput.setHint("Email");
                    emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    emailInput.setText(email);
                    emailInput.setEnabled(false); // Email is read-only
                    layout.addView(emailInput);

                    final EditText phoneInput = new EditText(AdminPage.this);
                    phoneInput.setHint("Phone");
                    phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
                    phoneInput.setText(phone);
                    layout.addView(phoneInput);

                    final EditText addressInput = new EditText(AdminPage.this);
                    addressInput.setHint("Address");
                    addressInput.setText(address);
                    layout.addView(addressInput);

                    builder.setView(layout);

                    builder.setPositiveButton("Update", (dialog, which) -> {
                        String newPhone = phoneInput.getText().toString().trim();
                        String newAddress = addressInput.getText().toString().trim();

                        if (!newPhone.isEmpty() && !newAddress.isEmpty()) {
                            userRef.child("phone").setValue(newPhone);
                            userRef.child("address").setValue(newAddress);
                            Toast.makeText(AdminPage.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                    builder.show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(AdminPage.this, "Error fetching profile data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Show messages fragment
    private void showMessagesFragment() {
        MessagesFragment fragment = new MessagesFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.coordinatorLayout, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        drawerLayout.closeDrawer(GravityCompat.START);
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