# Shopify Product Catalogue Dataset Extractor

Utilities for downloading the [`Shopify/product-catalogue`](https://huggingface.co/datasets/Shopify/product-catalogue) dataset from Hugging Face, saving product images locally, generating mock pricing and stock counts, cleaning text encoding issues, and splitting the resulting CSV into smaller chunks for database ingestion.

---

## Prerequisites

- **Python 3.9+**

---

## 1. Virtual Environment Setup

Create and activate a Python virtual environment (`venv`), then install the required dependencies:

```bash
# 1. Create the virtual environment
python3 -m venv venv

# 2. Activate the virtual environment (macOS / Linux)
source venv/bin/activate

# (Optional) Activate on Windows:
# venv\Scripts\activate

# 3. Upgrade pip and install required packages
pip install --upgrade pip
pip install datasets pandas pillow
```

To deactivate the virtual environment when finished:

```bash
deactivate
```

---

## 2. Running the Scripts

Make sure your virtual environment is activated (`source venv/bin/activate`) before running the scripts below.

### Step 1: Extract Images and Generate `products.csv`

Run `extract_catalogue_csv.py` to download the `Shopify/product-catalogue` dataset (both `train` and `test` splits), export every product image as `<id>.jpg`, and compile the product metadata with generated mock prices and stock counts (`10`–`100`):

```bash
python extract_catalogue_csv.py
```

**Outputs:**
- `demo_site/public/images/<id>.jpg` — Product images starting at ID `10000000` (e.g., `10000000.jpg`).
- `demo_site/products.csv` — Full product catalogue CSV with columns:
  `id, title, price, stock, description, brand, category`

---

### Step 2: Repair Text Encoding (Optional)

Run `repair_text.py` to clean common UTF-8 / CP1252 mojibake sequences (such as `â€™`, `â€“`, `â€œ`, `Â®`, `Â©`) in `./demo_site/products.csv`:

```bash
python repair_text.py
```

---

### Step 3: Split `products.csv` into Smaller Files

Run `split_csv.py` to split a large CSV file into smaller part files by row count while preserving the header row and normalizing any embedded line breaks inside fields:

```bash
python split_csv.py <path_to_csv> <rows_per_file>
```

**Example (10,000 rows per file):**

```bash
python split_csv.py demo_site/products.csv 10000
```

**Outputs:**
- `demo_site/products_part1.csv`
- `demo_site/products_part2.csv`
- `...`

---

## Project Structure

```text
.
├── extract_catalogue_csv.py   # Downloads dataset, saves <id>.jpg images, and builds products.csv
├── repair_text.py             # Cleans mojibake characters in demo_site/products.csv
├── split_csv.py               # Splits a CSV into smaller chunk files while preserving headers
├── README.md                  # Project documentation
└── demo_site/
    ├── products.csv           # Generated full product catalogue
    ├── products_part1.csv     # Generated split CSV chunks (after running split_csv.py)
    └── public/
        └── images/            # Extracted JPEG product images (10000000.jpg, 10000001.jpg, ...)
```

