import os
import random
import pandas as pd
from datasets import load_dataset

# 1. Create local directories for your demo site assets
os.makedirs("demo_site/public/images", exist_ok=True)

print("Downloading dataset from Hugging Face...")
dataset = load_dataset("Shopify/product-catalogue")

products_list = []

# Set your custom 8-digit starting number here
product_id_counter = 10000000 

def generate_mock_price(title, category):
    """Generates a realistic mock price based on product context."""
    text = (str(title) + " " + str(category)).lower()
    
    # Tiered pricing based on keywords
    if any(k in text for k in ["laptop", "phone", "tv", "camera", "electronics"]):
        base_price = random.uniform(299.00, 1299.00)
    elif any(k in text for k in ["watch", "shoes", "jacket", "coat", "bag"]):
        base_price = random.uniform(49.00, 199.00)
    elif any(k in text for k in ["shirt", "pants", "hat", "apparel", "clothing"]):
        base_price = random.uniform(15.00, 59.00)
    elif any(k in text for k in ["book", "sticker", "case", "accessory", "socks"]):
        base_price = random.uniform(4.99, 24.99)
    else:
        # Default fallback range for general products
        base_price = random.uniform(12.00, 89.00)
    
    # Format to common retail endings (.99, .50, or .00)
    ending = random.choice([0.99, 0.99, 0.99, 0.50, 0.00])
    return round(int(base_price) + ending, 2)

print("Extracting images and compiling CSV data...")

# Loop through all splits (both 'train' and 'test') to catch every product
for split in dataset.keys():
    print(f"Processing split: {split}...")
    for item in dataset[split]:
        
        # 1. Fetch using the dataset's actual schema keys
        img_obj = item.get("product_image")
        title = item.get("product_title", "")
        description = item.get("product_description", "")
        brand = item.get("ground_truth_brand", "")
        category = item.get("ground_truth_category", "")
        
        # 2. Skip entries that are missing an image asset entirely
        if img_obj is None:
            continue

        # 3. Apply your exact <increment_counter> file naming structure
        image_filename = f"{product_id_counter}.jpg"
        image_path = os.path.join("demo_site/public/images", image_filename)
        
        # 4. Save the image locally
        try:
            # Convert to RGB to ensure standard JPEG saving compatibility
            img = img_obj.convert('RGB')
            img.save(image_path, "JPEG")
        except Exception as e:
            print(f"Skipping index {product_id_counter} due to image save error: {e}")
            continue

        # 5. Map fields to your target CSV row format (paths removed, price & stock added)
        product_entry = {
            "id": product_id_counter,
            "title": title,
            "description": description,
            "brand": brand,
            "category": category,
            "price": generate_mock_price(title, category),
            "stock": random.randint(10, 100)
        }
        products_list.append(product_entry)
        
        # Increment the 8-digit ID for the next item
        product_id_counter += 1

# 2. Convert to a DataFrame and export to CSV
df = pd.DataFrame(products_list)
csv_output_path = "demo_site/products.csv"

# Reordering columns explicitly for clean database ingestion layout
df = df[["id", "title", "price", "stock", "description", "brand", "category"]]
df.to_csv(csv_output_path, index=False, encoding="utf-8")

print(f"\nSuccess! Extracted {len(products_list)} items total.")
print(f"📁 Images saved to: demo_site/public/images/ (e.g., 10000000.jpg)")
print(f"📄 CSV catalogue saved to: {csv_output_path}")

