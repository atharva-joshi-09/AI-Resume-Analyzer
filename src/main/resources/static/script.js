(() => {
  "use strict";

  const MAX_FILE_BYTES = 5 * 1024 * 1024; // 5 MB

  const dropZone = document.getElementById("drop-zone");
  const browseBtn = document.getElementById("browse-btn");
  const sampleBtn = document.getElementById("sample-btn");
  const fileInput = document.getElementById("file-input");
  const fileNameEl = document.getElementById("file-name");
  const jobDescriptionEl = document.getElementById("job-description");
  const analyzeBtn = document.getElementById("analyze-btn");
  const errorBox = document.getElementById("error-box");

  const uploadCard = document.getElementById("upload-card");
  const loadingCard = document.getElementById("loading-card");
  const resultsSection = document.getElementById("results");
  const resetBtn = document.getElementById("reset-btn");

  let selectedFile = null;

  // ---------- File selection ----------

  browseBtn.addEventListener("click", () => fileInput.click());

  if (sampleBtn) {
    sampleBtn.addEventListener("click", async () => {
      try {
        const res = await fetch("sample-resume.pdf");
        if (!res.ok) throw new Error("Could not load sample resume");
        const blob = await res.blob();
        const file = new File([blob], "sample-resume.pdf", { type: "application/pdf" });
        handleFile(file);
        if (!jobDescriptionEl.value.trim()) {
          jobDescriptionEl.value = `Job Title: Java Backend Developer (Entry Level)

Responsibilities:
- Design and develop REST APIs using Java and Spring Boot
- Work with relational databases such as MySQL or PostgreSQL
- Write unit tests using JUnit
- Collaborate using Git and GitHub

Requirements:
- Strong fundamentals in Java and object-oriented programming
- Experience with Spring Boot and REST API development
- Familiarity with SQL and relational databases
- Understanding of Git version control`;
        }
      } catch (err) {
        showError("Failed to load sample resume: " + err.message);
      }
    });
  }

  fileInput.addEventListener("change", (e) => {
    if (e.target.files.length > 0) handleFile(e.target.files[0]);
  });

  ["dragenter", "dragover"].forEach((evt) =>
    dropZone.addEventListener(evt, (e) => {
      e.preventDefault();
      dropZone.classList.add("dragover");
    })
  );

  ["dragleave", "drop"].forEach((evt) =>
    dropZone.addEventListener(evt, (e) => {
      e.preventDefault();
      dropZone.classList.remove("dragover");
    })
  );

  dropZone.addEventListener("drop", (e) => {
    const files = e.dataTransfer.files;
    if (files.length > 0) handleFile(files[0]);
  });

  function handleFile(file) {
    hideError();

    const looksLikePdf = file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
    if (!looksLikePdf) {
      showError("Please upload a PDF resume.");
      resetFileSelection();
      return;
    }

    if (file.size > MAX_FILE_BYTES) {
      showError("The uploaded file is larger than the allowed limit (5 MB).");
      resetFileSelection();
      return;
    }

    selectedFile = file;
    fileNameEl.textContent = `Selected: ${file.name}`;
    analyzeBtn.disabled = false;
  }

  function resetFileSelection() {
    selectedFile = null;
    fileInput.value = "";
    fileNameEl.textContent = "";
    analyzeBtn.disabled = true;
  }

  // ---------- Submit / analyze ----------

  analyzeBtn.addEventListener("click", async () => {
    if (!selectedFile) return;
    hideError();

    const formData = new FormData();
    formData.append("resume", selectedFile);
    if (jobDescriptionEl.value.trim()) {
      formData.append("jobDescription", jobDescriptionEl.value.trim());
    }

    showLoading();

    try {
      const response = await fetch("/api/analyze", { method: "POST", body: formData });
      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(data.error || "Something went wrong. Please try again.");
      }

      renderResults(data.analysis);
    } catch (err) {
      hideLoading();
      showUploadCard();
      showError(err.message || "We couldn't analyze this resume. Please try again.");
    }
  });

  resetBtn.addEventListener("click", () => {
    resetFileSelection();
    jobDescriptionEl.value = "";
    resultsSection.hidden = true;
    showUploadCard();
    window.scrollTo({ top: 0, behavior: "smooth" });
  });

  // ---------- View state helpers ----------

  function showLoading() {
    uploadCard.hidden = true;
    loadingCard.hidden = false;
    resultsSection.hidden = true;
  }

  function hideLoading() {
    loadingCard.hidden = true;
  }

  function showUploadCard() {
    uploadCard.hidden = false;
  }

  function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = false;
  }

  function hideError() {
    errorBox.hidden = true;
    errorBox.textContent = "";
  }

  // ---------- Rendering ----------

  function renderResults(analysis) {
    hideLoading();
    resultsSection.hidden = false;

    document.getElementById("demo-banner").hidden = !analysis.demoMode;

    setScoreCircle("overall-score-circle", "overall-score-value", analysis.overallScore, "--primary");
    setScoreCircle("ats-score-circle", "ats-score-value", analysis.atsScore, "--secondary");

    const jobMatchBlock = document.getElementById("job-match-block");
    if (analysis.jobMatchScore !== null && analysis.jobMatchScore !== undefined) {
      jobMatchBlock.hidden = false;
      setScoreCircle("job-match-circle", "job-match-value", analysis.jobMatchScore, "--tertiary");
    } else {
      jobMatchBlock.hidden = true;
    }

    document.getElementById("summary-text").textContent = analysis.summary || "No summary available.";

    renderChips("skills-list", analysis.skills, "No specific technical skills were detected.");

    const missingSkillsCard = document.getElementById("missing-skills-card");
    if (analysis.missingSkills && analysis.missingSkills.length > 0) {
      missingSkillsCard.hidden = false;
      renderChips("missing-skills-list", analysis.missingSkills);
    } else {
      missingSkillsCard.hidden = true;
    }

    const keywordsCard = document.getElementById("keywords-card");
    const hasKeywords =
      (analysis.matchedKeywords && analysis.matchedKeywords.length > 0) ||
      (analysis.missingKeywords && analysis.missingKeywords.length > 0);
    if (hasKeywords) {
      keywordsCard.hidden = false;
      renderChips("matched-keywords-list", analysis.matchedKeywords, "None found.");
      renderChips("missing-keywords-list", analysis.missingKeywords, "None - great match!");
    } else {
      keywordsCard.hidden = true;
    }

    renderList("strengths-list", analysis.strengths, "No specific strengths identified.");
    renderList("weaknesses-list", analysis.weaknesses, "No specific weaknesses identified.");
    renderList("suggestions-list", analysis.suggestions, "No specific suggestions.");

    renderSections(analysis.sectionAnalysis);

    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function setScoreCircle(circleId, valueId, score, colorVar) {
    const circle = document.getElementById(circleId);
    const valueEl = document.getElementById(valueId);
    const safeScore = Math.max(0, Math.min(100, score || 0));
    const degrees = (safeScore / 100) * 360;
    const color = getComputedStyle(document.documentElement).getPropertyValue(colorVar).trim();
    circle.style.background = `conic-gradient(${color} ${degrees}deg, #e5e7eb ${degrees}deg)`;
    valueEl.textContent = safeScore;
  }

  function renderChips(containerId, items, emptyMessage) {
    const container = document.getElementById(containerId);
    container.innerHTML = "";
    if (!items || items.length === 0) {
      if (emptyMessage) {
        const span = document.createElement("span");
        span.textContent = emptyMessage;
        container.appendChild(span);
      }
      return;
    }
    items.forEach((item) => {
      const span = document.createElement("span");
      span.textContent = item;
      container.appendChild(span);
    });
  }

  function renderList(containerId, items, emptyMessage) {
    const list = document.getElementById(containerId);
    list.innerHTML = "";
    const source = items && items.length > 0 ? items : [emptyMessage];
    source.forEach((item) => {
      const li = document.createElement("li");
      li.textContent = item;
      list.appendChild(li);
    });
  }

  function renderSections(sections) {
    const container = document.getElementById("section-analysis-list");
    container.innerHTML = "";
    if (!sections || sections.length === 0) {
      container.textContent = "No section-level analysis available.";
      return;
    }
    sections.forEach((section) => {
      const item = document.createElement("div");
      item.className = "section-item";

      const header = document.createElement("div");
      header.className = "section-item__header";

      const title = document.createElement("span");
      title.className = "section-item__title";
      title.textContent = section.section;

      const badge = document.createElement("span");
      const found = (section.status || "").toLowerCase().includes("found") &&
                    !(section.status || "").toLowerCase().includes("not");
      badge.className = "status-badge " + (found ? "status-badge--found" : "status-badge--missing");
      badge.textContent = section.status;

      header.appendChild(title);
      header.appendChild(badge);

      const feedback = document.createElement("p");
      feedback.className = "section-item__feedback";
      feedback.textContent = section.feedback || "";

      item.appendChild(header);
      item.appendChild(feedback);
      container.appendChild(item);
    });
  }
})();
