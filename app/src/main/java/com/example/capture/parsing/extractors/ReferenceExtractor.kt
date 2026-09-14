package com.example.capture.parsing.extractors

object ReferenceExtractor {
  private val UPI_REF_PATTERNS = listOf(
    Regex("(?i)\\b(?:upi\\s*(?:ref|reference)?\\s*(?:no|num|id)?|rrn|utr)\\s*[:#-]?\\s*([0-9]{8,16})\\b"),
    Regex("(?i)\\bref(?:erence)?\\s*(?:no|id)?\\s*[:#-]?\\s*([0-9A-Z]{8,18})\\b"),
    Regex("(?i)\\btxn\\s*(?:id|ref)?\\s*[:#-]?\\s*([0-9A-Z]{8,18})\\b")
  )

  fun extractReference(message: String): String? {
    for (pat in UPI_REF_PATTERNS) {
      val match = pat.find(message)
      if (match != null) {
        return match.groupValues[1].trim()
      }
    }
    return null
  }
}
