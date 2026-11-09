import argparse
import csv
import os
import re

# Matches any embedded newline / line-separator characters inside a field
LINE_BREAK_RE = re.compile(r"[\r\n\x0b\x0c\x85\u2028\u2029]+")

def clean_row(row):
    """Replaces embedded line breaks within CSV fields with a single space."""
    return [LINE_BREAK_RE.sub(" ", field) for field in row]

def split_csv(file_path, rows_per_file):
    if not os.path.exists(file_path):
        print(f"Error: File '{file_path}' not found.")
        return

    base_name, ext = os.path.splitext(file_path)

    with open(file_path, 'r', encoding='utf-8', errors='replace', newline='') as f:
        reader = csv.reader(f)
        try:
            header = next(reader)
        except StopIteration:
            print("Error: The file is empty.")
            return

        file_count = 1
        row_count = 0
        out_file = open(f"{base_name}_part{file_count}{ext}", 'w', encoding='utf-8', newline='')
        writer = csv.writer(out_file)
        writer.writerow(header)

        for row in reader:
            if row_count >= rows_per_file:
                out_file.close()
                file_count += 1
                row_count = 0
                out_file = open(f"{base_name}_part{file_count}{ext}", 'w', encoding='utf-8', newline='')
                writer = csv.writer(out_file)
                writer.writerow(header)

            writer.writerow(clean_row(row))
            row_count += 1

        out_file.close()

    print(f"Successfully split '{file_path}' into {file_count} smaller file(s).")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Split a large CSV file by row count while keeping headers.")
    parser.add_argument("file", help="Path to the CSV file")
    parser.add_argument("lines", type=int, help="Number of rows per split file (excluding header)")

    args = parser.parse_args()
    split_csv(args.file, args.lines)
