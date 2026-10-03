"""
Train and compare classifiers, then save the best Naive Bayes model.

Usage:
    cd ml-service
    python train_model.py

Steps:
  1. Load CSV from data/ and keep only Resume_str + Category.
  2. Remove the leading title line from each resume (data-leakage fix).
  3. Clean text using the shared text_utils module.
  4. Vectorize with TF-IDF (unigrams + bigrams).
  5. Train MultinomialNB, ComplementNB, and LogisticRegression.
  6. Print a comparison table (accuracy + weighted F1).
  7. Save the best Naive Bayes variant as model.joblib.
  8. Run 3 hardcoded sanity checks.
"""

import glob
import os
import sys

import pandas as pd
from joblib import dump
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB, ComplementNB
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, f1_score, classification_report

from text_utils import clean_text  # shared cleaning logic


# ── Data-leakage fix ────────────────────────────────────────────────

def remove_title_line(text):
    """
    DATA-LEAKAGE PROBLEM:
      In this dataset, the first line of every resume is the job category
      itself (e.g., "Java Developer\nI have 5 years of experience...").
      If we keep it, the model simply learns to read the first line and
      return it as the prediction — achieving high training accuracy but
      failing completely on real resumes that don't start with a label.

    SOLUTION:
      Strip the first line.  This forces the model to learn from actual
      skills, experience, and education — the same features a real
      resume contains.

    WHY THIS MATTERS IN INTERVIEWS:
      Data leakage is one of the most common ML mistakes.  It means
      information from the target (label) leaks into the training input,
      giving unrealistically good results that don't generalize.
    """
    lines = text.split("\n", 1)  # split into [first_line, everything_else]
    if len(lines) > 1:
        return lines[1]  # return everything after the first line
    return text  # if there's only one line, keep it


def main():
    # ── 1. Load the CSV ─────────────────────────────────────────────
    csv_files = glob.glob(os.path.join("data", "*.csv"))
    if not csv_files:
        print("ERROR: no CSV file found in data/ directory.")
        sys.exit(1)

    csv_path = csv_files[0]
    print(f"Loading dataset: {csv_path}")
    df = pd.read_csv(csv_path)

    # Keep only the two columns we need; ignore ID and Resume_html
    df = df[["Resume_str", "Category"]]

    # ── 2. Drop rows with missing values ────────────────────────────
    before = len(df)
    df = df.dropna(subset=["Resume_str", "Category"])
    after = len(df)
    print(f"Rows: {before} total, {before - after} dropped (missing), {after} used.")

    # ── 3. Remove title line (data-leakage fix) ─────────────────────
    df["Resume_str"] = df["Resume_str"].apply(remove_title_line)

    # ── 4. Clean text ───────────────────────────────────────────────
    df["Resume_str"] = df["Resume_str"].apply(clean_text)

    X = df["Resume_str"]  # features (cleaned resume text)
    y = df["Category"]    # labels (job category)

    # ── 5. Stratified 80/20 split ───────────────────────────────────
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )
    print(f"Train size: {len(X_train)}, Test size: {len(X_test)}")

    # ── 6. Vectorize once, reuse for all models ─────────────────────
    #
    # ngram_range=(1, 2) means we create features for:
    #   - unigrams: individual words like "java", "spring"
    #   - bigrams:  consecutive word pairs like "machine_learning", "spring_boot"
    # Bigrams capture multi-word skills that unigrams miss.
    tfidf = TfidfVectorizer(
        stop_words="english",
        max_features=5000,
        ngram_range=(1, 2)    # unigrams + bigrams
    )

    # fit_transform on train data, transform-only on test data
    # (never fit on test data — that would be another form of data leakage)
    X_train_tfidf = tfidf.fit_transform(X_train)
    X_test_tfidf = tfidf.transform(X_test)

    # ── 7. Train and compare three classifiers ──────────────────────
    models = {
        "MultinomialNB":      MultinomialNB(),
        "ComplementNB":       ComplementNB(),
        "LogisticRegression":  LogisticRegression(max_iter=1000),
    }

    results = {}
    for name, model in models.items():
        model.fit(X_train_tfidf, y_train)
        y_pred = model.predict(X_test_tfidf)

        acc = accuracy_score(y_test, y_pred)
        f1 = f1_score(y_test, y_pred, average="weighted")
        results[name] = {"model": model, "accuracy": acc, "f1": f1}

    # ── 8. Print comparison table ───────────────────────────────────
    print("\n" + "=" * 55)
    print(f"{'Model':<25} {'Accuracy':>12} {'Weighted F1':>14}")
    print("-" * 55)
    for name, r in results.items():
        print(f"{name:<25} {r['accuracy']:>12.4f} {r['f1']:>14.4f}")
    print("=" * 55)

    # ── 9. Select the best Naive Bayes variant ──────────────────────
    # We only consider NB models because the project is documented as
    # a Naive Bayes classifier.  LogisticRegression is shown for comparison.
    nb_candidates = {k: v for k, v in results.items() if "NB" in k}
    best_nb_name = max(nb_candidates, key=lambda k: nb_candidates[k]["f1"])
    best_nb = results[best_nb_name]

    print(f"\nBest Naive Bayes: {best_nb_name}")
    print(f"  Accuracy:    {best_nb['accuracy']:.4f}")
    print(f"  Weighted F1: {best_nb['f1']:.4f}")

    # Print full classification report for the best NB
    y_pred_best = best_nb["model"].predict(X_test_tfidf)
    print(f"\nClassification Report ({best_nb_name}):")
    print(classification_report(y_test, y_pred_best))

    # ── 10. Save as a Pipeline ──────────────────────────────────────
    # We wrap the already-fit vectorizer + classifier into a Pipeline
    # so that app.py can call pipeline.predict(["raw text"]) in one step.
    pipeline = Pipeline([
        ("tfidf", tfidf),
        ("clf", best_nb["model"]),
    ])

    model_path = "model.joblib"
    dump(pipeline, model_path)
    print(f"Model saved to {model_path} ({best_nb_name})")

    # ── 11. Sanity check with hardcoded samples ─────────────────────
    # Print the categories the model was trained on so it's clear
    # which labels it can predict.
    print("\nDataset categories:")
    for cat in sorted(pipeline.classes_):
        print(f"  - {cat}")

    # These are short, obvious texts to verify the model isn't broken.
    # "Expected" is the dataset category we hope the model predicts.
    # Accountant and Chef are NOT dataset categories, so they test
    # how the model handles out-of-domain input.
    samples = [
        (
            "INFORMATION-TECHNOLOGY",
            "Experienced software developer with expertise in Spring Boot, "
            "Hibernate, REST APIs, microservices architecture, Maven, "
            "and MySQL. Built scalable backend systems for e-commerce."
        ),
        (
            "ACCOUNTANT",
            "Certified public accountant skilled in financial reporting, "
            "tax preparation, auditing, bookkeeping, accounts payable "
            "and receivable, and regulatory compliance using SAP."
        ),
        (
            "CHEF",
            "Professional chef with 8 years of experience in menu planning, "
            "food preparation, kitchen management, catering services, "
            "food safety compliance, and team supervision."
        ),
    ]

    print("\nSanity Check:")
    print(f"  {'Expected':<25} {'Predicted':<25}")
    print("  " + "-" * 50)
    for expected, text in samples:
        cleaned = clean_text(text)
        predicted = pipeline.predict([cleaned])[0]
        match = "YES" if expected.upper() == predicted.upper() else "NO"
        print(f"  {expected:<25} {predicted:<25} {match}")


if __name__ == "__main__":
    main()
