package com.example.pricepk;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private boolean isAdmin;
    private String categoryId;
    private Context context;

    public ProductAdapter(Context context, List<Product> productList, boolean isAdmin, String categoryId) {
        this.context = context;
        this.productList = productList;
        this.isAdmin = isAdmin;
        this.categoryId = categoryId;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.productName.setText(product.getName());
        holder.productPrice.setText("Price: " + product.getPricePerKg() + " per kg");
        holder.productDiscount.setText("Discount: " + product.getDiscountRate() + "%");
        holder.productDescription.setText("Description: " + (product.getDescription() != null ? product.getDescription() : "N/A"));

        if (isAdmin) {
            holder.productActions.setVisibility(View.VISIBLE);

            // Edit Product
            holder.editProduct.setOnClickListener(v -> {
                showEditProductDialog(product);
            });

            // Delete Product
            holder.deleteProduct.setOnClickListener(v -> {
                DatabaseReference productRef = FirebaseDatabase.getInstance().getReference("categories")
                        .child(categoryId).child("products").child(product.getId());
                productRef.removeValue();
            });
        } else {
            holder.productActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateProducts(List<Product> newProducts) {
        this.productList = newProducts;
        notifyDataSetChanged();
    }

    private void showEditProductDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Product");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        final EditText nameInput = new EditText(context);
        nameInput.setHint("Product Name");
        nameInput.setText(product.getName());
        layout.addView(nameInput);

        final EditText priceInput = new EditText(context);
        priceInput.setHint("Price per kg");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        priceInput.setText(String.valueOf(product.getPricePerKg()));
        layout.addView(priceInput);

        final EditText discountInput = new EditText(context);
        discountInput.setHint("Discount Rate (%)");
        discountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        discountInput.setText(String.valueOf(product.getDiscountRate()));
        layout.addView(discountInput);

        final EditText descriptionInput = new EditText(context);
        descriptionInput.setHint("Description");
        descriptionInput.setText(product.getDescription());
        layout.addView(descriptionInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();
            String discountStr = discountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (!name.isEmpty() && !priceStr.isEmpty() && !discountStr.isEmpty() && !description.isEmpty()) {
                double price = Double.parseDouble(priceStr);
                double discount = Double.parseDouble(discountStr);
                Product updatedProduct = new Product(name, price, discount, description);
                updatedProduct.setId(product.getId());
                DatabaseReference productRef = FirebaseDatabase.getInstance().getReference("categories")
                        .child(categoryId).child("products").child(product.getId());
                productRef.setValue(updatedProduct);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productDiscount, productDescription;
        LinearLayout productActions;
        ImageView editProduct, deleteProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productDiscount = itemView.findViewById(R.id.product_discount);
            productDescription = itemView.findViewById(R.id.product_description);
            productActions = itemView.findViewById(R.id.product_actions);
            editProduct = itemView.findViewById(R.id.edit_product);
            deleteProduct = itemView.findViewById(R.id.delete_product);
        }
    }
}