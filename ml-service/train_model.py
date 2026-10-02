"""
Train a Naive Bayes classifier to predict job category from resume text.

Usage:
    cd ml-service
    python train_model.py

Reads the first .csv it finds in data/, trains a TF-IDF + MultinomialNB
pipeline, prints accuracy + classification report, and saves the pipeline
to model.joblib.
"""

import glob
import os
import sys

import pandas as pd
from joblib import dump
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, classification_report

from text_utils import clean_text  # shared cleaning logic


def main():
    # ── 1. Find and load the CSV ────────────────────────────────────
    csv_files = glob.glob(os.path.join("data", "*.csv"))
    if not csv_files:
        print("ERROR: no CSV file found in data/ directory.")
        sys.exit(1)

    csv_path = csv_files[0]  # use the first (and only) CSV
    print(f"Loading dataset: {csv_path}")
    df = pd.read_csv(csv_path)

    # ── 2. Keep only the columns we need ────────────────────────────
    # The CSV has: ID, Resume_str, Resume_html, Category
    # We ignore ID (just a counter) and Resume_html (markup noise).
    df = df[["Resume_str", "Category"]]

    # ── 3. Drop rows with missing values ────────────────────────────
    before = len(df)
    df = df.dropna(subset=["Resume_str", "Category"])
    after = len(df)
    print(f"Rows: {before} total, {before - after} dropped (missing), {after} used.")

    # ── 4. Clean the resume text ────────────────────────────────────
    # .apply() calls clean_text() on every row's Resume_str value.
    df["Resume_str"] = df["Resume_str"].apply(clean_text)

    X = df["Resume_str"]  # features (input text)
    y = df["Category"]    # labels (job category)

    # ── 5. Stratified train/test split ──────────────────────────────
    # stratify=y → keeps the same proportion of each category in both
    # train and test sets, which is important when some categories have
    # far fewer samples than others.
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.20,        # 80% train, 20% test
        random_state=42,       # reproducible results
        stratify=y             # balanced class distribution
    )
    print(f"Train size: {len(X_train)}, Test size: {len(X_test)}")

    # ── 6. Build the scikit-learn Pipeline ──────────────────────────
    #
    # A Pipeline chains multiple steps so that calling pipeline.fit()
    # runs TF-IDF fitting + NB fitting in sequence, and pipeline.predict()
    # runs TF-IDF transform + NB predict in sequence.
    #
    # TfidfVectorizer:
    #   - Converts raw text → a matrix of TF-IDF scores.
    #   - stop_words="english" removes common words like "the", "is".
    #   - max_features=5000 keeps only the 5000 most important terms
    #     (reduces memory and noise from rare words).
    #
    # MultinomialNB:
    #   - Naive Bayes classifier that works on word-frequency features.
    #   - "Naive" because it assumes words are independent of each other
    #     (e.g., P("java" | Developer) is independent of P("spring" | Developer)).
    #   - Despite this unrealistic assumption, it works surprisingly well
    #     for text because the decision depends on overall word patterns,
    #     not exact word combinations.
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(stop_words="english", max_features=5000)),
        ("clf",   MultinomialNB()),
    ])

    # ── 7. Train ────────────────────────────────────────────────────
    print("Training...")
    pipeline.fit(X_train, y_train)

    # ── 8. Evaluate ─────────────────────────────────────────────────
    y_pred = pipeline.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(f"\nAccuracy: {accuracy:.4f}  ({accuracy * 100:.2f}%)\n")

    # classification_report shows per-category precision, recall, F1.
    print("Classification Report:")
    print(classification_report(y_test, y_pred))

    # ── 9. Save the trained pipeline ────────────────────────────────
    model_path = "model.joblib"
    dump(pipeline, model_path)
    print(f"Model saved to {model_path}")


if __name__ == "__main__":
    main()
