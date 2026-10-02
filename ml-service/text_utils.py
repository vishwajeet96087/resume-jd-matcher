"""
Shared text-cleaning function used by BOTH train_model.py and app.py.
Keeping it in one file guarantees that the model sees the same cleaned
text during training and during prediction — a mismatch would silently
produce wrong predictions.
"""

import re


def clean_text(text):
    """
    Normalizes raw resume text into a format suitable for TF-IDF.

    Steps (order matters):
      1. Lowercase        → "Java" and "java" become the same token.
      2. Remove punctuation → "C++" becomes "c", commas/periods vanish.
      3. Remove numbers    → "3 years" becomes "years" (numbers add noise
                              because TF-IDF treats "3" and "5" as unrelated).
      4. Collapse spaces   → "java   spring" → "java spring".
      5. Strip edges       → remove leading/trailing whitespace.
    """
    text = text.lower()                        # step 1
    text = re.sub(r"[^\w\s]", " ", text)       # step 2: replace punctuation with space
    text = re.sub(r"\d+", " ", text)           # step 3: replace digit sequences with space
    text = re.sub(r"\s+", " ", text)           # step 4: collapse multiple spaces
    return text.strip()                        # step 5
