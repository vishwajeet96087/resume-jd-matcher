"""
Flask micro-service that predicts a job category from resume text.

Usage:
    cd ml-service
    python app.py          (starts on http://127.0.0.1:5000)

Endpoint:
    POST /predict
    Body: {"text": "resume content here ..."}
    Response: {
        "category": "Java Developer",
        "confidence": 0.87,
        "top3": [
            {"category": "Java Developer", "probability": 0.87},
            {"category": "Full Stack Developer", "probability": 0.06},
            {"category": "Python Developer", "probability": 0.03}
        ]
    }
"""

import os

import numpy as np
from flask import Flask, request, jsonify
from joblib import load

from text_utils import clean_text  # same cleaning used during training

app = Flask(__name__)

MODEL_PATH = "model.joblib"
pipeline = None  # loaded once at startup, reused for every request


def load_model():
    """
    Loads the trained pipeline from disk into memory.
    Called once when the server starts — not on every request,
    because loading a model from disk is slow (~100 ms) and
    keeping it in memory makes predictions instant (~1 ms).
    """
    global pipeline
    if not os.path.exists(MODEL_PATH):
        print(f"WARNING: {MODEL_PATH} not found. Run train_model.py first.")
        return
    pipeline = load(MODEL_PATH)
    print(f"Model loaded from {MODEL_PATH}")


@app.route("/predict", methods=["POST"])
def predict():
    """
    Accepts JSON {"text": "..."} and returns the predicted category.

    Steps:
      1. Validate: check JSON body exists and "text" is non-empty.
      2. Clean the text the same way we cleaned training data.
      3. Use pipeline.predict() to get the category label.
      4. Use pipeline.predict_proba() to get the confidence score.
      5. Return JSON response.
    """
    # ── Guard: model must be loaded ─────────────────────────────
    if pipeline is None:
        return jsonify({"error": "Model not loaded. Run train_model.py first."}), 503

    # ── Guard: request must have JSON body ──────────────────────
    data = request.get_json(silent=True)
    if data is None or "text" not in data:
        return jsonify({"error": "Request body must be JSON with a 'text' field."}), 400

    raw_text = data["text"].strip()
    if not raw_text:
        return jsonify({"error": "The 'text' field is empty."}), 400

    # ── Clean → Predict ─────────────────────────────────────────
    cleaned = clean_text(raw_text)

    # predict() returns an array of labels; we take the first (only) one.
    category = pipeline.predict([cleaned])[0]

    # predict_proba() returns a 2D array of probabilities for each class.
    # Shape: (1, num_classes) — one row per input text.
    probabilities = pipeline.predict_proba([cleaned])[0]

    # Confidence = probability of the predicted (top-1) class
    confidence = round(float(max(probabilities)), 2)

    # ── Top-3 predictions ───────────────────────────────────────
    # argsort() returns indices that would sort the array ascending,
    # so [-3:] picks the 3 largest and [::-1] reverses to descending.
    classes = pipeline.classes_                     # array of all category names
    top3_indices = np.argsort(probabilities)[-3:][::-1]
    top3 = [
        {
            "category": classes[i],
            "probability": round(float(probabilities[i]), 2)
        }
        for i in top3_indices
    ]

    return jsonify({
        "category": category,        # kept for backward compatibility
        "confidence": confidence,     # kept for backward compatibility
        "top3": top3                  # new: three most likely categories
    })


if __name__ == "__main__":
    load_model()
    # debug=True auto-reloads on code changes (dev only, never in production)
    app.run(host="0.0.0.0", port=5000, debug=True)
